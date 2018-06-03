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

///
// Determines the index of the most significant "1" bit. Note that the result is undefined if the
// input is zero.
///
// @class BitScanReverseNode
public final class BitScanReverseNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<BitScanReverseNode> TYPE = NodeClass.create(BitScanReverseNode.class);

    // @cons
    public BitScanReverseNode(ValueNode __value)
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
        int __lastAlwaysSetBit = scan(__valueStamp.downMask() & __mask);
        if (__lastAlwaysSetBit == -1)
        {
            int __firstMaybeSetBit = BitScanForwardNode.scan(__valueStamp.upMask() & __mask);
            __min = __firstMaybeSetBit;
        }
        else
        {
            __min = __lastAlwaysSetBit;
        }
        int __lastMaybeSetBit = scan(__valueStamp.upMask() & __mask);
        __max = __lastMaybeSetBit;
        return StampFactory.forInteger(JavaKind.Int, __min, __max);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__forValue.isConstant())
        {
            JavaConstant __c = __forValue.asJavaConstant();
            if (__c.asLong() != 0)
            {
                return ConstantNode.forInt(__forValue.getStackKind() == JavaKind.Int ? scan(__c.asInt()) : scan(__c.asLong()));
            }
        }
        return this;
    }

    ///
    // Utility method with defined return value for 0.
    //
    // @return index of first set bit or -1 if {@code v} == 0.
    ///
    public static int scan(long __v)
    {
        return 63 - Long.numberOfLeadingZeros(__v);
    }

    ///
    // Utility method with defined return value for 0.
    //
    // @return index of first set bit or -1 if {@code v} == 0.
    ///
    public static int scan(int __v)
    {
        return 31 - Integer.numberOfLeadingZeros(__v);
    }

    ///
    // Raw intrinsic for bsr instruction.
    //
    // @return index of first set bit or an undefined value if {@code v} == 0.
    ///
    @NodeIntrinsic
    public static native int unsafeScan(int __v);

    ///
    // Raw intrinsic for bsr instruction.
    //
    // @return index of first set bit or an undefined value if {@code v} == 0.
    ///
    @NodeIntrinsic
    public static native int unsafeScan(long __v);

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        __builder.setResult(this, __gen.emitBitScanReverse(__builder.operand(getValue())));
    }
}
