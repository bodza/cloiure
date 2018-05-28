package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

/**
 * Provides an implementation of {@link StateSplit}.
 */
public abstract class AbstractStateSplit extends FixedWithNextNode implements StateSplit
{
    public static final NodeClass<AbstractStateSplit> TYPE = NodeClass.create(AbstractStateSplit.class);

    @OptionalInput(InputType.State) protected FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> c, Stamp stamp)
    {
        this(c, stamp, null);
    }

    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> c, Stamp stamp, FrameState stateAfter)
    {
        super(c, stamp);
        this.stateAfter = stateAfter;
    }
}
