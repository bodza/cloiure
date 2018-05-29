package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

/**
 * An {@link FixedAccessNode} that can be converted to a {@link FloatingAccessNode}.
 */
// @class FloatableAccessNode
public abstract class FloatableAccessNode extends FixedAccessNode
{
    public static final NodeClass<FloatableAccessNode> TYPE = NodeClass.create(FloatableAccessNode.class);

    // @cons
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        super(c, address, location, stamp);
    }

    // @cons
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType)
    {
        super(c, address, location, stamp, guard, barrierType, false, null);
    }

    // @cons
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType, boolean nullCheck, FrameState stateBefore)
    {
        super(c, address, location, stamp, guard, barrierType, nullCheck, stateBefore);
    }

    public abstract FloatingAccessNode asFloatingNode(MemoryNode lastLocationAccess);

    protected boolean forceFixed;

    public void setForceFixed(boolean flag)
    {
        this.forceFixed = flag;
    }

    /**
     * AccessNodes can float only if their location identities are not ANY_LOCATION. Furthermore, in
     * case G1 is enabled any access (read) to the java.lang.ref.Reference.referent field which has
     * an attached write barrier with pre-semantics can not also float.
     */
    public boolean canFloat()
    {
        return !forceFixed && getLocationIdentity().isSingle() && getBarrierType() == BarrierType.NONE;
    }
}
