package giraaff.hotspot.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.extended.MonitorExit;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Intrinsic for closing a {@linkplain BeginLockScopeNode scope} binding a stack-based lock with an object.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class EndLockScopeNode
public final class EndLockScopeNode extends AbstractMemoryCheckpoint implements LIRLowerable, MonitorExit, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<EndLockScopeNode> TYPE = NodeClass.create(EndLockScopeNode.class);

    // @cons EndLockScopeNode
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
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    @Node.NodeIntrinsic
    public static native void endLockScope();
}
