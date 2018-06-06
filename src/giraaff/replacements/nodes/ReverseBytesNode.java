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
    // @def
    public static final NodeClass<ReverseBytesNode> TYPE = NodeClass.create(ReverseBytesNode.class);

    // @cons ReverseBytesNode
    public ReverseBytesNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forKind(__value.getStackKind()), __value);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        IntegerStamp __valueStamp = (IntegerStamp) __newStamp;
        if (getStackKind() == JavaKind.Int)
        {
            long __mask = CodeUtil.mask(JavaKind.Int.getBitCount());
            return IntegerStamp.stampForMask(__valueStamp.getBits(), Integer.reverse((int) __valueStamp.downMask()) & __mask, Integer.reverse((int) __valueStamp.upMask()) & __mask);
        }
        else if (getStackKind() == JavaKind.Long)
        {
            return IntegerStamp.stampForMask(__valueStamp.getBits(), Long.reverse(__valueStamp.downMask()), Long.reverse(__valueStamp.upMask()));
        }
        else
        {
            return stamp(NodeView.DEFAULT);
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__forValue.isConstant())
        {
            JavaConstant __c = __forValue.asJavaConstant();
            long __reversed = getStackKind() == JavaKind.Int ? Integer.reverseBytes(__c.asInt()) : Long.reverseBytes(__c.asLong());
            return ConstantNode.forIntegerKind(getStackKind(), __reversed);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = __gen.getLIRGeneratorTool().emitByteSwap(__gen.operand(getValue()));
        __gen.setResult(this, __result);
    }
}
