package graalvm.compiler.nodes.extended;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

/**
 * Creates a memory barrier.
 */
@NodeInfo(nameTemplate = "Membar#{p#location/s}", allowedUsageTypes = Memory, cycles = CYCLES_2, size = SIZE_2)
public final class MembarNode extends FixedWithNextNode implements LIRLowerable, MemoryCheckpoint.Single {

    public static final NodeClass<MembarNode> TYPE = NodeClass.create(MembarNode.class);
    protected final int barriers;
    protected final LocationIdentity location;

    public MembarNode(int barriers) {
        this(barriers, LocationIdentity.any());
    }

    public MembarNode(int barriers, LocationIdentity location) {
        super(TYPE, StampFactory.forVoid());
        this.barriers = barriers;
        this.location = location;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return location;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        generator.getLIRGeneratorTool().emitMembar(barriers);
    }

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers);

    @NodeIntrinsic
    public static native void memoryBarrier(@ConstantNodeParameter int barriers, @ConstantNodeParameter LocationIdentity location);
}
