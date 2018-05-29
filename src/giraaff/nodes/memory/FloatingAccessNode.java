package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FloatingGuardedNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

// @class FloatingAccessNode
public abstract class FloatingAccessNode extends FloatingGuardedNode implements Access, MemoryAccess
{
    public static final NodeClass<FloatingAccessNode> TYPE = NodeClass.create(FloatingAccessNode.class);

    @Input(InputType.Association) AddressNode address;
    protected final LocationIdentity location;

    protected BarrierType barrierType;

    // @cons
    protected FloatingAccessNode(NodeClass<? extends FloatingAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        super(c, stamp);
        this.address = address;
        this.location = location;
    }

    // @cons
    protected FloatingAccessNode(NodeClass<? extends FloatingAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType)
    {
        super(c, stamp, guard);
        this.address = address;
        this.location = location;
        this.barrierType = barrierType;
    }

    @Override
    public AddressNode getAddress()
    {
        return address;
    }

    @Override
    public void setAddress(AddressNode address)
    {
        updateUsages(this.address, address);
        this.address = address;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return location;
    }

    @Override
    public BarrierType getBarrierType()
    {
        return barrierType;
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    public abstract FixedAccessNode asFixedNode();
}
