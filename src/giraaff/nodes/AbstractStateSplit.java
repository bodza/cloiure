package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

/**
 * Provides an implementation of {@link StateSplit}.
 */
// @class AbstractStateSplit
public abstract class AbstractStateSplit extends FixedWithNextNode implements StateSplit
{
    // @def
    public static final NodeClass<AbstractStateSplit> TYPE = NodeClass.create(AbstractStateSplit.class);

    @OptionalInput(InputType.State)
    // @field
    protected FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(stateAfter, __x);
        stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    // @cons
    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> __c, Stamp __stamp)
    {
        this(__c, __stamp, null);
    }

    // @cons
    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> __c, Stamp __stamp, FrameState __stateAfter)
    {
        super(__c, __stamp);
        this.stateAfter = __stateAfter;
    }
}
