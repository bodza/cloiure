package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * A floating read of a value from memory specified in terms of an object base and an object
 * relative location. This node does not null check the object.
 */
// @class FloatingReadNode
public final class FloatingReadNode extends FloatingAccessNode implements LIRLowerableAccess, Canonicalizable
{
    // @def
    public static final NodeClass<FloatingReadNode> TYPE = NodeClass.create(FloatingReadNode.class);

    @OptionalInput(InputType.Memory)
    // @field
    MemoryNode lastLocationAccess;

    // @cons
    public FloatingReadNode(AddressNode __address, LocationIdentity __location, MemoryNode __lastLocationAccess, Stamp __stamp)
    {
        this(__address, __location, __lastLocationAccess, __stamp, null, BarrierType.NONE);
    }

    // @cons
    public FloatingReadNode(AddressNode __address, LocationIdentity __location, MemoryNode __lastLocationAccess, Stamp __stamp, GuardingNode __guard)
    {
        this(__address, __location, __lastLocationAccess, __stamp, __guard, BarrierType.NONE);
    }

    // @cons
    public FloatingReadNode(AddressNode __address, LocationIdentity __location, MemoryNode __lastLocationAccess, Stamp __stamp, GuardingNode __guard, BarrierType __barrierType)
    {
        super(TYPE, __address, __location, __stamp, __guard, __barrierType);
        this.lastLocationAccess = __lastLocationAccess;

        // The input to floating reads must be always non-null or have at least a guard.
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __newlla)
    {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(__newlla));
        lastLocationAccess = __newlla;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRKind __readKind = __gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        __gen.setResult(this, __gen.getLIRGeneratorTool().getArithmetic().emitLoad(__readKind, __gen.operand(address), null));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        Node __result = ReadNode.canonicalizeRead(this, getAddress(), getLocationIdentity(), __tool);
        if (__result != this)
        {
            return __result;
        }
        if (__tool.canonicalizeReads() && getAddress().hasMoreThanOneUsage() && lastLocationAccess instanceof WriteNode)
        {
            WriteNode __write = (WriteNode) lastLocationAccess;
            if (__write.getAddress() == getAddress() && __write.getAccessStamp().isCompatible(getAccessStamp()))
            {
                // same memory location with no intervening write
                return __write.value();
            }
        }
        return this;
    }

    @Override
    public FixedAccessNode asFixedNode()
    {
        ReadNode __result = graph().add(new ReadNode(getAddress(), getLocationIdentity(), stamp(NodeView.DEFAULT), getBarrierType()));
        __result.setGuard(getGuard());
        return __result;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return stamp(NodeView.DEFAULT);
    }
}
