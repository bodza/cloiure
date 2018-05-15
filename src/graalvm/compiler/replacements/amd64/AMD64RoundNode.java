package graalvm.compiler.replacements.amd64;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;

import graalvm.compiler.core.common.type.FloatStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import graalvm.compiler.lir.amd64.AMD64ArithmeticLIRGeneratorTool.RoundingMode;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Round floating-point value.
 */
@NodeInfo(cycles = CYCLES_1)
public final class AMD64RoundNode extends UnaryNode implements ArithmeticLIRLowerable {
    public static final NodeClass<AMD64RoundNode> TYPE = NodeClass.create(AMD64RoundNode.class);

    private final RoundingMode mode;

    public AMD64RoundNode(ValueNode value, RoundingMode mode) {
        super(TYPE, roundStamp((FloatStamp) value.stamp(NodeView.DEFAULT), mode), value);
        this.mode = mode;
    }

    private static double round(RoundingMode mode, double input) {
        switch (mode) {
            case DOWN:
                return Math.floor(input);
            case NEAREST:
                return Math.rint(input);
            case UP:
                return Math.ceil(input);
            case TRUNCATE:
                return (long) input;
            default:
                throw GraalError.unimplemented("unimplemented RoundingMode " + mode);
        }
    }

    private static FloatStamp roundStamp(FloatStamp stamp, RoundingMode mode) {
        double min = stamp.lowerBound();
        min = Math.min(min, round(mode, min));

        double max = stamp.upperBound();
        max = Math.max(max, round(mode, max));

        return new FloatStamp(stamp.getBits(), min, max, stamp.isNonNaN());
    }

    @Override
    public Stamp foldStamp(Stamp newStamp) {
        assert newStamp.isCompatible(getValue().stamp(NodeView.DEFAULT));
        return roundStamp((FloatStamp) newStamp, mode);
    }

    public ValueNode tryFold(ValueNode input) {
        if (input.isConstant()) {
            JavaConstant c = input.asJavaConstant();
            if (c.getJavaKind() == JavaKind.Double) {
                return ConstantNode.forDouble(round(mode, c.asDouble()));
            } else if (c.getJavaKind() == JavaKind.Float) {
                return ConstantNode.forFloat((float) round(mode, c.asFloat()));
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        ValueNode folded = tryFold(forValue);
        return folded != null ? folded : this;
    }

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen) {
        builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) gen).emitRound(builder.operand(getValue()), mode));
    }
}
