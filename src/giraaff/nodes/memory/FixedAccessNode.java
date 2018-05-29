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
    public static final NodeClass<FixedAccessNode> TYPE = NodeClass.create(FixedAccessNode.class);

    @OptionalInput(InputType.Guard) protected GuardingNode guard;

    @Input(InputType.Association) AddressNode address;
    protected final LocationIdentity location;

    protected boolean nullCheck;
    protected BarrierType barrierType;

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

    public boolean getNullCheck()
    {
        return nullCheck;
    }

    public void setNullCheck(boolean check)
    {
        this.nullCheck = check;
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        this(c, address, location, stamp, BarrierType.NONE);
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, BarrierType barrierType)
    {
        this(c, address, location, stamp, null, barrierType, false, null);
    }

    // @cons
    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType, boolean nullCheck, FrameState stateBefore)
    {
        super(c, stamp, stateBefore);
        this.address = address;
        this.location = location;
        this.guard = guard;
        this.barrierType = barrierType;
        this.nullCheck = nullCheck;
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
    public void setGuard(GuardingNode guard)
    {
        updateUsagesInterface(this.guard, guard);
        this.guard = guard;
    }

    @Override
    public BarrierType getBarrierType()
    {
        return barrierType;
    }
}
