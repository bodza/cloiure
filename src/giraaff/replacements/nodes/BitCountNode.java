package giraaff.replacements.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
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

// @class BitCountNode
public final class BitCountNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<BitCountNode> TYPE = NodeClass.create(BitCountNode.class);

    // @cons
    public BitCountNode(ValueNode value)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), value), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        ValueNode theValue = getValue();
        return computeStamp(newStamp, theValue);
    }

    static Stamp computeStamp(Stamp newStamp, ValueNode theValue)
    {
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        return StampFactory.forInteger(JavaKind.Int, Long.bitCount(valueStamp.downMask()), Long.bitCount(valueStamp.upMask()));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            JavaConstant c = forValue.asJavaConstant();
            return ConstantNode.forInt(forValue.getStackKind() == JavaKind.Int ? Integer.bitCount(c.asInt()) : Long.bitCount(c.asLong()));
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen)
    {
        builder.setResult(this, gen.emitBitCount(builder.operand(getValue())));
    }
}
