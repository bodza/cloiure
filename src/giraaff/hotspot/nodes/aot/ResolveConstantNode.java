package giraaff.hotspot.nodes.aot;

import giraaff.graph.NodeClass;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public class ResolveConstantNode extends DeoptimizingFixedWithNextNode implements Lowerable
{
    public static final NodeClass<ResolveConstantNode> TYPE = NodeClass.create(ResolveConstantNode.class);

    @Input ValueNode value;
    protected HotSpotConstantLoadAction action;

    public ResolveConstantNode(ValueNode value, HotSpotConstantLoadAction action)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.action = action;
    }

    public ResolveConstantNode(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
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
