package giraaff.nodes.calc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.SerializableConstant;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.ArithmeticStamp;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * The {@code ReinterpretNode} class represents a reinterpreting conversion that changes the stamp
 * of a primitive value to some other incompatible stamp. The new stamp must have the same width as
 * the old stamp.
 */
// @class ReinterpretNode
public final class ReinterpretNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<ReinterpretNode> TYPE = NodeClass.create(ReinterpretNode.class);

    // @cons
    protected ReinterpretNode(JavaKind __to, ValueNode __value)
    {
        this(StampFactory.forKind(__to), __value);
    }

    // @cons
    protected ReinterpretNode(Stamp __to, ValueNode __value)
    {
        super(TYPE, getReinterpretStamp(__to, __value.stamp(NodeView.DEFAULT)), __value);
    }

    public static ValueNode create(JavaKind __to, ValueNode __value, NodeView __view)
    {
        return create(StampFactory.forKind(__to), __value, __view);
    }

    public static ValueNode create(Stamp __to, ValueNode __value, NodeView __view)
    {
        return canonical(null, __to, __value, __view);
    }

    private static SerializableConstant evalConst(Stamp __stamp, SerializableConstant __c)
    {
        // We don't care about byte order here. Either would produce the correct result.
        ByteBuffer __buffer = ByteBuffer.wrap(new byte[__c.getSerializedSize()]).order(ByteOrder.nativeOrder());
        __c.serialize(__buffer);

        __buffer.rewind();
        return ((ArithmeticStamp) __stamp).deserialize(__buffer);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        NodeView __view = NodeView.from(__tool);
        return canonical(this, this.stamp(__view), __forValue, __view);
    }

    public static ValueNode canonical(ReinterpretNode __node, Stamp __forStamp, ValueNode __forValue, NodeView __view)
    {
        if (__forValue.isConstant())
        {
            return ConstantNode.forConstant(__forStamp, evalConst(__forStamp, (SerializableConstant) __forValue.asConstant()), null);
        }
        if (__forStamp.isCompatible(__forValue.stamp(__view)))
        {
            return __forValue;
        }
        if (__forValue instanceof ReinterpretNode)
        {
            ReinterpretNode __reinterpret = (ReinterpretNode) __forValue;
            return new ReinterpretNode(__forStamp, __reinterpret.getValue());
        }
        return __node != null ? __node : new ReinterpretNode(__forStamp, __forValue);
    }

    /**
     * Compute the {@link IntegerStamp} from a {@link FloatStamp}, losing as little information as possible.
     *
     * Sorting by their bit pattern reinterpreted as signed integers gives the following order of
     * floating point numbers:
     *
     * -0 | negative numbers | -Inf | NaNs | 0 | positive numbers | +Inf | NaNs
     *
     * So we can compute a better integer range if we know that the input is positive, negative,
     * finite, non-zero and/or not NaN.
     */
    private static IntegerStamp floatToInt(FloatStamp __stamp)
    {
        int __bits = __stamp.getBits();

        long __signBit = 1L << (__bits - 1);
        long __exponentMask;
        if (__bits == 64)
        {
            __exponentMask = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
        }
        else
        {
            __exponentMask = Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
        }

        long __positiveInfinity = __exponentMask;
        long __negativeInfinity = CodeUtil.signExtend(__signBit | __positiveInfinity, __bits);
        long __negativeZero = CodeUtil.signExtend(__signBit | 0, __bits);

        if (__stamp.isNaN())
        {
            // special case: in addition to the range, we know NaN has all exponent bits set
            return IntegerStamp.create(__bits, __negativeInfinity + 1, CodeUtil.maxValue(__bits), __exponentMask, CodeUtil.mask(__bits));
        }

        long __upperBound;
        if (__stamp.isNonNaN())
        {
            if (__stamp.upperBound() < 0.0)
            {
                if (__stamp.lowerBound() > Double.NEGATIVE_INFINITY)
                {
                    __upperBound = __negativeInfinity - 1;
                }
                else
                {
                    __upperBound = __negativeInfinity;
                }
            }
            else if (__stamp.upperBound() == 0.0)
            {
                __upperBound = 0;
            }
            else if (__stamp.upperBound() < Double.POSITIVE_INFINITY)
            {
                __upperBound = __positiveInfinity - 1;
            }
            else
            {
                __upperBound = __positiveInfinity;
            }
        }
        else
        {
            __upperBound = CodeUtil.maxValue(__bits);
        }

        long __lowerBound;
        if (__stamp.lowerBound() > 0.0)
        {
            if (__stamp.isNonNaN())
            {
                __lowerBound = 1;
            }
            else
            {
                __lowerBound = __negativeInfinity + 1;
            }
        }
        else if (__stamp.upperBound() == Double.NEGATIVE_INFINITY)
        {
            __lowerBound = __negativeInfinity;
        }
        else if (__stamp.upperBound() < 0.0)
        {
            __lowerBound = __negativeZero + 1;
        }
        else
        {
            __lowerBound = __negativeZero;
        }

        return StampFactory.forInteger(__bits, __lowerBound, __upperBound);
    }

    /**
     * Compute the {@link IntegerStamp} from a {@link FloatStamp}, losing as little information as possible.
     *
     * Sorting by their bit pattern reinterpreted as signed integers gives the following order of
     * floating point numbers:
     *
     * -0 | negative numbers | -Inf | NaNs | 0 | positive numbers | +Inf | NaNs
     *
     * So from certain integer ranges we may be able to infer something about the sign, finiteness
     * or NaN-ness of the result.
     */
    private static FloatStamp intToFloat(IntegerStamp __stamp)
    {
        int __bits = __stamp.getBits();

        double __minPositive;
        double __maxPositive;

        long __signBit = 1L << (__bits - 1);
        long __exponentMask;
        if (__bits == 64)
        {
            __exponentMask = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
            __minPositive = Double.MIN_VALUE;
            __maxPositive = Double.MAX_VALUE;
        }
        else
        {
            __exponentMask = Float.floatToRawIntBits(Float.POSITIVE_INFINITY);
            __minPositive = Float.MIN_VALUE;
            __maxPositive = Float.MAX_VALUE;
        }

        long __significandMask = CodeUtil.mask(__bits) & ~(__signBit | __exponentMask);

        long __positiveInfinity = __exponentMask;
        long __negativeInfinity = CodeUtil.signExtend(__signBit | __positiveInfinity, __bits);
        long __negativeZero = CodeUtil.signExtend(__signBit | 0, __bits);

        if ((__stamp.downMask() & __exponentMask) == __exponentMask && (__stamp.downMask() & __significandMask) != 0)
        {
            // if all exponent bits and at least one significand bit are set, the result is NaN
            return new FloatStamp(__bits, Double.NaN, Double.NaN, false);
        }

        double __upperBound;
        if (__stamp.upperBound() < __negativeInfinity)
        {
            if (__stamp.lowerBound() > __negativeZero)
            {
                __upperBound = -__minPositive;
            }
            else
            {
                __upperBound = -0.0;
            }
        }
        else if (__stamp.upperBound() < 0)
        {
            if (__stamp.lowerBound() > __negativeInfinity)
            {
                return new FloatStamp(__bits, Double.NaN, Double.NaN, false);
            }
            else if (__stamp.lowerBound() == __negativeInfinity)
            {
                __upperBound = Double.NEGATIVE_INFINITY;
            }
            else if (__stamp.lowerBound() > __negativeZero)
            {
                __upperBound = -__minPositive;
            }
            else
            {
                __upperBound = -0.0;
            }
        }
        else if (__stamp.upperBound() == 0)
        {
            __upperBound = 0.0;
        }
        else if (__stamp.upperBound() < __positiveInfinity)
        {
            __upperBound = __maxPositive;
        }
        else
        {
            __upperBound = Double.POSITIVE_INFINITY;
        }

        double __lowerBound;
        if (__stamp.lowerBound() > __positiveInfinity)
        {
            return new FloatStamp(__bits, Double.NaN, Double.NaN, false);
        }
        else if (__stamp.lowerBound() == __positiveInfinity)
        {
            __lowerBound = Double.POSITIVE_INFINITY;
        }
        else if (__stamp.lowerBound() > 0)
        {
            __lowerBound = __minPositive;
        }
        else if (__stamp.lowerBound() > __negativeInfinity)
        {
            __lowerBound = 0.0;
        }
        else
        {
            __lowerBound = Double.NEGATIVE_INFINITY;
        }

        boolean __nonNaN;
        if ((__stamp.upMask() & __exponentMask) != __exponentMask)
        {
            // NaN has all exponent bits set
            __nonNaN = true;
        }
        else
        {
            boolean __negativeNaNBlock = __stamp.lowerBound() < 0 && __stamp.upperBound() > __negativeInfinity;
            boolean __positiveNaNBlock = __stamp.upperBound() > __positiveInfinity;
            __nonNaN = !__negativeNaNBlock && !__positiveNaNBlock;
        }

        return new FloatStamp(__bits, __lowerBound, __upperBound, __nonNaN);
    }

    private static Stamp getReinterpretStamp(Stamp __toStamp, Stamp __fromStamp)
    {
        if (__toStamp instanceof IntegerStamp && __fromStamp instanceof FloatStamp)
        {
            return floatToInt((FloatStamp) __fromStamp);
        }
        else if (__toStamp instanceof FloatStamp && __fromStamp instanceof IntegerStamp)
        {
            return intToFloat((IntegerStamp) __fromStamp);
        }
        else
        {
            return __toStamp;
        }
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(getReinterpretStamp(stamp(NodeView.DEFAULT), getValue().stamp(NodeView.DEFAULT)));
    }

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        LIRKind __kind = __builder.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        __builder.setResult(this, __gen.emitReinterpret(__kind, __builder.operand(getValue())));
    }

    public static ValueNode reinterpret(JavaKind __toKind, ValueNode __value)
    {
        return __value.graph().unique(new ReinterpretNode(__toKind, __value));
    }
}
