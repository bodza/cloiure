package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.AbstractStateSplit;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.StateSplit;

/**
 * Provides an implementation of {@link StateSplit}.
 */
public abstract class AbstractMemoryCheckpoint extends AbstractStateSplit implements MemoryCheckpoint
{
    public static final NodeClass<AbstractMemoryCheckpoint> TYPE = NodeClass.create(AbstractMemoryCheckpoint.class);

    protected AbstractMemoryCheckpoint(NodeClass<? extends AbstractMemoryCheckpoint> c, Stamp stamp)
    {
        this(c, stamp, null);
    }

    protected AbstractMemoryCheckpoint(NodeClass<? extends AbstractMemoryCheckpoint> c, Stamp stamp, FrameState stateAfter)
    {
        super(c, stamp, stateAfter);
    }
}
