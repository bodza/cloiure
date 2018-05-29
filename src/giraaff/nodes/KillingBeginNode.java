package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.memory.MemoryCheckpoint;

// @NodeInfo.allowedUsageTypes "Memory"
// @class KillingBeginNode
public final class KillingBeginNode extends AbstractBeginNode implements MemoryCheckpoint.Single
{
    public static final NodeClass<KillingBeginNode> TYPE = NodeClass.create(KillingBeginNode.class);

    protected LocationIdentity locationIdentity;

    // @cons
    public KillingBeginNode(LocationIdentity locationIdentity)
    {
        super(TYPE);
        this.locationIdentity = locationIdentity;
    }

    public static AbstractBeginNode begin(FixedNode with, LocationIdentity locationIdentity)
    {
        if (with instanceof KillingBeginNode)
        {
            return (KillingBeginNode) with;
        }
        AbstractBeginNode begin = with.graph().add(KillingBeginNode.create(locationIdentity));
        begin.setNext(with);
        return begin;
    }

    public static AbstractBeginNode create(LocationIdentity locationIdentity)
    {
        return new KillingBeginNode(locationIdentity);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }
}
