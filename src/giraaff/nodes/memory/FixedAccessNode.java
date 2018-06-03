package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

/**
 * Accesses a value at an memory address specified by an {@linkplain #address address}. The access
 * does not include a null check on the object.
 */
// @class FixedAccessNode
public abstract class FixedAccessNode extends DeoptimizingFixedWithNextNode implements Access, IterableNodeType
{
    // @def
    public static final NodeClass<FixedAccessNode> TYPE = NodeClass.create(FixedAccessNode.class);

    @OptionalInput(InputType.Guard)
    // @field
    protected GuardingNode guard;

    @Input(InputType.Association)
    // @field
    AddressNode address;
    // @field
    protected final LocationIdentity location;

    // @field
    protected boolean nullCheck;
    // @field
    protected BarrierType barrierType;

    @Override
    public AddressNode getAddress()
    {
        return address;
    }

    @Override
    public void setAddress(AddressNode __address)
    {
        updateUsages(this.address, __address);
        this.address = __address;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return location;
    }

    public boolean getNullCheck()
    {
        return nullCheck;
    }

    public void setNullCheck(boolean __check)
    {
        this.nullCheck = __check;
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp)
    {
        this(__c, __address, __location, __stamp, BarrierType.NONE);
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, BarrierType __barrierType)
    {
        this(__c, __address, __location, __stamp, null, __barrierType, false, null);
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, GuardingNode __guard, BarrierType __barrierType, boolean __nullCheck, FrameState __stateBefore)
    {
        super(__c, __stamp, __stateBefore);
        this.address = __address;
        this.location = __location;
        this.guard = __guard;
        this.barrierType = __barrierType;
        this.nullCheck = __nullCheck;
    }

    @Override
    public boolean canDeoptimize()
    {
        return nullCheck;
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

    @Override
    public BarrierType getBarrierType()
    {
        return barrierType;
    }
}
