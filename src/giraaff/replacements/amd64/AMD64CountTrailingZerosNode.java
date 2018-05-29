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
 * Count the number of trailing zeros using the {@code tzcntq} or {@code tzcntl} instructions.
 */
// @class AMD64CountTrailingZerosNode
public final class AMD64CountTrailingZerosNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<AMD64CountTrailingZerosNode> TYPE = NodeClass.create(AMD64CountTrailingZerosNode.class);

    // @cons
    public AMD64CountTrailingZerosNode(ValueNode value)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), value), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return computeStamp(newStamp, getValue());
    }

    static Stamp computeStamp(Stamp newStamp, ValueNode value)
    {
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        return StampTool.stampForTrailingZeros(valueStamp);
    }

    public static ValueNode tryFold(ValueNode value)
    {
        if (value.isConstant())
        {
            JavaConstant c = value.asJavaConstant();
            if (value.getStackKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Integer.numberOfTrailingZeros(c.asInt()));
            }
            else
            {
                return ConstantNode.forInt(Long.numberOfTrailingZeros(c.asLong()));
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
        builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) gen).emitCountTrailingZeros(builder.operand(getValue())));
    }
}
