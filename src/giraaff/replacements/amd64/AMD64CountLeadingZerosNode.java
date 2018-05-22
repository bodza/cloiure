package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.type.StampTool;

/**
 * Count the number of leading zeros using the {@code lzcntq} or {@code lzcntl} instructions.
 */
public final class AMD64CountLeadingZerosNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<AMD64CountLeadingZerosNode> TYPE = NodeClass.create(AMD64CountLeadingZerosNode.class);

    public AMD64CountLeadingZerosNode(ValueNode value)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), value), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return computeStamp(newStamp, getValue());
    }

    private static Stamp computeStamp(Stamp newStamp, ValueNode theValue)
    {
        return StampTool.stampForLeadingZeros((IntegerStamp) newStamp);
    }

    public static ValueNode tryFold(ValueNode value)
    {
        if (value.isConstant())
        {
            JavaConstant c = value.asJavaConstant();
            if (value.getStackKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Integer.numberOfLeadingZeros(c.asInt()));
            }
            else
            {
                return ConstantNode.forInt(Long.numberOfLeadingZeros(c.asLong()));
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
        builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) gen).emitCountLeadingZeros(builder.operand(getValue())));
    }
}
