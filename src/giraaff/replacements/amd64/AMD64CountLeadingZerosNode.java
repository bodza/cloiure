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
// @class AMD64CountLeadingZerosNode
public final class AMD64CountLeadingZerosNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<AMD64CountLeadingZerosNode> TYPE = NodeClass.create(AMD64CountLeadingZerosNode.class);

    // @cons
    public AMD64CountLeadingZerosNode(ValueNode __value)
    {
        super(TYPE, computeStamp(__value.stamp(NodeView.DEFAULT), __value), __value);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return computeStamp(__newStamp, getValue());
    }

    private static Stamp computeStamp(Stamp __newStamp, ValueNode __theValue)
    {
        return StampTool.stampForLeadingZeros((IntegerStamp) __newStamp);
    }

    public static ValueNode tryFold(ValueNode __value)
    {
        if (__value.isConstant())
        {
            JavaConstant __c = __value.asJavaConstant();
            if (__value.getStackKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Integer.numberOfLeadingZeros(__c.asInt()));
            }
            else
            {
                return ConstantNode.forInt(Long.numberOfLeadingZeros(__c.asLong()));
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
        __builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) __gen).emitCountLeadingZeros(__builder.operand(getValue())));
    }
}
