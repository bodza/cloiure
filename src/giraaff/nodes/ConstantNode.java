package giraaff.nodes;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.lir.ConstantValue;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.util.GraalError;

///
// The {@code ConstantNode} represents a {@link Constant constant}.
///
// @class ConstantNode
public final class ConstantNode extends FloatingNode implements LIRLowerable
{
    // @def
    public static final NodeClass<ConstantNode> TYPE = NodeClass.create(ConstantNode.class);

    // @field
    protected final Constant ___value;

    // @field
    private final int ___stableDimension;
    // @field
    private final boolean ___isDefaultStable;

    private static ConstantNode createPrimitive(JavaConstant __value)
    {
        return new ConstantNode(__value, StampFactory.forConstant(__value));
    }

    ///
    // Constructs a new node representing the specified constant.
    //
    // @param value the constant
    ///
    // @cons
    public ConstantNode(Constant __value, Stamp __stamp)
    {
        this(__value, __stamp, 0, false);
    }

    // @cons
    private ConstantNode(Constant __value, Stamp __stamp, int __stableDimension, boolean __isDefaultStable)
    {
        super(TYPE, __stamp);
        this.___value = __value;
        this.___stableDimension = __stableDimension;
        if (__stableDimension == 0)
        {
            // Ensure that isDefaultStable has a canonical value to avoid having two constant nodes that only differ
            // in this field. The value of isDefaultStable is only used when we have a stable array dimension.
            this.___isDefaultStable = false;
        }
        else
        {
            this.___isDefaultStable = __isDefaultStable;
        }
    }

    ///
    // @return the constant value represented by this node
    ///
    public Constant getValue()
    {
        return this.___value;
    }

    ///
    // @return the number of stable dimensions if this is a stable array, otherwise 0
    ///
    public int getStableDimension()
    {
        return this.___stableDimension;
    }

    ///
    // @return true if this is a stable array and the default elements are considered stable
    ///
    public boolean isDefaultStable()
    {
        return this.___isDefaultStable;
    }

    ///
    // Gathers all the {@link ConstantNode}s that are inputs to the
    // {@linkplain StructuredGraph#getNodes() live nodes} in a given graph.
    ///
    public static NodeIterable<ConstantNode> getConstantNodes(StructuredGraph __graph)
    {
        return __graph.getNodes().filter(ConstantNode.class);
    }

    ///
    // Replaces this node at its usages with another node.
    ///
    public void replace(StructuredGraph __graph, Node __replacement)
    {
        replaceAtUsagesAndDelete(__replacement);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRKind __kind = __gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        if (onlyUsedInVirtualState())
        {
            __gen.setResult(this, new ConstantValue(__kind, this.___value));
        }
        else
        {
            __gen.setResult(this, __gen.getLIRGeneratorTool().emitConstant(__kind, this.___value));
        }
    }

    private boolean onlyUsedInVirtualState()
    {
        for (Node __n : this.usages())
        {
            if (__n instanceof VirtualState)
            {
                // Only virtual usage.
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    public static ConstantNode forConstant(JavaConstant __constant, MetaAccessProvider __metaAccess, StructuredGraph __graph)
    {
        if (__constant.getJavaKind().getStackKind() == JavaKind.Int && __constant.getJavaKind() != JavaKind.Int)
        {
            return forInt(__constant.asInt(), __graph);
        }
        if (__constant.getJavaKind() == JavaKind.Object)
        {
            return unique(__graph, new ConstantNode(__constant, StampFactory.forConstant(__constant, __metaAccess)));
        }
        else
        {
            return unique(__graph, createPrimitive(__constant));
        }
    }

    public static ConstantNode forConstant(JavaConstant __constant, int __stableDimension, boolean __isDefaultStable, MetaAccessProvider __metaAccess)
    {
        if (__constant.getJavaKind().getStackKind() == JavaKind.Int && __constant.getJavaKind() != JavaKind.Int)
        {
            return forInt(__constant.asInt());
        }
        if (__constant.getJavaKind() == JavaKind.Object)
        {
            return new ConstantNode(__constant, StampFactory.forConstant(__constant, __metaAccess), __stableDimension, __isDefaultStable);
        }
        else
        {
            return createPrimitive(__constant);
        }
    }

    public static ConstantNode forConstant(JavaConstant __array, MetaAccessProvider __metaAccess)
    {
        return forConstant(__array, 0, false, __metaAccess);
    }

    public static ConstantNode forConstant(Stamp __stamp, Constant __constant, MetaAccessProvider __metaAccess, StructuredGraph __graph)
    {
        return __graph.unique(new ConstantNode(__constant, __stamp.constant(__constant, __metaAccess)));
    }

    public static ConstantNode forConstant(Stamp __stamp, Constant __constant, int __stableDimension, boolean __isDefaultStable, MetaAccessProvider __metaAccess)
    {
        return new ConstantNode(__constant, __stamp.constant(__constant, __metaAccess), __stableDimension, __isDefaultStable);
    }

    public static ConstantNode forConstant(Stamp __stamp, Constant __constant, MetaAccessProvider __metaAccess)
    {
        return new ConstantNode(__constant, __stamp.constant(__constant, __metaAccess));
    }

    ///
    // Returns a node for a Java primitive.
    ///
    public static ConstantNode forPrimitive(JavaConstant __constant, StructuredGraph __graph)
    {
        return forConstant(__constant, null, __graph);
    }

    ///
    // Returns a node for a Java primitive.
    ///
    public static ConstantNode forPrimitive(JavaConstant __constant)
    {
        return forConstant(__constant, null);
    }

    ///
    // Returns a node for a primitive of a given type.
    ///
    public static ConstantNode forPrimitive(Stamp __stamp, JavaConstant __constant, StructuredGraph __graph)
    {
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __istamp = (IntegerStamp) __stamp;
            return forIntegerBits(__istamp.getBits(), __constant, __graph);
        }
        else
        {
            return forPrimitive(__constant, __graph);
        }
    }

    ///
    // Returns a node for a primitive of a given type.
    ///
    public static ConstantNode forPrimitive(Stamp __stamp, Constant __constant)
    {
        if (__stamp instanceof IntegerStamp)
        {
            PrimitiveConstant __primitive = (PrimitiveConstant) __constant;
            IntegerStamp __istamp = (IntegerStamp) __stamp;
            return forIntegerBits(__istamp.getBits(), __primitive);
        }
        else
        {
            return new ConstantNode(__constant, __stamp.constant(__constant, null));
        }
    }

    ///
    // Returns a node for an long constant.
    //
    // @param i the long value for which to create the instruction
    // @return a node for an long constant
    ///
    public static ConstantNode forLong(long __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forLong(__i)));
    }

    ///
    // Returns a node for an long constant.
    //
    // @param i the long value for which to create the instruction
    // @return a node for an long constant
    ///
    public static ConstantNode forLong(long __i)
    {
        return createPrimitive(JavaConstant.forLong(__i));
    }

    ///
    // Returns a node for an integer constant.
    //
    // @param i the integer value for which to create the instruction
    // @return a node for an integer constant
    ///
    public static ConstantNode forInt(int __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forInt(__i)));
    }

    ///
    // Returns a node for an integer constant.
    //
    // @param i the integer value for which to create the instruction
    // @return a node for an integer constant
    ///
    public static ConstantNode forInt(int __i)
    {
        return createPrimitive(JavaConstant.forInt(__i));
    }

    ///
    // Returns a node for a boolean constant.
    //
    // @param i the boolean value for which to create the instruction
    // @return a node representing the boolean
    ///
    public static ConstantNode forBoolean(boolean __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forInt(__i ? 1 : 0)));
    }

    ///
    // Returns a node for a boolean constant.
    //
    // @param i the boolean value for which to create the instruction
    // @return a node representing the boolean
    ///
    public static ConstantNode forBoolean(boolean __i)
    {
        return createPrimitive(JavaConstant.forInt(__i ? 1 : 0));
    }

    ///
    // Returns a node for a byte constant.
    //
    // @param i the byte value for which to create the instruction
    // @return a node representing the byte
    ///
    public static ConstantNode forByte(byte __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forInt(__i)));
    }

    ///
    // Returns a node for a char constant.
    //
    // @param i the char value for which to create the instruction
    // @return a node representing the char
    ///
    public static ConstantNode forChar(char __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forInt(__i)));
    }

    ///
    // Returns a node for a short constant.
    //
    // @param i the short value for which to create the instruction
    // @return a node representing the short
    ///
    public static ConstantNode forShort(short __i, StructuredGraph __graph)
    {
        return unique(__graph, createPrimitive(JavaConstant.forInt(__i)));
    }

    private static ConstantNode unique(StructuredGraph __graph, ConstantNode __node)
    {
        return __graph.unique(__node);
    }

    private static ConstantNode forIntegerBits(int __bits, JavaConstant __constant, StructuredGraph __graph)
    {
        long __value = __constant.asLong();
        long __bounds = CodeUtil.signExtend(__value, __bits);
        return unique(__graph, new ConstantNode(__constant, StampFactory.forInteger(__bits, __bounds, __bounds)));
    }

    ///
    // Returns a node for a constant integer that's not directly representable as Java primitive
    // (e.g. short).
    ///
    public static ConstantNode forIntegerBits(int __bits, long __value, StructuredGraph __graph)
    {
        return forIntegerBits(__bits, JavaConstant.forPrimitiveInt(__bits, __value), __graph);
    }

    private static ConstantNode forIntegerBits(int __bits, JavaConstant __constant)
    {
        long __value = __constant.asLong();
        long __bounds = CodeUtil.signExtend(__value, __bits);
        return new ConstantNode(__constant, StampFactory.forInteger(__bits, __bounds, __bounds));
    }

    ///
    // Returns a node for a constant integer that's not directly representable as Java primitive
    // (e.g. short).
    ///
    public static ConstantNode forIntegerBits(int __bits, long __value)
    {
        return forIntegerBits(__bits, JavaConstant.forPrimitiveInt(__bits, __value));
    }

    ///
    // Returns a node for a constant integer that's compatible to a given stamp.
    ///
    public static ConstantNode forIntegerStamp(Stamp __stamp, long __value, StructuredGraph __graph)
    {
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __intStamp = (IntegerStamp) __stamp;
            return forIntegerBits(__intStamp.getBits(), __value, __graph);
        }
        else
        {
            return forIntegerKind(__stamp.getStackKind(), __value, __graph);
        }
    }

    ///
    // Returns a node for a constant integer that's compatible to a given stamp.
    ///
    public static ConstantNode forIntegerStamp(Stamp __stamp, long __value)
    {
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __intStamp = (IntegerStamp) __stamp;
            return forIntegerBits(__intStamp.getBits(), __value);
        }
        else
        {
            return forIntegerKind(__stamp.getStackKind(), __value);
        }
    }

    public static ConstantNode forIntegerKind(JavaKind __kind, long __value, StructuredGraph __graph)
    {
        switch (__kind)
        {
            case Byte:
            case Short:
            case Int:
                return ConstantNode.forInt((int) __value, __graph);
            case Long:
                return ConstantNode.forLong(__value, __graph);
            default:
                throw GraalError.shouldNotReachHere("unknown kind " + __kind);
        }
    }

    public static ConstantNode forIntegerKind(JavaKind __kind, long __value)
    {
        switch (__kind)
        {
            case Byte:
            case Short:
            case Int:
                return createPrimitive(JavaConstant.forInt((int) __value));
            case Long:
                return createPrimitive(JavaConstant.forLong(__value));
            default:
                throw GraalError.shouldNotReachHere("unknown kind " + __kind);
        }
    }

    public static ConstantNode defaultForKind(JavaKind __kind, StructuredGraph __graph)
    {
        return unique(__graph, defaultForKind(__kind));
    }

    public static ConstantNode defaultForKind(JavaKind __kind)
    {
        switch (__kind)
        {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Int:
                return ConstantNode.forInt(0);
            case Long:
                return ConstantNode.forLong(0L);
            case Object:
                return ConstantNode.forConstant(JavaConstant.NULL_POINTER, null);
            default:
                return null;
        }
    }
}
