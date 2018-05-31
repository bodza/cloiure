package giraaff.word;

import java.lang.reflect.Constructor;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.impl.WordFactoryOperation;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BridgeMethodUtils;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.calc.Condition.CanonicalizedCondition;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.XorNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.extended.JavaReadNode;
import giraaff.nodes.extended.JavaWriteNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.NodePlugin;
import giraaff.nodes.graphbuilderconf.TypePlugin;
import giraaff.nodes.java.AbstractCompareAndSwapNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.LogicCompareAndSwapNode;
import giraaff.nodes.java.StoreIndexedNode;
import giraaff.nodes.java.ValueCompareAndSwapNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;
import giraaff.word.Word.Opcode;
import giraaff.word.Word.Operation;

/**
 * A plugin for calls to {@linkplain Operation word operations}, as well as all other nodes that
 * need special handling for {@link Word} types.
 */
// @class WordOperationPlugin
public class WordOperationPlugin implements NodePlugin, TypePlugin, InlineInvokePlugin
{
    protected final WordTypes wordTypes;
    protected final JavaKind wordKind;
    protected final SnippetReflectionProvider snippetReflection;

    // @cons
    public WordOperationPlugin(SnippetReflectionProvider snippetReflection, WordTypes wordTypes)
    {
        super();
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
                throw b.bailout("cannot store a non-word value into a word field: " + field.format("%H.%n"));
            }
            else if (!isWordField && isWordValue)
            {
                throw b.bailout("cannot store a word value into a non-word field: " + field.format("%H.%n"));
            }
        }

        // We never need to intercept the field store.
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
                throw b.bailout("cannot store a non-word value into a word array: " + arrayType.toJavaName(true));
            }
            b.add(createStoreIndexedNode(array, index, value));
            return true;
        }
        if (elementKind == JavaKind.Object && value.getStackKind() == wordKind)
        {
            throw b.bailout("cannot store a word value into a non-word array: " + arrayType.toJavaName(true));
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
                throw b.bailout("cannot cast a word value to a non-word type: " + type.toJavaName(true));
            }
            return false;
        }

        if (object.getStackKind() != wordKind)
        {
            throw b.bailout("cannot cast a non-word value to a word type: " + type.toJavaName(true));
        }
        b.push(JavaKind.Object, object);
        return true;
    }

    @Override
    public boolean handleInstanceOf(GraphBuilderContext b, ValueNode object, ResolvedJavaType type, JavaTypeProfile profile)
    {
        if (wordTypes.isWord(type))
        {
            throw b.bailout("cannot use instanceof for word a type: " + type.toJavaName(true));
        }
        else if (object.getStackKind() != JavaKind.Object)
        {
            throw b.bailout("cannot use instanceof on a word value: " + type.toJavaName(true));
        }
        return false;
    }

    protected void processWordOperation(GraphBuilderContext b, ValueNode[] args, ResolvedJavaMethod wordMethod)
    {
        JavaKind returnKind = wordMethod.getSignature().getReturnKind();
        WordFactoryOperation factoryOperation = BridgeMethodUtils.getAnnotation(WordFactoryOperation.class, wordMethod);
        if (factoryOperation != null)
        {
            switch (factoryOperation.opcode())
            {
                case ZERO:
                    b.addPush(returnKind, ConstantNode.forIntegerKind(wordKind, 0L));
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
            throw b.bailout("cannot call method on a word value: " + wordMethod.format("%H.%n(%p)"));
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
                b.addPush(returnKind, new XorNode(args[0], b.add(ConstantNode.forIntegerKind(wordKind, -1))));
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
                    location = LocationIdentity.any();
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
                b.push(returnKind, readOp(b, readKind, address, LocationIdentity.any(), barrierType, true));
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
                    location = LocationIdentity.any();
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
        catch (Throwable t)
        {
            throw new GraalError(t);
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

        ConstantNode trueValue = graph.add(ConstantNode.forInt(1));
        ConstantNode falseValue = graph.add(ConstantNode.forInt(0));

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
        final BarrierType barrier = (op == Opcode.READ_BARRIERED) ? BarrierType.PRECISE : BarrierType.NONE;
        final boolean compressible = (op == Opcode.READ_OBJECT || op == Opcode.READ_BARRIERED);

        return readOp(b, readKind, address, location, barrier, compressible);
    }

    public static ValueNode readOp(GraphBuilderContext b, JavaKind readKind, AddressNode address, LocationIdentity location, BarrierType barrierType, boolean compressible)
    {
        /*
         * A JavaReadNode lowered to a ReadNode that will not float. This means it cannot float above
         * an explicit zero check on its base address or any other test that ensures the read is safe.
         */
        return b.add(new JavaReadNode(readKind, address, location, barrierType, compressible));
    }

    protected void writeOp(GraphBuilderContext b, JavaKind writeKind, AddressNode address, LocationIdentity location, ValueNode value, Opcode op)
    {
        final BarrierType barrier = (op == Opcode.WRITE_BARRIERED) ? BarrierType.PRECISE : BarrierType.NONE;
        final boolean compressible = (op == Opcode.WRITE_OBJECT || op == Opcode.WRITE_BARRIERED);
        b.add(new JavaWriteNode(writeKind, address, location, value, barrier, compressible));
    }

    protected AbstractCompareAndSwapNode casOp(JavaKind writeKind, JavaKind returnKind, AddressNode address, LocationIdentity location, ValueNode expectedValue, ValueNode newValue)
    {
        if (returnKind == JavaKind.Boolean)
        {
            return new LogicCompareAndSwapNode(address, expectedValue, newValue, location);
        }
        else
        {
            return new ValueCompareAndSwapNode(address, expectedValue, newValue, location);
        }
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
}
