package giraaff.nodes.extended;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;

// @class GuardedUnsafeLoadNode
public final class GuardedUnsafeLoadNode extends RawLoadNode implements GuardedNode
{
    // @def
    public static final NodeClass<GuardedUnsafeLoadNode> TYPE = NodeClass.create(GuardedUnsafeLoadNode.class);

    @OptionalInput(InputType.Guard)
    // @field
    protected GuardingNode guard;

    // @cons
    public GuardedUnsafeLoadNode(ValueNode __object, ValueNode __offset, JavaKind __accessKind, LocationIdentity __locationIdentity, GuardingNode __guard)
    {
        super(TYPE, __object, __offset, __accessKind, __locationIdentity);
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
