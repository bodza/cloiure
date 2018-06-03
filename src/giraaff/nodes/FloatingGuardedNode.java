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
    protected GuardingNode ___guard;

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> __c, Stamp __stamp, GuardingNode __guard)
    {
        super(__c, __stamp);
        this.___guard = __guard;
    }

    @Override
    public GuardingNode getGuard()
    {
        return this.___guard;
    }

    @Override
    public void setGuard(GuardingNode __guard)
    {
        updateUsagesInterface(this.___guard, __guard);
        this.___guard = __guard;
    }
}
