package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.memory.MemoryCheckpoint;

// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class KillingBeginNode
public final class KillingBeginNode extends AbstractBeginNode implements MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<KillingBeginNode> TYPE = NodeClass.create(KillingBeginNode.class);

    // @field
    protected LocationIdentity ___locationIdentity;

    // @cons KillingBeginNode
    public KillingBeginNode(LocationIdentity __locationIdentity)
    {
        super(TYPE);
        this.___locationIdentity = __locationIdentity;
    }

    public static AbstractBeginNode begin(FixedNode __with, LocationIdentity __locationIdentity)
    {
        if (__with instanceof KillingBeginNode)
        {
            return (KillingBeginNode) __with;
        }
        AbstractBeginNode __begin = __with.graph().add(KillingBeginNode.create(__locationIdentity));
        __begin.setNext(__with);
        return __begin;
    }

    public static AbstractBeginNode create(LocationIdentity __locationIdentity)
    {
        return new KillingBeginNode(__locationIdentity);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }
}
