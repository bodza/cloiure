package graalvm.compiler.hotspot.nodes;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.extended.MonitorExit;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Intrinsic for closing a {@linkplain BeginLockScopeNode scope} binding a stack-based lock with an
 * object.
 */
public final class EndLockScopeNode extends AbstractMemoryCheckpoint implements LIRLowerable, MonitorExit, MemoryCheckpoint.Single
{
    public static final NodeClass<EndLockScopeNode> TYPE = NodeClass.create(EndLockScopeNode.class);

    public EndLockScopeNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public boolean hasSideEffect()
    {
        return false;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
    }

    @NodeIntrinsic
    public static native void endLockScope();
}
