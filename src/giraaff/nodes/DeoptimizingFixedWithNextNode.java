package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

// @class DeoptimizingFixedWithNextNode
public abstract class DeoptimizingFixedWithNextNode extends FixedWithNextNode implements DeoptimizingNode.DeoptBefore
{
    // @def
    public static final NodeClass<DeoptimizingFixedWithNextNode> TYPE = NodeClass.create(DeoptimizingFixedWithNextNode.class);

    @OptionalInput(InputType.State)
    // @field
    protected FrameState stateBefore;

    // @cons
    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    // @cons
    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> __c, Stamp __stamp, FrameState __stateBefore)
    {
        super(__c, __stamp);
        this.stateBefore = __stateBefore;
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(stateBefore, __f);
        stateBefore = __f;
    }
}
