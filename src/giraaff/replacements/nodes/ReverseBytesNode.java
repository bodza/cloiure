package giraaff.replacements.nodes;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class ReverseBytesNode
public final class ReverseBytesNode extends UnaryNode implements LIRLowerable
{
    public static final NodeClass<ReverseBytesNode> TYPE = NodeClass.create(ReverseBytesNode.class);

    // @cons
    public ReverseBytesNode(ValueNode value)
    {
        super(TYPE, StampFactory.forKind(value.getStackKind()), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        if (getStackKind() == JavaKind.Int)
        {
            long mask = CodeUtil.mask(JavaKind.Int.getBitCount());
            return IntegerStamp.stampForMask(valueStamp.getBits(), Integer.reverse((int) valueStamp.downMask()) & mask, Integer.reverse((int) valueStamp.upMask()) & mask);
        }
        else if (getStackKind() == JavaKind.Long)
        {
            return IntegerStamp.stampForMask(valueStamp.getBits(), Long.reverse(valueStamp.downMask()), Long.reverse(valueStamp.upMask()));
        }
        else
        {
            return stamp(NodeView.DEFAULT);
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            JavaConstant c = forValue.asJavaConstant();
            long reversed = getStackKind() == JavaKind.Int ? Integer.reverseBytes(c.asInt()) : Long.reverseBytes(c.asLong());
            return ConstantNode.forIntegerKind(getStackKind(), reversed);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result = gen.getLIRGeneratorTool().emitByteSwap(gen.operand(getValue()));
        gen.setResult(this, result);
    }
}
