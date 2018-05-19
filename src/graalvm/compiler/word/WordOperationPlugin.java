package graalvm.compiler.word;

import static graalvm.compiler.nodes.ConstantNode.forInt;
import static graalvm.compiler.nodes.ConstantNode.forIntegerKind;
import static org.graalvm.word.LocationIdentity.any;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.bytecode.BridgeMethodUtils;
import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.calc.Condition;
import graalvm.compiler.core.common.calc.Condition.CanonicalizedCondition;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.calc.IntegerBelowNode;
import graalvm.compiler.nodes.calc.IntegerEqualsNode;
import graalvm.compiler.nodes.calc.IntegerLessThanNode;
import graalvm.compiler.nodes.calc.NarrowNode;
import graalvm.compiler.nodes.calc.SignExtendNode;
import graalvm.compiler.nodes.calc.XorNode;
import graalvm.compiler.nodes.calc.ZeroExtendNode;
import graalvm.compiler.nodes.extended.JavaReadNode;
import graalvm.compiler.nodes.extended.JavaWriteNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderTool;
import graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin;
import graalvm.compiler.nodes.graphbuilderconf.NodePlugin;
import graalvm.compiler.nodes.graphbuilderconf.TypePlugin;
import graalvm.compiler.nodes.java.AbstractCompareAndSwapNode;
import graalvm.compiler.nodes.java.LoadFieldNode;
import graalvm.compiler.nodes.java.LoadIndexedNode;
import graalvm.compiler.nodes.java.LogicCompareAndSwapNode;
import graalvm.compiler.nodes.java.StoreIndexedNode;
import graalvm.compiler.nodes.java.ValueCompareAndSwapNode;
import graalvm.compiler.nodes.memory.HeapAccess.BarrierType;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import graalvm.compiler.nodes.type.StampTool;
import graalvm.compiler.word.Word.Opcode;
import graalvm.compiler.word.Word.Operation;
import org.graalvm.word.LocationIdentity;
import org.graalvm.word.impl.WordFactoryOperation;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * A plugin for calls to {@linkplain Operation word operations}, as well as all other nodes that
 * need special handling for {@link Word} types.
 */
public class WordOperationPlugin implements NodePlugin, TypePlugin, InlineInvokePlugin
{
    protected final WordTypes wordTypes;
    protected final JavaKind wordKind;
    protected final SnippetReflectionProvider snippetReflection;

    public WordOperationPlugin(SnippetReflectionProvider snippetReflection, WordTypes wordTypes)
    {
        this.snippetReflection = snippetReflection;
        this.wordTypes = wordTypes;
        this.wordKind = wordTypes.getWordKind();
    }

    @Override
    public boolean canChangeStackKind(GraphBuilderContext b)
    {
        return true;
    }

    /**
     * Processes a call to a method if it is annotated as a word operation by adding nodes to the
     * graph being built that implement the denoted operation.
     *
     * @return {@code true} iff {@code method} is annotated with {@link Operation} (and was thus
     *         processed by this method)
     */
    @Override
    public boolean handleInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args)
    {
        if (!wordTypes.isWordOperation(method))
        {
            return false;
        }
        processWordOperation(b, args, wordTypes.getWordOperation(method, b.getMethod().getDeclaringClass()));
        return true;
    }

    @Override
    public StampPair interceptType(GraphBuilderTool b, JavaType declaredType, boolean nonNull)
    {
        Stamp wordStamp = null;
        if (declaredType instanceof ResolvedJavaType)
        {
            ResolvedJavaType resolved = (ResolvedJavaType) declaredType;
            if (wordTypes.isWord(resolved))
            {
                wordStamp = wordTypes.getWordStamp(resolved);
            }
            else if (resolved.isArray() && wordTypes.isWord(resolved.getElementalType()))
            {
                TypeReference trusted = TypeReference.createTrustedWithoutAssumptions(resolved);
                wordStamp = StampFactory.object(trusted, nonNull);
            }
        }
        if (wordStamp != null)
        {
            return StampPair.createSingle(wordStamp);
        }
        else
        {
            return null;
        }
    }

    @Override
    public void notifyNotInlined(GraphBuilderContext b, ResolvedJavaMethod method, Invoke invoke)
    {
        if (wordTypes.isWord(invoke.asNode()))
        {
            invoke.asNode().setStamp(wordTypes.getWordStamp(StampTool.typeOrNull(invoke.asNode())));
        }
    }

    @Override
    public boolean handleLoadField(GraphBuilderContext b, ValueNode receiver, ResolvedJavaField field)
    {
        StampPair wordStamp = interceptType(b, field.getType(), false);
        if (wordStamp != null)
        {
            LoadFieldNode loadFieldNode = LoadFieldNode.createOverrideStamp(wordStamp, receiver, field);
            b.addPush(field.getJavaKind(), loadFieldNode);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadStaticField(GraphBuilderContext b, ResolvedJavaField staticField)
    {
        return handleLoadField(b, null, staticField);
    }

    @Override
    public boolean handleLoadIndexed(GraphBuilderContext b, ValueNode array, ValueNode index, JavaKind elementKind)
    {
        ResolvedJavaType arrayType = StampTool.typeOrNull(array);
        /*
         * There are cases where the array does not have a known type yet, i.e., the type is null.
         * In that case we assume it is not a word type.
         */
        if (arrayType != null && wordTypes.isWord(arrayType.getComponentType()))
        {
            b.addPush(elementKind, createLoadIndexedNode(array, index));
            return true;
        }
        return false;
    }

    protected LoadIndexedNode createLoadIndexedNode(ValueNode array, ValueNode index)
    {
        return new LoadIndexedNode(null, array, index, wordKind);
    }

    @Override
    public boolean handleStoreField(GraphBuilderContext b, ValueNode object, ResolvedJavaField field, ValueNode value)
    {
        if (field.getJavaKind() == JavaKind.Object)
        {
            boolean isWordField = wordTypes.isWord(field.getType());
            boolean isWordValue = value.getStackKind() == wordKind;

            if (isWordField && !isWordValue)
            {
                throw bailout(b, "Cannot store a non-word value into a word field: " + field.format("%H.%n"));
            }
            else if (!isWordField && isWordValue)
            {
                throw bailout(b, "Cannot store a word value into a non-word field: " + field.format("%H.%n"));
            }
        }

        /* We never need to intercept the field store. */
        return false;
    }

    @Override
    public boolean handleStoreStaticField(GraphBuilderContext b, ResolvedJavaField field, ValueNode value)
    {
        return handleStoreField(b, null, field, value);
    }

    @Override
    public boolean handleStoreIndexed(GraphBuilderContext b, ValueNode array, ValueNode index, JavaKind elementKind, ValueNode value)
    {
        ResolvedJavaType arrayType = StampTool.typeOrNull(array);
        if (arrayType != null && wordTypes.isWord(arrayType.getComponentType()))
        {
            if (value.getStackKind() != wordKind)
            {
                throw bailout(b, "Cannot store a non-word value into a word array: " + arrayType.toJavaName(true));
            }
            b.add(createStoreIndexedNode(array, index, value));
            return true;
        }
        if (elementKind == JavaKind.Object && value.getStackKind() == wordKind)
        {
            throw bailout(b, "Cannot store a word value into a non-word array: " + arrayType.toJavaName(true));
        }
        return false;
    }

    protected StoreIndexedNode createStoreIndexedNode(ValueNode array, ValueNode index, ValueNode value)
    {
        return new StoreIndexedNode(array, index, wordKind, value);
    }

    @Override
    public boolean handleCheckCast(GraphBuilderContext b, ValueNode object, ResolvedJavaType type, JavaTypeProfile profile)
    {
        if (!wordTypes.isWord(type))
        {
            if (object.getStackKind() != JavaKind.Object)
            {
                throw bailout(b, "Cannot cast a word value to a non-word type: " + type.toJavaName(true));
            }
            return false;
        }

        if (object.getStackKind() != wordKind)
        {
            throw bailout(b, "Cannot cast a non-word value to a word type: " + type.toJavaName(true));
        }
        b.push(JavaKind.Object, object);
        return true;
    }

    @Override
    public boolean handleInstanceOf(GraphBuilderContext b, ValueNode object, ResolvedJavaType type, JavaTypeProfile profile)
    {
        if (wordTypes.isWord(type))
        {
            throw bailout(b, "Cannot use instanceof for word a type: " + type.toJavaName(true));
        }
        else if (object.getStackKind() != JavaKind.Object)
        {
            throw bailout(b, "Cannot use instanceof on a word value: " + type.toJavaName(true));
        }
        return false;
    }

    protected void processWordOperation(GraphBuilderContext b, ValueNode[] args, ResolvedJavaMethod wordMethod) throws GraalError
    {
        JavaKind returnKind = wordMethod.getSignature().getReturnKind();
        WordFactoryOperation factoryOperation = BridgeMethodUtils.getAnnotation(WordFactoryOperation.class, wordMethod);
        if (factoryOperation != null)
        {
            switch (factoryOperation.opcode())
            {
                case ZERO:
                    b.addPush(returnKind, forIntegerKind(wordKind, 0L));
                    return;

                case FROM_UNSIGNED:
                    b.push(returnKind, fromUnsigned(b, args[0]));
                    return;

                case FROM_SIGNED:
                    b.push(returnKind, fromSigned(b, args[0]));
                    return;
            }
        }

        Word.Operation operation = BridgeMethodUtils.getAnnotation(Word.Operation.class, wordMethod);
        if (operation == null)
        {
            throw bailout(b, "Cannot call method on a word value: " + wordMethod.format("%H.%n(%p)"));
        }
        switch (operation.opcode())
        {
            case NODE_CLASS:
                ValueNode left = args[0];
                ValueNode right = operation.rightOperandIsInt() ? toUnsigned(b, args[1], JavaKind.Int) : fromSigned(b, args[1]);

                b.addPush(returnKind, createBinaryNodeInstance(operation.node(), left, right));
                break;

            case COMPARISON:
                b.push(returnKind, comparisonOp(b, operation.condition(), args[0], fromSigned(b, args[1])));
                break;

            case IS_NULL:
                b.push(returnKind, comparisonOp(b, Condition.EQ, args[0], ConstantNode.forIntegerKind(wordKind, 0L)));
                break;

            case IS_NON_NULL:
                b.push(returnKind, comparisonOp(b, Condition.NE, args[0], ConstantNode.forIntegerKind(wordKind, 0L)));
                break;

            case NOT:
                b.addPush(returnKind, new XorNode(args[0], b.add(forIntegerKind(wordKind, -1))));
                break;

            case READ_POINTER:
            case READ_OBJECT:
            case READ_BARRIERED:
            {
                JavaKind readKind = wordTypes.asKind(wordMethod.getSignature().getReturnType(wordMethod.getDeclaringClass()));
                AddressNode address = makeAddress(b, args[0], args[1]);
                LocationIdentity location;
                if (args.length == 2)
                {
                    location = any();
                }
                else
                {
                    location = snippetReflection.asObject(LocationIdentity.class, args[2].asJavaConstant());
                }
                b.push(returnKind, readOp(b, readKind, address, location, operation.opcode()));
                break;
            }
            case READ_HEAP:
            {
                JavaKind readKind = wordTypes.asKind(wordMethod.getSignature().getReturnType(wordMethod.getDeclaringClass()));
                AddressNode address = makeAddress(b, args[0], args[1]);
                BarrierType barrierType = snippetReflection.asObject(BarrierType.class, args[2].asJavaConstant());
                b.push(returnKind, readOp(b, readKind, address, any(), barrierType, true));
                break;
            }
            case WRITE_POINTER:
            case WRITE_OBJECT:
            case WRITE_BARRIERED:
            case INITIALIZE:
            {
                JavaKind writeKind = wordTypes.asKind(wordMethod.getSignature().getParameterType(wordMethod.isStatic() ? 2 : 1, wordMethod.getDeclaringClass()));
                AddressNode address = makeAddress(b, args[0], args[1]);
                LocationIdentity location;
                if (args.length == 3)
                {
                    location = any();
                }
                else
                {
                    location = snippetReflection.asObject(LocationIdentity.class, args[3].asJavaConstant());
                }
                writeOp(b, writeKind, address, location, args[2], operation.opcode());
                break;
            }

            case TO_RAW_VALUE:
                b.push(returnKind, toUnsigned(b, args[0], JavaKind.Long));
                break;

            case OBJECT_TO_TRACKED:
                WordCastNode objectToTracked = b.add(WordCastNode.objectToTrackedPointer(args[0], wordKind));
                b.push(returnKind, objectToTracked);
                break;

            case OBJECT_TO_UNTRACKED:
                WordCastNode objectToUntracked = b.add(WordCastNode.objectToUntrackedPointer(args[0], wordKind));
                b.push(returnKind, objectToUntracked);
                break;

            case FROM_ADDRESS:
                WordCastNode addressToWord = b.add(WordCastNode.addressToWord(args[0], wordKind));
                b.push(returnKind, addressToWord);
                break;

            case TO_OBJECT:
                WordCastNode wordToObject = b.add(WordCastNode.wordToObject(args[0], wordKind));
                b.push(returnKind, wordToObject);
                break;

            case TO_OBJECT_NON_NULL:
                WordCastNode wordToObjectNonNull = b.add(WordCastNode.wordToObjectNonNull(args[0], wordKind));
                b.push(returnKind, wordToObjectNonNull);
                break;

            case CAS_POINTER:
                AddressNode address = makeAddress(b, args[0], args[1]);
                JavaKind valueKind = wordTypes.asKind(wordMethod.getSignature().getParameterType(1, wordMethod.getDeclaringClass()));
                LocationIdentity location = snippetReflection.asObject(LocationIdentity.class, args[4].asJavaConstant());
                JavaType returnType = wordMethod.getSignature().getReturnType(wordMethod.getDeclaringClass());
                b.addPush(returnKind, casOp(valueKind, wordTypes.asKind(returnType), address, location, args[2], args[3]));
                break;
            default:
                throw new GraalError("Unknown opcode: %s", operation.opcode());
        }
    }

    /**
     * Create an instance of a binary node which is used to lower {@link Word} operations. This
     * method is called for all {@link Word} operations which are annotated with @Operation(node =
     * ...) and encapsulates the reflective allocation of the node.
     */
    private static ValueNode createBinaryNodeInstance(Class<? extends ValueNode> nodeClass, ValueNode left, ValueNode right)
    {
        try
        {
            Constructor<?> cons = nodeClass.getDeclaredConstructor(ValueNode.class, ValueNode.class);
            return (ValueNode) cons.newInstance(left, right);
        }
        catch (Throwable ex)
        {
            throw new GraalError(ex).addContext(nodeClass.getName());
        }
    }

    private ValueNode comparisonOp(GraphBuilderContext graph, Condition condition, ValueNode left, ValueNode right)
    {
        CanonicalizedCondition canonical = condition.canonicalize();

        ValueNode a = canonical.mustMirror() ? right : left;
        ValueNode b = canonical.mustMirror() ? left : right;

        CompareNode comparison;
        if (canonical.getCanonicalCondition() == CanonicalCondition.EQ)
        {
            comparison = new IntegerEqualsNode(a, b);
        }
        else if (canonical.getCanonicalCondition() == CanonicalCondition.BT)
        {
            comparison = new IntegerBelowNode(a, b);
        }
        else
        {
            comparison = new IntegerLessThanNode(a, b);
        }

        ConstantNode trueValue = graph.add(forInt(1));
        ConstantNode falseValue = graph.add(forInt(0));

        if (canonical.mustNegate())
        {
            ConstantNode temp = trueValue;
            trueValue = falseValue;
            falseValue = temp;
        }
        return graph.add(new ConditionalNode(graph.add(comparison), trueValue, falseValue));
    }

    protected ValueNode readOp(GraphBuilderContext b, JavaKind readKind, AddressNode address, LocationIdentity location, Opcode op)
    {
        final BarrierType barrier = (op == Opcode.READ_BARRIERED ? BarrierType.PRECISE : BarrierType.NONE);
        final boolean compressible = (op == Opcode.READ_OBJECT || op == Opcode.READ_BARRIERED);

        return readOp(b, readKind, address, location, barrier, compressible);
    }

    public static ValueNode readOp(GraphBuilderContext b, JavaKind readKind, AddressNode address, LocationIdentity location, BarrierType barrierType, boolean compressible)
    {
        /*
         * A JavaReadNode lowered to a ReadNode that will not float. This means it cannot float
         * above an explicit zero check on its base address or any other test that ensures the read
         * is safe.
         */
        JavaReadNode read = b.add(new JavaReadNode(readKind, address, location, barrierType, compressible));
        return read;
    }

    protected void writeOp(GraphBuilderContext b, JavaKind writeKind, AddressNode address, LocationIdentity location, ValueNode value, Opcode op)
    {
        final BarrierType barrier = (op == Opcode.WRITE_BARRIERED ? BarrierType.PRECISE : BarrierType.NONE);
        final boolean compressible = (op == Opcode.WRITE_OBJECT || op == Opcode.WRITE_BARRIERED);
        b.add(new JavaWriteNode(writeKind, address, location, value, barrier, compressible));
    }

    protected AbstractCompareAndSwapNode casOp(JavaKind writeKind, JavaKind returnKind, AddressNode address, LocationIdentity location, ValueNode expectedValue, ValueNode newValue)
    {
        boolean isLogic = returnKind == JavaKind.Boolean;
        AbstractCompareAndSwapNode cas;
        if (isLogic)
        {
            cas = new LogicCompareAndSwapNode(address, expectedValue, newValue, location);
        }
        else
        {
            cas = new ValueCompareAndSwapNode(address, expectedValue, newValue, location);
        }
        return cas;
    }

    public AddressNode makeAddress(GraphBuilderContext b, ValueNode base, ValueNode offset)
    {
        return b.add(new OffsetAddressNode(base, fromSigned(b, offset)));
    }

    public ValueNode fromUnsigned(GraphBuilderContext b, ValueNode value)
    {
        return convert(b, value, wordKind, true);
    }

    public ValueNode fromSigned(GraphBuilderContext b, ValueNode value)
    {
        return convert(b, value, wordKind, false);
    }

    public ValueNode toUnsigned(GraphBuilderContext b, ValueNode value, JavaKind toKind)
    {
        return convert(b, value, toKind, true);
    }

    public ValueNode convert(GraphBuilderContext b, ValueNode value, JavaKind toKind, boolean unsigned)
    {
        if (value.getStackKind() == toKind)
        {
            return value;
        }

        if (toKind == JavaKind.Int)
        {
            return b.add(new NarrowNode(value, 32));
        }
        else
        {
            if (unsigned)
            {
                return b.add(new ZeroExtendNode(value, 64));
            }
            else
            {
                return b.add(new SignExtendNode(value, 64));
            }
        }
    }

    private static BailoutException bailout(GraphBuilderContext b, String msg)
    {
        throw b.bailout(msg + "\nat " + b.getCode().asStackTraceElement(b.bci()));
    }
}
