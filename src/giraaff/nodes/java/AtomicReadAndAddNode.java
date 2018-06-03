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

///
// Represents an atomic read-and-add operation like
// {@link sun.misc.Unsafe#getAndAddInt(Object, long, int)}.
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class AtomicReadAndAddNode
public final class AtomicReadAndAddNode extends AbstractMemoryCheckpoint implements LIRLowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<AtomicReadAndAddNode> TYPE = NodeClass.create(AtomicReadAndAddNode.class);

    @Input(InputType.Association)
    // @field
    AddressNode ___address;
    @Input
    // @field
    ValueNode ___delta;

    // @field
    protected final LocationIdentity ___locationIdentity;

    // @cons
    public AtomicReadAndAddNode(AddressNode __address, ValueNode __delta, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forKind(__delta.getStackKind()));
        this.___address = __address;
        this.___delta = __delta;
        this.___locationIdentity = __locationIdentity;
    }

    public ValueNode delta()
    {
        return this.___delta;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = __gen.getLIRGeneratorTool().emitAtomicReadAndAdd(__gen.operand(this.___address), __gen.operand(this.___delta));
        __gen.setResult(this, __result);
    }
}
