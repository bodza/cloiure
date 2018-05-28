package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

public abstract class DeoptimizingFixedWithNextNode extends FixedWithNextNode implements DeoptimizingNode.DeoptBefore
{
    public static final NodeClass<DeoptimizingFixedWithNextNode> TYPE = NodeClass.create(DeoptimizingFixedWithNextNode.class);

    @OptionalInput(InputType.State) protected FrameState stateBefore;

    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> c, Stamp stamp, FrameState stateBefore)
    {
        super(c, stamp);
        this.stateBefore = stateBefore;
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState f)
    {
        updateUsages(stateBefore, f);
        stateBefore = f;
    }
}
