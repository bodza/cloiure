package graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.ValueNode;

public class GuardedUnsafeLoadNode extends RawLoadNode implements GuardedNode
{
    public static final NodeClass<GuardedUnsafeLoadNode> TYPE = NodeClass.create(GuardedUnsafeLoadNode.class);

    @OptionalInput(InputType.Guard) protected GuardingNode guard;

    public GuardedUnsafeLoadNode(ValueNode object, ValueNode offset, JavaKind accessKind, LocationIdentity locationIdentity, GuardingNode guard)
    {
        super(TYPE, object, offset, accessKind, locationIdentity);
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
