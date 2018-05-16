package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.word.LocationIdentity;

/**
 * Accesses a value at an memory address specified by an {@linkplain #address address}. The access
 * does not include a null check on the object.
 */
@NodeInfo
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

    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        this(c, address, location, stamp, BarrierType.NONE);
    }

    protected FixedAccessNode(NodeClass<? extends FixedAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, BarrierType barrierType)
    {
        this(c, address, location, stamp, null, barrierType, false, null);
    }

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
