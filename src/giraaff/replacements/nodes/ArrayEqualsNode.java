package giraaff.replacements.nodes;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// Compares two arrays with the same length.
///
// @class ArrayEqualsNode
public final class ArrayEqualsNode extends FixedWithNextNode implements LIRLowerable, Canonicalizable, Virtualizable, MemoryAccess
{
    // @def
    public static final NodeClass<ArrayEqualsNode> TYPE = NodeClass.create(ArrayEqualsNode.class);

    /// {@link JavaKind} of the arrays to compare.
    // @field
    protected final JavaKind ___kind;

    /// One array to be tested for equality.
    @Input
    // @field
    ValueNode ___array1;

    /// The other array to be tested for equality.
    @Input
    // @field
    ValueNode ___array2;

    /// Length of both arrays.
    @Input
    // @field
    ValueNode ___length;

    @OptionalInput(InputType.Memory)
    // @field
    MemoryNode ___lastLocationAccess;

    // @cons
    public ArrayEqualsNode(ValueNode __array1, ValueNode __array2, ValueNode __length, @ConstantNodeParameter JavaKind __kind)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean));
        this.___kind = __kind;
        this.___array1 = __array1;
        this.___array2 = __array2;
        this.___length = __length;
    }

    public ValueNode getArray1()
    {
        return this.___array1;
    }

    public ValueNode getArray2()
    {
        return this.___array2;
    }

    public ValueNode getLength()
    {
        return this.___length;
    }

    private static boolean isNaNFloat(JavaConstant __constant)
    {
        JavaKind __kind = __constant.getJavaKind();
        return (__kind == JavaKind.Float && Float.isNaN(__constant.asFloat())) || (__kind == JavaKind.Double && Double.isNaN(__constant.asDouble()));
    }

    private static boolean arrayEquals(ConstantReflectionProvider __constantReflection, JavaConstant __a, JavaConstant __b, int __len)
    {
        for (int __i = 0; __i < __len; __i++)
        {
            JavaConstant __aElem = __constantReflection.readArrayElement(__a, __i);
            JavaConstant __bElem = __constantReflection.readArrayElement(__b, __i);
            if (!__constantReflection.constantEquals(__aElem, __bElem) && !(isNaNFloat(__aElem) && isNaNFloat(__bElem)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        ValueNode __a1 = GraphUtil.unproxify(this.___array1);
        ValueNode __a2 = GraphUtil.unproxify(this.___array2);
        if (__a1 == __a2)
        {
            return ConstantNode.forBoolean(true);
        }
        if (__a1.isConstant() && __a2.isConstant() && this.___length.isConstant())
        {
            ConstantNode __c1 = (ConstantNode) __a1;
            ConstantNode __c2 = (ConstantNode) __a2;
            if (__c1.getStableDimension() >= 1 && __c2.getStableDimension() >= 1)
            {
                boolean __ret = arrayEquals(__tool.getConstantReflection(), __c1.asJavaConstant(), __c2.asJavaConstant(), this.___length.asJavaConstant().asInt());
                return ConstantNode.forBoolean(__ret);
            }
        }
        return this;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias1 = __tool.getAlias(this.___array1);
        ValueNode __alias2 = __tool.getAlias(this.___array2);
        if (__alias1 == __alias2)
        {
            // the same virtual objects will always have the same contents
            __tool.replaceWithValue(ConstantNode.forBoolean(true, graph()));
        }
        else if (__alias1 instanceof VirtualObjectNode && __alias2 instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual1 = (VirtualObjectNode) __alias1;
            VirtualObjectNode __virtual2 = (VirtualObjectNode) __alias2;

            if (__virtual1.entryCount() == __virtual2.entryCount())
            {
                int __entryCount = __virtual1.entryCount();
                boolean __allEqual = true;
                for (int __i = 0; __i < __entryCount; __i++)
                {
                    ValueNode __entry1 = __tool.getEntry(__virtual1, __i);
                    ValueNode __entry2 = __tool.getEntry(__virtual2, __i);
                    if (__entry1 != __entry2)
                    {
                        if (__entry1 instanceof ConstantNode && __entry2 instanceof ConstantNode)
                        {
                            // Float NaN constants are different constant nodes but treated as
                            // equal in Arrays.equals([F[F) or Arrays.equals([D[D).
                            if (__entry1.getStackKind() == JavaKind.Float && __entry2.getStackKind() == JavaKind.Float)
                            {
                                float __value1 = ((JavaConstant) ((ConstantNode) __entry1).asConstant()).asFloat();
                                float __value2 = ((JavaConstant) ((ConstantNode) __entry2).asConstant()).asFloat();
                                if (Float.floatToIntBits(__value1) != Float.floatToIntBits(__value2))
                                {
                                    __allEqual = false;
                                }
                            }
                            else if (__entry1.getStackKind() == JavaKind.Double && __entry2.getStackKind() == JavaKind.Double)
                            {
                                double __value1 = ((JavaConstant) ((ConstantNode) __entry1).asConstant()).asDouble();
                                double __value2 = ((JavaConstant) ((ConstantNode) __entry2).asConstant()).asDouble();
                                if (Double.doubleToLongBits(__value1) != Double.doubleToLongBits(__value2))
                                {
                                    __allEqual = false;
                                }
                            }
                            else
                            {
                                __allEqual = false;
                            }
                        }
                        else
                        {
                            // the contents might be different
                            __allEqual = false;
                        }
                    }
                    if (__entry1.stamp(NodeView.DEFAULT).alwaysDistinct(__entry2.stamp(NodeView.DEFAULT)))
                    {
                        // the contents are different
                        __tool.replaceWithValue(ConstantNode.forBoolean(false, graph()));
                        return;
                    }
                }
                if (__allEqual)
                {
                    __tool.replaceWithValue(ConstantNode.forBoolean(true, graph()));
                }
            }
        }
    }

    @NodeIntrinsic
    public static native boolean equals(Object __array1, Object __array2, int __length, @ConstantNodeParameter JavaKind __kind);

    public static boolean equals(boolean[] __array1, boolean[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Boolean);
    }

    public static boolean equals(byte[] __array1, byte[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Byte);
    }

    public static boolean equals(char[] __array1, char[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Char);
    }

    public static boolean equals(short[] __array1, short[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Short);
    }

    public static boolean equals(int[] __array1, int[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Int);
    }

    public static boolean equals(long[] __array1, long[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Long);
    }

    public static boolean equals(float[] __array1, float[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Float);
    }

    public static boolean equals(double[] __array1, double[] __array2, int __length)
    {
        return equals(__array1, __array2, __length, JavaKind.Double);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = __gen.getLIRGeneratorTool().emitArrayEquals(this.___kind, __gen.operand(this.___array1), __gen.operand(this.___array2), __gen.operand(this.___length));
        __gen.setResult(this, __result);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return NamedLocationIdentity.getArrayLocation(this.___kind);
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return this.___lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsages(ValueNodeUtil.asNode(this.___lastLocationAccess), ValueNodeUtil.asNode(__lla));
        this.___lastLocationAccess = __lla;
    }
}
