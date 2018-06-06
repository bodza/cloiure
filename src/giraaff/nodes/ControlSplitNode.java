package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;

///
// The {@code ControlSplitNode} is a base class for all instructions that split the control flow
// (ie. have more than one successor).
///
// @class ControlSplitNode
public abstract class ControlSplitNode extends FixedNode implements IterableNodeType
{
    // @def
    public static final NodeClass<ControlSplitNode> TYPE = NodeClass.create(ControlSplitNode.class);

    // @cons ControlSplitNode
    protected ControlSplitNode(NodeClass<? extends ControlSplitNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    public abstract double probability(AbstractBeginNode __successor);

    ///
    // Attempts to set the probability for the given successor to the passed value (which has to be
    // in the range of 0.0 and 1.0). Returns whether setting the probability was successful.
    ///
    public abstract boolean setProbability(AbstractBeginNode __successor, double __value);

    ///
    // Primary successor of the control split. Data dependencies on the node have to be scheduled in
    // the primary successor. Returns null if data dependencies are not expected.
    //
    // @return the primary successor
    ///
    public abstract AbstractBeginNode getPrimarySuccessor();

    ///
    // Returns the number of successors.
    ///
    public abstract int getSuccessorCount();
}
