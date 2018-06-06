package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

///
// Provides an implementation of {@link StateSplit}.
///
// @class AbstractStateSplit
public abstract class AbstractStateSplit extends FixedWithNextNode implements StateSplit
{
    // @def
    public static final NodeClass<AbstractStateSplit> TYPE = NodeClass.create(AbstractStateSplit.class);

    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(this.___stateAfter, __x);
        this.___stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    // @cons AbstractStateSplit
    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> __c, Stamp __stamp)
    {
        this(__c, __stamp, null);
    }

    // @cons AbstractStateSplit
    protected AbstractStateSplit(NodeClass<? extends AbstractStateSplit> __c, Stamp __stamp, FrameState __stateAfter)
    {
        super(__c, __stamp);
        this.___stateAfter = __stateAfter;
    }
}
