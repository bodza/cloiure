package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.Stamp;
import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool.RoundingMode;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Round floating-point value.
 */
public final class AMD64RoundNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<AMD64RoundNode> TYPE = NodeClass.create(AMD64RoundNode.class);

    private final RoundingMode mode;

    public AMD64RoundNode(ValueNode value, RoundingMode mode)
    {
        super(TYPE, roundStamp((FloatStamp) value.stamp(NodeView.DEFAULT), mode), value);
        this.mode = mode;
    }

    private static double round(RoundingMode mode, double input)
    {
        switch (mode)
        {
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

    private static FloatStamp roundStamp(FloatStamp stamp, RoundingMode mode)
    {
        double min = stamp.lowerBound();
        min = Math.min(min, round(mode, min));

        double max = stamp.upperBound();
        max = Math.max(max, round(mode, max));

        return new FloatStamp(stamp.getBits(), min, max, stamp.isNonNaN());
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return roundStamp((FloatStamp) newStamp, mode);
    }

    public ValueNode tryFold(ValueNode input)
    {
        if (input.isConstant())
        {
            JavaConstant c = input.asJavaConstant();
            if (c.getJavaKind() == JavaKind.Double)
            {
                return ConstantNode.forDouble(round(mode, c.asDouble()));
            }
            else if (c.getJavaKind() == JavaKind.Float)
            {
                return ConstantNode.forFloat((float) round(mode, c.asFloat()));
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode folded = tryFold(forValue);
        return folded != null ? folded : this;
    }

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen)
    {
        builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) gen).emitRound(builder.operand(getValue()), mode));
    }
}
