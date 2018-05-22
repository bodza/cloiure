package giraaff.hotspot.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.MonitorExit;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

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
