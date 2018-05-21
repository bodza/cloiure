package graalvm.compiler.replacements.nodes;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.PrimitiveStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Determines the index of the most significant "1" bit. Note that the result is undefined if the
 * input is zero.
 */
public final class BitScanReverseNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<BitScanReverseNode> TYPE = NodeClass.create(BitScanReverseNode.class);

    public BitScanReverseNode(ValueNode value)
    {
        super(TYPE, StampFactory.forInteger(JavaKind.Int, 0, ((PrimitiveStamp) value.stamp(NodeView.DEFAULT)).getBits()), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        int min;
        int max;
        long mask = CodeUtil.mask(valueStamp.getBits());
        int lastAlwaysSetBit = scan(valueStamp.downMask() & mask);
        if (lastAlwaysSetBit == -1)
        {
            int firstMaybeSetBit = BitScanForwardNode.scan(valueStamp.upMask() & mask);
            min = firstMaybeSetBit;
        }
        else
        {
            min = lastAlwaysSetBit;
        }
        int lastMaybeSetBit = scan(valueStamp.upMask() & mask);
        max = lastMaybeSetBit;
        return StampFactory.forInteger(JavaKind.Int, min, max);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            JavaConstant c = forValue.asJavaConstant();
            if (c.asLong() != 0)
            {
                return ConstantNode.forInt(forValue.getStackKind() == JavaKind.Int ? scan(c.asInt()) : scan(c.asLong()));
            }
        }
        return this;
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @return index of first set bit or -1 if {@code v} == 0.
     */
    public static int scan(long v)
    {
        return 63 - Long.numberOfLeadingZeros(v);
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @return index of first set bit or -1 if {@code v} == 0.
     */
    public static int scan(int v)
    {
        return 31 - Integer.numberOfLeadingZeros(v);
    }

    /**
     * Raw intrinsic for bsr instruction.
     *
     * @return index of first set bit or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(int v);

    /**
     * Raw intrinsic for bsr instruction.
     *
     * @return index of first set bit or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(long v);

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen)
    {
        builder.setResult(this, gen.emitBitScanReverse(builder.operand(getValue())));
    }
}
