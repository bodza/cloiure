package giraaff.nodes.memory;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractStateSplit;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;

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
