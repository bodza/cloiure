package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FloatingGuardedNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.word.LocationIdentity;

public abstract class FloatingAccessNode extends FloatingGuardedNode implements Access, MemoryAccess
{
    public static final NodeClass<FloatingAccessNode> TYPE = NodeClass.create(FloatingAccessNode.class);

    @Input(InputType.Association) AddressNode address;
    protected final LocationIdentity location;

    protected BarrierType barrierType;

    protected FloatingAccessNode(NodeClass<? extends FloatingAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        super(c, stamp);
        this.address = address;
        this.location = location;
    }

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
