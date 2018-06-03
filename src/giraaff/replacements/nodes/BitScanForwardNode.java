package giraaff.replacements.nodes;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Determines the index of the least significant "1" bit. Note that the result is undefined if the
 * input is zero.
 */
// @class BitScanForwardNode
public final class BitScanForwardNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<BitScanForwardNode> TYPE = NodeClass.create(BitScanForwardNode.class);

    // @cons
    public BitScanForwardNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forInteger(JavaKind.Int, 0, ((PrimitiveStamp) __value.stamp(NodeView.DEFAULT)).getBits()), __value);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        IntegerStamp __valueStamp = (IntegerStamp) __newStamp;
        int __min;
        int __max;
        long __mask = CodeUtil.mask(__valueStamp.getBits());
        int __firstAlwaysSetBit = scan(__valueStamp.downMask() & __mask);
        int __firstMaybeSetBit = scan(__valueStamp.upMask() & __mask);
        if (__firstAlwaysSetBit == -1)
        {
            int __lastMaybeSetBit = BitScanReverseNode.scan(__valueStamp.upMask() & __mask);
            __min = __firstMaybeSetBit;
            __max = __lastMaybeSetBit;
        }
        else
        {
            __min = __firstMaybeSetBit;
            __max = __firstAlwaysSetBit;
        }
        return StampFactory.forInteger(JavaKind.Int, __min, __max);
    }

    public static ValueNode tryFold(ValueNode __value)
    {
        if (__value.isConstant())
        {
            JavaConstant __c = __value.asJavaConstant();
            if (__c.asLong() != 0)
            {
                return ConstantNode.forInt(__value.getStackKind() == JavaKind.Int ? scan(__c.asInt()) : scan(__c.asLong()));
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __folded = tryFold(__forValue);
        return __folded != null ? __folded : this;
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @return number of trailing zeros or -1 if {@code v} == 0.
     */
    public static int scan(long __v)
    {
        if (__v == 0)
        {
            return -1;
        }
        return Long.numberOfTrailingZeros(__v);
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @return number of trailing zeros or -1 if {@code v} == 0.
     */
    public static int scan(int __v)
    {
        return scan(0xffffffffL & __v);
    }

    /**
     * Raw intrinsic for bsf instruction.
     *
     * @return number of trailing zeros or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(long v);

    /**
     * Raw intrinsic for bsf instruction.
     *
     * @return number of trailing zeros or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(int v);

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        __builder.setResult(this, __gen.emitBitScanForward(__builder.operand(getValue())));
    }
}
