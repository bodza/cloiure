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
    // @def
    public static final NodeClass<BitCountNode> TYPE = NodeClass.create(BitCountNode.class);

    // @cons BitCountNode
    public BitCountNode(ValueNode __value)
    {
        super(TYPE, computeStamp(__value.stamp(NodeView.DEFAULT), __value), __value);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        ValueNode __theValue = getValue();
        return computeStamp(__newStamp, __theValue);
    }

    static Stamp computeStamp(Stamp __newStamp, ValueNode __theValue)
    {
        IntegerStamp __valueStamp = (IntegerStamp) __newStamp;
        return StampFactory.forInteger(JavaKind.Int, Long.bitCount(__valueStamp.downMask()), Long.bitCount(__valueStamp.upMask()));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__forValue.isConstant())
        {
            JavaConstant __c = __forValue.asJavaConstant();
            return ConstantNode.forInt(__forValue.getStackKind() == JavaKind.Int ? Integer.bitCount(__c.asInt()) : Long.bitCount(__c.asLong()));
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        __builder.setResult(this, __gen.emitBitCount(__builder.operand(getValue())));
    }
}
