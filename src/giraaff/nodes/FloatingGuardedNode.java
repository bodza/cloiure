package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GuardedNode;
import giraaff.nodes.extended.GuardingNode;

// @class FloatingGuardedNode
public abstract class FloatingGuardedNode extends FloatingNode implements GuardedNode
{
    // @def
    public static final NodeClass<FloatingGuardedNode> TYPE = NodeClass.create(FloatingGuardedNode.class);

    @OptionalInput(InputType.Guard)
    // @field
    protected GuardingNode guard;

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> __c, Stamp __stamp, GuardingNode __guard)
    {
        super(__c, __stamp);
        this.guard = __guard;
    }

    @Override
    public GuardingNode getGuard()
    {
        return guard;
    }

    @Override
    public void setGuard(GuardingNode __guard)
    {
        updateUsagesInterface(this.guard, __guard);
        this.guard = __guard;
    }
}
