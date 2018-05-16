package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.GuardedNode;
import graalvm.compiler.nodes.extended.GuardingNode;

@NodeInfo
public abstract class FloatingGuardedNode extends FloatingNode implements GuardedNode
{
    public static final NodeClass<FloatingGuardedNode> TYPE = NodeClass.create(FloatingGuardedNode.class);

    @OptionalInput(InputType.Guard) protected GuardingNode guard;

    protected FloatingGuardedNode(NodeClass<? extends FloatingGuardedNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

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
