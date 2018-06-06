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

///
// Produces the platform dependent first or second half of a long or double value as an int.
///
// @class UnpackEndianHalfNode
public final class UnpackEndianHalfNode extends UnaryNode implements Lowerable
{
    // @def
    public static final NodeClass<UnpackEndianHalfNode> TYPE = NodeClass.create(UnpackEndianHalfNode.class);

    // @field
    private final boolean ___firstHalf;

    // @cons UnpackEndianHalfNode
    protected UnpackEndianHalfNode(ValueNode __value, boolean __firstHalf)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int), __value);
        this.___firstHalf = __firstHalf;
    }

    @SuppressWarnings("unused")
    public static ValueNode create(ValueNode __value, boolean __firstHalf, NodeView __view)
    {
        if (__value.isConstant() && __value.asConstant().isDefaultForKind())
        {
            return ConstantNode.defaultForKind(JavaKind.Int);
        }
        return new UnpackEndianHalfNode(__value, __firstHalf);
    }

    public boolean isFirstHalf()
    {
        return this.___firstHalf;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__forValue.isConstant() && __forValue.asConstant().isDefaultForKind())
        {
            return ConstantNode.defaultForKind(this.___stamp.getStackKind());
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public void lower(ByteOrder __byteOrder)
    {
        ValueNode __result = this.___value;
        if ((__byteOrder == ByteOrder.BIG_ENDIAN) == this.___firstHalf)
        {
            __result = graph().unique(new UnsignedRightShiftNode(__result, ConstantNode.forInt(32, graph())));
        }
        __result = IntegerConvertNode.convert(__result, StampFactory.forKind(JavaKind.Int), graph(), NodeView.DEFAULT);
        replaceAtUsagesAndDelete(__result);
    }
}
