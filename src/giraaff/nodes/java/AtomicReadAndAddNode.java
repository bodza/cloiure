package giraaff.nodes.java;

import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Represents an atomic read-and-add operation like
 * {@link sun.misc.Unsafe#getAndAddInt(Object, long, int)}.
 */
// @NodeInfo.allowedUsageTypes "Memory"
public final class AtomicReadAndAddNode extends AbstractMemoryCheckpoint implements LIRLowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<AtomicReadAndAddNode> TYPE = NodeClass.create(AtomicReadAndAddNode.class);
    @Input(InputType.Association) AddressNode address;
    @Input ValueNode delta;

    protected final LocationIdentity locationIdentity;

    public AtomicReadAndAddNode(AddressNode address, ValueNode delta, LocationIdentity locationIdentity)
    {
        super(TYPE, StampFactory.forKind(delta.getStackKind()));
        this.address = address;
        this.delta = delta;
        this.locationIdentity = locationIdentity;
    }

    public ValueNode delta()
    {
        return delta;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result = gen.getLIRGeneratorTool().emitAtomicReadAndAdd(gen.operand(address), gen.operand(delta));
        gen.setResult(this, result);
    }
}
