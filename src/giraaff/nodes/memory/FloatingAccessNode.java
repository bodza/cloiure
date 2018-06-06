package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FloatingGuardedNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

// @class FloatingAccessNode
public abstract class FloatingAccessNode extends FloatingGuardedNode implements Access, MemoryAccess
{
    // @def
    public static final NodeClass<FloatingAccessNode> TYPE = NodeClass.create(FloatingAccessNode.class);

    @Node.Input(InputType.Association)
    // @field
    AddressNode ___address;
    // @field
    protected final LocationIdentity ___location;

    // @field
    protected HeapAccess.BarrierType ___barrierType;

    // @cons FloatingAccessNode
    protected FloatingAccessNode(NodeClass<? extends FloatingAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp)
    {
        super(__c, __stamp);
        this.___address = __address;
        this.___location = __location;
    }

    // @cons FloatingAccessNode
    protected FloatingAccessNode(NodeClass<? extends FloatingAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, GuardingNode __guard, HeapAccess.BarrierType __barrierType)
    {
        super(__c, __stamp, __guard);
        this.___address = __address;
        this.___location = __location;
        this.___barrierType = __barrierType;
    }

    @Override
    public AddressNode getAddress()
    {
        return this.___address;
    }

    @Override
    public void setAddress(AddressNode __address)
    {
        updateUsages(this.___address, __address);
        this.___address = __address;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___location;
    }

    @Override
    public HeapAccess.BarrierType getBarrierType()
    {
        return this.___barrierType;
    }

    @Override
    public boolean canNullCheck()
    {
        return true;
    }

    public abstract FixedAccessNode asFixedNode();
}
