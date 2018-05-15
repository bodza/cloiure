package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.PrimitiveStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
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
 * Determines the index of the least significant "1" bit. Note that the result is undefined if the
 * input is zero.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class BitScanForwardNode extends UnaryNode implements ArithmeticLIRLowerable {

    public static final NodeClass<BitScanForwardNode> TYPE = NodeClass.create(BitScanForwardNode.class);

    public BitScanForwardNode(ValueNode value) {
        super(TYPE, StampFactory.forInteger(JavaKind.Int, 0, ((PrimitiveStamp) value.stamp(NodeView.DEFAULT)).getBits()), value);
        assert value.getStackKind() == JavaKind.Int || value.getStackKind() == JavaKind.Long;
    }

    @Override
    public Stamp foldStamp(Stamp newStamp) {
        assert newStamp.isCompatible(getValue().stamp(NodeView.DEFAULT));
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        int min;
        int max;
        long mask = CodeUtil.mask(valueStamp.getBits());
        int firstAlwaysSetBit = scan(valueStamp.downMask() & mask);
        int firstMaybeSetBit = scan(valueStamp.upMask() & mask);
        if (firstAlwaysSetBit == -1) {
            int lastMaybeSetBit = BitScanReverseNode.scan(valueStamp.upMask() & mask);
            min = firstMaybeSetBit;
            max = lastMaybeSetBit;
        } else {
            min = firstMaybeSetBit;
            max = firstAlwaysSetBit;
        }
        return StampFactory.forInteger(JavaKind.Int, min, max);
    }

    public static ValueNode tryFold(ValueNode value) {
        if (value.isConstant()) {
            JavaConstant c = value.asJavaConstant();
            if (c.asLong() != 0) {
                return ConstantNode.forInt(value.getStackKind() == JavaKind.Int ? scan(c.asInt()) : scan(c.asLong()));
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        ValueNode folded = tryFold(forValue);
        return folded != null ? folded : this;
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @param v
     * @return number of trailing zeros or -1 if {@code v} == 0.
     */
    public static int scan(long v) {
        if (v == 0) {
            return -1;
        }
        return Long.numberOfTrailingZeros(v);
    }

    /**
     * Utility method with defined return value for 0.
     *
     * @param v
     * @return number of trailing zeros or -1 if {@code v} == 0.
     */
    public static int scan(int v) {
        return scan(0xffffffffL & v);
    }

    /**
     * Raw intrinsic for bsf instruction.
     *
     * @param v
     * @return number of trailing zeros or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(long v);

    /**
     * Raw intrinsic for bsf instruction.
     *
     * @param v
     * @return number of trailing zeros or an undefined value if {@code v} == 0.
     */
    @NodeIntrinsic
    public static native int unsafeScan(int v);

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen) {
        builder.setResult(this, gen.emitBitScanForward(builder.operand(getValue())));
    }
}
