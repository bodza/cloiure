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
    public static final NodeClass<FloatingGuardedNode> TYPE = NodeClass.create(FloatingGuardedNode.class);

    @OptionalInput(InputType.Guard) protected GuardingNode guard;

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    // @cons
    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> c, Stamp stamp, GuardingNode guard)
    {
        super(c, stamp);
        this.guard = guard;
    }

    @Override
    public GuardingNode getGuard()
    {
        return guard;
    }

    @Override
    public void setGuard(GuardingNode guard)
    {
        updateUsagesInterface(this.guard, guard);
        this.guard = guard;
    }
}
