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
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;
import giraaff.word.Word;

///
// A plugin for calls to {@linkplain Word.Operation word operations}, as well as all other nodes
// that need special handling for {@link Word} types.
///
// @class WordOperationPlugin
public class WordOperationPlugin implements NodePlugin, TypePlugin, InlineInvokePlugin
{
    // @field
    protected final WordTypes ___wordTypes;
    // @field
    protected final JavaKind ___wordKind;
    // @field
    protected final SnippetReflectionProvider ___snippetReflection;

    // @cons WordOperationPlugin
    public WordOperationPlugin(SnippetReflectionProvider __snippetReflection, WordTypes __wordTypes)
    {
        super();
        this.___snippetReflection = __snippetReflection;
        this.___wordTypes = __wordTypes;
        this.___wordKind = __wordTypes.getWordKind();
    }

    @Override
    public boolean canChangeStackKind(GraphBuilderContext __b)
    {
        return true;
    }

    ///
    // Processes a call to a method if it is annotated as a word operation by adding nodes to the
    // graph being built that implement the denoted operation.
    //
    // @return {@code true} iff {@code method} is annotated with {@link Word.Operation} (and was thus
    //         processed by this method)
    ///
    @Override
    public boolean handleInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        if (!this.___wordTypes.isWordOperation(__method))
        {
            return false;
        }
        processWordOperation(__b, __args, this.___wordTypes.getWordOperation(__method, __b.getMethod().getDeclaringClass()));
        return true;
    }

    @Override
    public StampPair interceptType(GraphBuilderTool __b, JavaType __declaredType, boolean __nonNull)
    {
        Stamp __wordStamp = null;
        if (__declaredType instanceof ResolvedJavaType)
        {
            ResolvedJavaType __resolved = (ResolvedJavaType) __declaredType;
            if (this.___wordTypes.isWord(__resolved))
            {
                __wordStamp = this.___wordTypes.getWordStamp(__resolved);
            }
            else if (__resolved.isArray() && this.___wordTypes.isWord(__resolved.getElementalType()))
            {
                TypeReference __trusted = TypeReference.createTrustedWithoutAssumptions(__resolved);
                __wordStamp = StampFactory.object(__trusted, __nonNull);
            }
        }
        if (__wordStamp != null)
        {
            return StampPair.createSingle(__wordStamp);
        }
        else
        {
            return null;
        }
    }

    @Override
    public void notifyNotInlined(GraphBuilderContext __b, ResolvedJavaMethod __method, Invoke __invoke)
    {
        if (this.___wordTypes.isWord(__invoke.asNode()))
        {
            __invoke.asNode().setStamp(this.___wordTypes.getWordStamp(StampTool.typeOrNull(__invoke.asNode())));
        }
    }

    @Override
    public boolean handleLoadField(GraphBuilderContext __b, ValueNode __receiver, ResolvedJavaField __field)
    {
        StampPair __wordStamp = interceptType(__b, __field.getType(), false);
        if (__wordStamp != null)
        {
            LoadFieldNode __loadFieldNode = LoadFieldNode.createOverrideStamp(__wordStamp, __receiver, __field);
            __b.addPush(__field.getJavaKind(), __loadFieldNode);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadStaticField(GraphBuilderContext __b, ResolvedJavaField __staticField)
    {
        return handleLoadField(__b, null, __staticField);
    }

    @Override
    public boolean handleLoadIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        ResolvedJavaType __arrayType = StampTool.typeOrNull(__array);
        // There are cases where the array does not have a known type yet, i.e., the type is null.
        // In that case we assume it is not a word type.
        if (__arrayType != null && this.___wordTypes.isWord(__arrayType.getComponentType()))
        {
            __b.addPush(__elementKind, createLoadIndexedNode(__array, __index));
            return true;
        }
        return false;
    }

    protected LoadIndexedNode createLoadIndexedNode(ValueNode __array, ValueNode __index)
    {
        return new LoadIndexedNode(null, __array, __index, this.___wordKind);
    }

    @Override
    public boolean handleStoreField(GraphBuilderContext __b, ValueNode __object, ResolvedJavaField __field, ValueNode __value)
    {
        if (__field.getJavaKind() == JavaKind.Object)
        {
            boolean __isWordField = this.___wordTypes.isWord(__field.getType());
            boolean __isWordValue = __value.getStackKind() == this.___wordKind;

            if (__isWordField && !__isWordValue)
            {
                throw __b.bailout("cannot store a non-word value into a word field: " + __field.format("%H.%n"));
            }
            else if (!__isWordField && __isWordValue)
            {
                throw __b.bailout("cannot store a word value into a non-word field: " + __field.format("%H.%n"));
            }
        }

        // We never need to intercept the field store.
        return false;
    }

    @Override
    public boolean handleStoreStaticField(GraphBuilderContext __b, ResolvedJavaField __field, ValueNode __value)
    {
        return handleStoreField(__b, null, __field, __value);
    }

    @Override
    public boolean handleStoreIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind, ValueNode __value)
    {
        ResolvedJavaType __arrayType = StampTool.typeOrNull(__array);
        if (__arrayType != null && this.___wordTypes.isWord(__arrayType.getComponentType()))
        {
            if (__value.getStackKind() != this.___wordKind)
            {
                throw __b.bailout("cannot store a non-word value into a word array: " + __arrayType.toJavaName(true));
            }
            __b.add(createStoreIndexedNode(__array, __index, __value));
            return true;
        }
        if (__elementKind == JavaKind.Object && __value.getStackKind() == this.___wordKind)
        {
            throw __b.bailout("cannot store a word value into a non-word array: " + __arrayType.toJavaName(true));
        }
        return false;
    }

    protected StoreIndexedNode createStoreIndexedNode(ValueNode __array, ValueNode __index, ValueNode __value)
    {
        return new StoreIndexedNode(__array, __index, this.___wordKind, __value);
    }

    @Override
    public boolean handleCheckCast(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        if (!this.___wordTypes.isWord(__type))
        {
            if (__object.getStackKind() != JavaKind.Object)
            {
                throw __b.bailout("cannot cast a word value to a non-word type: " + __type.toJavaName(true));
            }
            return false;
        }

        if (__object.getStackKind() != this.___wordKind)
        {
            throw __b.bailout("cannot cast a non-word value to a word type: " + __type.toJavaName(true));
        }
        __b.push(JavaKind.Object, __object);
        return true;
    }

    @Override
    public boolean handleInstanceOf(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        if (this.___wordTypes.isWord(__type))
        {
            throw __b.bailout("cannot use instanceof for word a type: " + __type.toJavaName(true));
        }
        else if (__object.getStackKind() != JavaKind.Object)
        {
            throw __b.bailout("cannot use instanceof on a word value: " + __type.toJavaName(true));
        }
        return false;
    }

    protected void processWordOperation(GraphBuilderContext __b, ValueNode[] __args, ResolvedJavaMethod __wordMethod)
    {
        JavaKind __returnKind = __wordMethod.getSignature().getReturnKind();
        WordFactoryOperation __factoryOperation = BridgeMethodUtils.getAnnotation(WordFactoryOperation.class, __wordMethod);
        if (__factoryOperation != null)
        {
            switch (__factoryOperation.opcode())
            {
                case ZERO:
                {
                    __b.addPush(__returnKind, ConstantNode.forIntegerKind(this.___wordKind, 0L));
                    return;
                }

                case FROM_UNSIGNED:
                {
                    __b.push(__returnKind, fromUnsigned(__b, __args[0]));
                    return;
                }

                case FROM_SIGNED:
                {
                    __b.push(__returnKind, fromSigned(__b, __args[0]));
                    return;
                }
            }
        }

        Word.Operation __operation = BridgeMethodUtils.getAnnotation(Word.Operation.class, __wordMethod);
        if (__operation == null)
        {
            throw __b.bailout("cannot call method on a word value: " + __wordMethod.format("%H.%n(%p)"));
        }
        switch (__operation.opcode())
        {
            case NODE_CLASS:
            {
                ValueNode __left = __args[0];
                ValueNode __right = __operation.rightOperandIsInt() ? toUnsigned(__b, __args[1], JavaKind.Int) : fromSigned(__b, __args[1]);

                __b.addPush(__returnKind, createBinaryNodeInstance(__operation.node(), __left, __right));
                break;
            }

            case COMPARISON:
            {
                __b.push(__returnKind, comparisonOp(__b, __operation.condition(), __args[0], fromSigned(__b, __args[1])));
                break;
            }

            case IS_NULL:
            {
                __b.push(__returnKind, comparisonOp(__b, Condition.EQ, __args[0], ConstantNode.forIntegerKind(this.___wordKind, 0L)));
                break;
            }

            case IS_NON_NULL:
            {
                __b.push(__returnKind, comparisonOp(__b, Condition.NE, __args[0], ConstantNode.forIntegerKind(this.___wordKind, 0L)));
                break;
            }

            case NOT:
            {
                __b.addPush(__returnKind, new XorNode(__args[0], __b.add(ConstantNode.forIntegerKind(this.___wordKind, -1))));
                break;
            }

            case READ_POINTER:
            case READ_OBJECT:
            case READ_BARRIERED:
            {
                JavaKind __readKind = this.___wordTypes.asKind(__wordMethod.getSignature().getReturnType(__wordMethod.getDeclaringClass()));
                AddressNode __address = makeAddress(__b, __args[0], __args[1]);
                LocationIdentity __location;
                if (__args.length == 2)
                {
                    __location = LocationIdentity.any();
                }
                else
                {
                    __location = this.___snippetReflection.asObject(LocationIdentity.class, __args[2].asJavaConstant());
                }
                __b.push(__returnKind, readOp(__b, __readKind, __address, __location, __operation.opcode()));
                break;
            }
            case READ_HEAP:
            {
                JavaKind __readKind = this.___wordTypes.asKind(__wordMethod.getSignature().getReturnType(__wordMethod.getDeclaringClass()));
                AddressNode __address = makeAddress(__b, __args[0], __args[1]);
                HeapAccess.BarrierType __barrierType = this.___snippetReflection.asObject(HeapAccess.BarrierType.class, __args[2].asJavaConstant());
                __b.push(__returnKind, readOp(__b, __readKind, __address, LocationIdentity.any(), __barrierType, true));
                break;
            }
            case WRITE_POINTER:
            case WRITE_OBJECT:
            case WRITE_BARRIERED:
            case INITIALIZE:
            {
                JavaKind __writeKind = this.___wordTypes.asKind(__wordMethod.getSignature().getParameterType(__wordMethod.isStatic() ? 2 : 1, __wordMethod.getDeclaringClass()));
                AddressNode __address = makeAddress(__b, __args[0], __args[1]);
                LocationIdentity __location;
                if (__args.length == 3)
                {
                    __location = LocationIdentity.any();
                }
                else
                {
                    __location = this.___snippetReflection.asObject(LocationIdentity.class, __args[3].asJavaConstant());
                }
                writeOp(__b, __writeKind, __address, __location, __args[2], __operation.opcode());
                break;
            }

            case TO_RAW_VALUE:
            {
                __b.push(__returnKind, toUnsigned(__b, __args[0], JavaKind.Long));
                break;
            }

            case OBJECT_TO_TRACKED:
            {
                WordCastNode __objectToTracked = __b.add(WordCastNode.objectToTrackedPointer(__args[0], this.___wordKind));
                __b.push(__returnKind, __objectToTracked);
                break;
            }

            case OBJECT_TO_UNTRACKED:
            {
                WordCastNode __objectToUntracked = __b.add(WordCastNode.objectToUntrackedPointer(__args[0], this.___wordKind));
                __b.push(__returnKind, __objectToUntracked);
                break;
            }

            case FROM_ADDRESS:
            {
                WordCastNode __addressToWord = __b.add(WordCastNode.addressToWord(__args[0], this.___wordKind));
                __b.push(__returnKind, __addressToWord);
                break;
            }

            case TO_OBJECT:
            {
                WordCastNode __wordToObject = __b.add(WordCastNode.wordToObject(__args[0], this.___wordKind));
                __b.push(__returnKind, __wordToObject);
                break;
            }

            case TO_OBJECT_NON_NULL:
            {
                WordCastNode __wordToObjectNonNull = __b.add(WordCastNode.wordToObjectNonNull(__args[0], this.___wordKind));
                __b.push(__returnKind, __wordToObjectNonNull);
                break;
            }

            case CAS_POINTER:
            {
                AddressNode __address = makeAddress(__b, __args[0], __args[1]);
                JavaKind __valueKind = this.___wordTypes.asKind(__wordMethod.getSignature().getParameterType(1, __wordMethod.getDeclaringClass()));
                LocationIdentity __location = this.___snippetReflection.asObject(LocationIdentity.class, __args[4].asJavaConstant());
                JavaType __returnType = __wordMethod.getSignature().getReturnType(__wordMethod.getDeclaringClass());
                __b.addPush(__returnKind, casOp(__valueKind, this.___wordTypes.asKind(__returnType), __address, __location, __args[2], __args[3]));
                break;
            }

            default:
                throw new GraalError("unknown opcode: %s", __operation.opcode());
        }
    }

    ///
    // Create an instance of a binary node which is used to lower {@link Word} operations.
    // This method is called for all {@link Word} operations which are annotated with
    // @Word.Operation(node = ...) and encapsulates the reflective allocation of the node.
    ///
    private static ValueNode createBinaryNodeInstance(Class<? extends ValueNode> __nodeClass, ValueNode __left, ValueNode __right)
    {
        try
        {
            Constructor<?> __cons = __nodeClass.getDeclaredConstructor(ValueNode.class, ValueNode.class);
            return (ValueNode) __cons.newInstance(__left, __right);
        }
        catch (Throwable __t)
        {
            throw new GraalError(__t);
        }
    }

    private ValueNode comparisonOp(GraphBuilderContext __graph, Condition __condition, ValueNode __left, ValueNode __right)
    {
        Condition.CanonicalizedCondition __canonical = __condition.canonicalize();

        ValueNode __a = __canonical.mustMirror() ? __right : __left;
        ValueNode __b = __canonical.mustMirror() ? __left : __right;

        CompareNode __comparison;
        if (__canonical.getCanonicalCondition() == CanonicalCondition.EQ)
        {
            __comparison = new IntegerEqualsNode(__a, __b);
        }
        else if (__canonical.getCanonicalCondition() == CanonicalCondition.BT)
        {
            __comparison = new IntegerBelowNode(__a, __b);
        }
        else
        {
            __comparison = new IntegerLessThanNode(__a, __b);
        }

        ConstantNode __trueValue = __graph.add(ConstantNode.forInt(1));
        ConstantNode __falseValue = __graph.add(ConstantNode.forInt(0));

        if (__canonical.mustNegate())
        {
            ConstantNode __temp = __trueValue;
            __trueValue = __falseValue;
            __falseValue = __temp;
        }
        return __graph.add(new ConditionalNode(__graph.add(__comparison), __trueValue, __falseValue));
    }

    protected ValueNode readOp(GraphBuilderContext __b, JavaKind __readKind, AddressNode __address, LocationIdentity __location, Word.WordOpcode __op)
    {
        final HeapAccess.BarrierType __barrier = (__op == Word.WordOpcode.READ_BARRIERED) ? HeapAccess.BarrierType.PRECISE : HeapAccess.BarrierType.NONE;
        final boolean __compressible = (__op == Word.WordOpcode.READ_OBJECT || __op == Word.WordOpcode.READ_BARRIERED);

        return readOp(__b, __readKind, __address, __location, __barrier, __compressible);
    }

    public static ValueNode readOp(GraphBuilderContext __b, JavaKind __readKind, AddressNode __address, LocationIdentity __location, HeapAccess.BarrierType __barrierType, boolean __compressible)
    {
        // A JavaReadNode lowered to a ReadNode that will not float. This means it cannot float above
        // an explicit zero check on its base address or any other test that ensures the read is safe.
        return __b.add(new JavaReadNode(__readKind, __address, __location, __barrierType, __compressible));
    }

    protected void writeOp(GraphBuilderContext __b, JavaKind __writeKind, AddressNode __address, LocationIdentity __location, ValueNode __value, Word.WordOpcode __op)
    {
        final HeapAccess.BarrierType __barrier = (__op == Word.WordOpcode.WRITE_BARRIERED) ? HeapAccess.BarrierType.PRECISE : HeapAccess.BarrierType.NONE;
        final boolean __compressible = (__op == Word.WordOpcode.WRITE_OBJECT || __op == Word.WordOpcode.WRITE_BARRIERED);
        __b.add(new JavaWriteNode(__writeKind, __address, __location, __value, __barrier, __compressible));
    }

    protected AbstractCompareAndSwapNode casOp(JavaKind __writeKind, JavaKind __returnKind, AddressNode __address, LocationIdentity __location, ValueNode __expectedValue, ValueNode __newValue)
    {
        if (__returnKind == JavaKind.Boolean)
        {
            return new LogicCompareAndSwapNode(__address, __expectedValue, __newValue, __location);
        }
        else
        {
            return new ValueCompareAndSwapNode(__address, __expectedValue, __newValue, __location);
        }
    }

    public AddressNode makeAddress(GraphBuilderContext __b, ValueNode __base, ValueNode __offset)
    {
        return __b.add(new OffsetAddressNode(__base, fromSigned(__b, __offset)));
    }

    public ValueNode fromUnsigned(GraphBuilderContext __b, ValueNode __value)
    {
        return convert(__b, __value, this.___wordKind, true);
    }

    public ValueNode fromSigned(GraphBuilderContext __b, ValueNode __value)
    {
        return convert(__b, __value, this.___wordKind, false);
    }

    public ValueNode toUnsigned(GraphBuilderContext __b, ValueNode __value, JavaKind __toKind)
    {
        return convert(__b, __value, __toKind, true);
    }

    public ValueNode convert(GraphBuilderContext __b, ValueNode __value, JavaKind __toKind, boolean __unsigned)
    {
        if (__value.getStackKind() == __toKind)
        {
            return __value;
        }

        if (__toKind == JavaKind.Int)
        {
            return __b.add(new NarrowNode(__value, 32));
        }
        else
        {
            if (__unsigned)
            {
                return __b.add(new ZeroExtendNode(__value, 64));
            }
            else
            {
                return __b.add(new SignExtendNode(__value, 64));
            }
        }
    }
}
