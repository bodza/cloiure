package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

// @class DeoptimizingFixedWithNextNode
public abstract class DeoptimizingFixedWithNextNode extends FixedWithNextNode implements DeoptimizingNode.DeoptBefore
{
    // @def
    public static final NodeClass<DeoptimizingFixedWithNextNode> TYPE = NodeClass.create(DeoptimizingFixedWithNextNode.class);

    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateBefore;

    // @cons DeoptimizingFixedWithNextNode
    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    // @cons DeoptimizingFixedWithNextNode
    protected DeoptimizingFixedWithNextNode(NodeClass<? extends DeoptimizingFixedWithNextNode> __c, Stamp __stamp, FrameState __stateBefore)
    {
        super(__c, __stamp);
        this.___stateBefore = __stateBefore;
    }

    @Override
    public FrameState stateBefore()
    {
        return this.___stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(this.___stateBefore, __f);
        this.___stateBefore = __f;
    }
}
