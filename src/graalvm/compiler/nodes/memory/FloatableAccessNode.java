package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.word.LocationIdentity;

/**
 * An {@link FixedAccessNode} that can be converted to a {@link FloatingAccessNode}.
 */
public abstract class FloatableAccessNode extends FixedAccessNode
{
    public static final NodeClass<FloatableAccessNode> TYPE = NodeClass.create(FloatableAccessNode.class);

    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp)
    {
        super(c, address, location, stamp);
    }

    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> c, AddressNode address, LocationIdentity location, Stamp stamp, GuardingNode guard, BarrierType barrierType)
    {
        super(c, address, location, stamp, guard, barrierType, false, null);
    }

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
