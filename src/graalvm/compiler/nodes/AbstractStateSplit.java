package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;

/**
 * Provides an implementation of {@link StateSplit}.
 */
@NodeInfo
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
        assert x == null || x.isAlive() : "frame state must be in a graph";
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
