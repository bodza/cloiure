package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

///
// Base class for {@link AbstractBeginNode}s that are associated with a frame state.
//
// TODO this not needed until {@link AbstractBeginNode} no longer implements {@link StateSplit}
// which is not possible until loop peeling works without requiring begin nodes to have frames states.
///
// @class BeginStateSplitNode
public abstract class BeginStateSplitNode extends AbstractBeginNode implements StateSplit
{
    // @def
    public static final NodeClass<BeginStateSplitNode> TYPE = NodeClass.create(BeginStateSplitNode.class);

    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateAfter;

    // @cons BeginStateSplitNode
    protected BeginStateSplitNode(NodeClass<? extends BeginStateSplitNode> __c)
    {
        super(__c);
    }

    // @cons BeginStateSplitNode
    protected BeginStateSplitNode(NodeClass<? extends BeginStateSplitNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

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

    ///
    // A begin node has no side effect.
    ///
    @Override
    public boolean hasSideEffect()
    {
        return false;
    }
}
