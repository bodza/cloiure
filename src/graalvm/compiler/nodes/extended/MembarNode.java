package graalvm.compiler.nodes.extended;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Creates a memory barrier.
 */
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
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.getLIRGeneratorTool().emitMembar(barriers);
    }

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers);

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers, @ConstantNodeParameter LocationIdentity location);
}
