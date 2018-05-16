package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.InputType.Association;
import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.Value;

/**
 * Represents an atomic read-and-add operation like
 * {@link sun.misc.Unsafe#getAndAddInt(Object, long, int)}.
 */
@NodeInfo(allowedUsageTypes = Memory, cycles = CYCLES_8, size = SIZE_2)
public final class AtomicReadAndAddNode extends AbstractMemoryCheckpoint implements LIRLowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<AtomicReadAndAddNode> TYPE = NodeClass.create(AtomicReadAndAddNode.class);
    @Input(Association) AddressNode address;
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
