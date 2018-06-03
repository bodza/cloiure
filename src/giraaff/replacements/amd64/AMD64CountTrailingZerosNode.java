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

///
// Count the number of trailing zeros using the {@code tzcntq} or {@code tzcntl} instructions.
///
// @class AMD64CountTrailingZerosNode
public final class AMD64CountTrailingZerosNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<AMD64CountTrailingZerosNode> TYPE = NodeClass.create(AMD64CountTrailingZerosNode.class);

    // @cons
    public AMD64CountTrailingZerosNode(ValueNode __value)
    {
        super(TYPE, computeStamp(__value.stamp(NodeView.DEFAULT), __value), __value);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return computeStamp(__newStamp, getValue());
    }

    static Stamp computeStamp(Stamp __newStamp, ValueNode __value)
    {
        IntegerStamp __valueStamp = (IntegerStamp) __newStamp;
        return StampTool.stampForTrailingZeros(__valueStamp);
    }

    public static ValueNode tryFold(ValueNode __value)
    {
        if (__value.isConstant())
        {
            JavaConstant __c = __value.asJavaConstant();
            if (__value.getStackKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Integer.numberOfTrailingZeros(__c.asInt()));
            }
            else
            {
                return ConstantNode.forInt(Long.numberOfTrailingZeros(__c.asLong()));
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
        __builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) __gen).emitCountTrailingZeros(__builder.operand(getValue())));
    }
}
