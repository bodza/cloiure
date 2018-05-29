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
    public static final NodeClass<FloatingReadNode> TYPE = NodeClass.create(FloatingReadNode.class);

    @OptionalInput(InputType.Memory) MemoryNode lastLocationAccess;

    // @cons
    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp)
    {
        this(address, location, lastLocationAccess, stamp, null, BarrierType.NONE);
    }

    // @cons
    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp, GuardingNode guard)
    {
        this(address, location, lastLocationAccess, stamp, guard, BarrierType.NONE);
    }

    // @cons
    public FloatingReadNode(AddressNode address, LocationIdentity location, MemoryNode lastLocationAccess, Stamp stamp, GuardingNode guard, BarrierType barrierType)
    {
        super(TYPE, address, location, stamp, guard, barrierType);
        this.lastLocationAccess = lastLocationAccess;

        // The input to floating reads must be always non-null or have at least a guard.
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode newlla)
    {
        updateUsages(ValueNodeUtil.asNode(lastLocationAccess), ValueNodeUtil.asNode(newlla));
        lastLocationAccess = newlla;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRKind readKind = gen.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitLoad(readKind, gen.operand(address), null));
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        Node result = ReadNode.canonicalizeRead(this, getAddress(), getLocationIdentity(), tool);
        if (result != this)
        {
            return result;
        }
        if (tool.canonicalizeReads() && getAddress().hasMoreThanOneUsage() && lastLocationAccess instanceof WriteNode)
        {
            WriteNode write = (WriteNode) lastLocationAccess;
            if (write.getAddress() == getAddress() && write.getAccessStamp().isCompatible(getAccessStamp()))
            {
                // same memory location with no intervening write
                return write.value();
            }
        }
        return this;
    }

    @Override
    public FixedAccessNode asFixedNode()
    {
        ReadNode result = graph().add(new ReadNode(getAddress(), getLocationIdentity(), stamp(NodeView.DEFAULT), getBarrierType()));
        result.setGuard(getGuard());
        return result;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return stamp(NodeView.DEFAULT);
    }
}
