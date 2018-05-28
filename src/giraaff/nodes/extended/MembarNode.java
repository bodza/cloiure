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
public final class MembarNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<MembarNode> TYPE = NodeClass.create(MembarNode.class);

    protected final int barriers;
    protected final LocationIdentity location;

    public MembarNode(int barriers)
    {
        this(barriers, LocationIdentity.any());
    }

    public MembarNode(int barriers, LocationIdentity location)
    {
        super(TYPE, StampFactory.forVoid());
        this.barriers = barriers;
        this.location = location;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return location;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitMembar(barriers);
    }

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers);

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers, @ConstantNodeParameter LocationIdentity location);
}
