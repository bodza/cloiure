package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

///
// An {@link FixedAccessNode} that can be converted to a {@link FloatingAccessNode}.
///
// @class FloatableAccessNode
public abstract class FloatableAccessNode extends FixedAccessNode
{
    // @def
    public static final NodeClass<FloatableAccessNode> TYPE = NodeClass.create(FloatableAccessNode.class);

    // @cons FloatableAccessNode
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp)
    {
        super(__c, __address, __location, __stamp);
    }

    // @cons FloatableAccessNode
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, GuardingNode __guard, HeapAccess.BarrierType __barrierType)
    {
        super(__c, __address, __location, __stamp, __guard, __barrierType, false, null);
    }

    // @cons FloatableAccessNode
    protected FloatableAccessNode(NodeClass<? extends FloatableAccessNode> __c, AddressNode __address, LocationIdentity __location, Stamp __stamp, GuardingNode __guard, HeapAccess.BarrierType __barrierType, boolean __nullCheck, FrameState __stateBefore)
    {
        super(__c, __address, __location, __stamp, __guard, __barrierType, __nullCheck, __stateBefore);
    }

    public abstract FloatingAccessNode asFloatingNode(MemoryNode __lastLocationAccess);

    // @field
    protected boolean ___forceFixed;

    public void setForceFixed(boolean __flag)
    {
        this.___forceFixed = __flag;
    }

    ///
    // AccessNodes can float only if their location identities are not ANY_LOCATION. Furthermore, in
    // case G1 is enabled any access (read) to the java.lang.ref.Reference.referent field which has
    // an attached write barrier with pre-semantics can not also float.
    ///
    public boolean canFloat()
    {
        return !this.___forceFixed && getLocationIdentity().isSingle() && getBarrierType() == HeapAccess.BarrierType.NONE;
    }
}
