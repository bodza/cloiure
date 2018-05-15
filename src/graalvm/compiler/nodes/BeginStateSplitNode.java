package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;

/**
 * Base class for {@link AbstractBeginNode}s that are associated with a frame state.
 *
 * TODO (dnsimon) this not needed until {@link AbstractBeginNode} no longer implements
 * {@link StateSplit} which is not possible until loop peeling works without requiring begin nodes
 * to have frames states.
 */
@NodeInfo
public abstract class BeginStateSplitNode extends AbstractBeginNode implements StateSplit {

    public static final NodeClass<BeginStateSplitNode> TYPE = NodeClass.create(BeginStateSplitNode.class);
    @OptionalInput(InputType.State) protected FrameState stateAfter;

    protected BeginStateSplitNode(NodeClass<? extends BeginStateSplitNode> c) {
        super(c);
    }

    protected BeginStateSplitNode(NodeClass<? extends BeginStateSplitNode> c, Stamp stamp) {
        super(c, stamp);
    }

    @Override
    public FrameState stateAfter() {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x) {
        assert x == null || x.isAlive() : "frame state must be in a graph";
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    /**
     * A begin node has no side effect.
     */
    @Override
    public boolean hasSideEffect() {
        return false;
    }
}