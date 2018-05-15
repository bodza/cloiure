package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.extended.MonitorExit;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

/**
 * Intrinsic for closing a {@linkplain BeginLockScopeNode scope} binding a stack-based lock with an
 * object.
 */
@NodeInfo(allowedUsageTypes = Memory, cycles = CYCLES_0, size = SIZE_0)
public final class EndLockScopeNode extends AbstractMemoryCheckpoint implements LIRLowerable, MonitorExit, MemoryCheckpoint.Single {
    public static final NodeClass<EndLockScopeNode> TYPE = NodeClass.create(EndLockScopeNode.class);

    public EndLockScopeNode() {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public boolean hasSideEffect() {
        return false;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return LocationIdentity.any();
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
    }

    @NodeIntrinsic
    public static native void endLockScope();
}
