package giraaff.nodes.calc;

import java.nio.ByteOrder;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Produces the platform dependent first or second half of a long or double value as an int.
 */
public final class UnpackEndianHalfNode extends UnaryNode implements Lowerable
{
    public static final NodeClass<UnpackEndianHalfNode> TYPE = NodeClass.create(UnpackEndianHalfNode.class);

    private final boolean firstHalf;

    protected UnpackEndianHalfNode(ValueNode value, boolean firstHalf)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int), value);
        this.firstHalf = firstHalf;
    }

    @SuppressWarnings("unused")
    public static ValueNode create(ValueNode value, boolean firstHalf, NodeView view)
    {
        if (value.isConstant() && value.asConstant().isDefaultForKind())
        {
            return ConstantNode.defaultForKind(JavaKind.Int);
        }
        return new UnpackEndianHalfNode(value, firstHalf);
    }

    public boolean isFirstHalf()
    {
        return firstHalf;
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant() && forValue.asConstant().isDefaultForKind())
        {
            return ConstantNode.defaultForKind(stamp.getStackKind());
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public void lower(ByteOrder byteOrder)
    {
        ValueNode result = value;
        if (value.getStackKind() == JavaKind.Double)
        {
            result = graph().unique(new ReinterpretNode(JavaKind.Long, value));
        }
        if ((byteOrder == ByteOrder.BIG_ENDIAN) == firstHalf)
        {
            result = graph().unique(new UnsignedRightShiftNode(result, ConstantNode.forInt(32, graph())));
        }
        result = IntegerConvertNode.convert(result, StampFactory.forKind(JavaKind.Int), graph(), NodeView.DEFAULT);
        replaceAtUsagesAndDelete(result);
    }
}
