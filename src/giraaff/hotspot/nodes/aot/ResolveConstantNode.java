package giraaff.hotspot.nodes.aot;

import giraaff.graph.NodeClass;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class ResolveConstantNode
public final class ResolveConstantNode extends DeoptimizingFixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<ResolveConstantNode> TYPE = NodeClass.create(ResolveConstantNode.class);

    @Input
    // @field
    ValueNode value;
    // @field
    protected HotSpotConstantLoadAction action;

    // @cons
    public ResolveConstantNode(ValueNode __value, HotSpotConstantLoadAction __action)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.action = __action;
    }

    // @cons
    public ResolveConstantNode(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    public ValueNode value()
    {
        return value;
    }

    public HotSpotConstantLoadAction action()
    {
        return action;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
