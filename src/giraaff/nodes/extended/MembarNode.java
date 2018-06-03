package giraaff.nodes.extended;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Creates a memory barrier.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class MembarNode
public final class MembarNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<MembarNode> TYPE = NodeClass.create(MembarNode.class);

    // @field
    protected final int barriers;
    // @field
    protected final LocationIdentity location;

    // @cons
    public MembarNode(int __barriers)
    {
        this(__barriers, LocationIdentity.any());
    }

    // @cons
    public MembarNode(int __barriers, LocationIdentity __location)
    {
        super(TYPE, StampFactory.forVoid());
        this.barriers = __barriers;
        this.location = __location;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return location;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitMembar(barriers);
    }

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers);

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers, @ConstantNodeParameter LocationIdentity location);
}
