package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.Stamp;
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
import giraaff.util.GraalError;

/**
 * Round floating-point value.
 */
// @class AMD64RoundNode
public final class AMD64RoundNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<AMD64RoundNode> TYPE = NodeClass.create(AMD64RoundNode.class);

    // @field
    private final RoundingMode mode;

    // @cons
    public AMD64RoundNode(ValueNode __value, RoundingMode __mode)
    {
        super(TYPE, roundStamp((FloatStamp) __value.stamp(NodeView.DEFAULT), __mode), __value);
        this.mode = __mode;
    }

    private static double round(RoundingMode __mode, double __input)
    {
        switch (__mode)
        {
            case DOWN:
                return Math.floor(__input);
            case NEAREST:
                return Math.rint(__input);
            case UP:
                return Math.ceil(__input);
            case TRUNCATE:
                return (long) __input;
            default:
                throw GraalError.unimplemented("unimplemented RoundingMode " + __mode);
        }
    }

    private static FloatStamp roundStamp(FloatStamp __stamp, RoundingMode __mode)
    {
        double __min = __stamp.lowerBound();
        __min = Math.min(__min, round(__mode, __min));

        double __max = __stamp.upperBound();
        __max = Math.max(__max, round(__mode, __max));

        return new FloatStamp(__stamp.getBits(), __min, __max, __stamp.isNonNaN());
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return roundStamp((FloatStamp) __newStamp, mode);
    }

    public ValueNode tryFold(ValueNode __input)
    {
        if (__input.isConstant())
        {
            JavaConstant __c = __input.asJavaConstant();
            if (__c.getJavaKind() == JavaKind.Double)
            {
                return ConstantNode.forDouble(round(mode, __c.asDouble()));
            }
            else if (__c.getJavaKind() == JavaKind.Float)
            {
                return ConstantNode.forFloat((float) round(mode, __c.asFloat()));
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

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        __builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) __gen).emitRound(__builder.operand(getValue()), mode));
    }
}
