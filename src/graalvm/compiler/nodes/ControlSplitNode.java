package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;

/**
 * The {@code ControlSplitNode} is a base class for all instructions that split the control flow
 * (ie. have more than one successor).
 */
public abstract class ControlSplitNode extends FixedNode implements IterableNodeType
{
    public static final NodeClass<ControlSplitNode> TYPE = NodeClass.create(ControlSplitNode.class);

    protected ControlSplitNode(NodeClass<? extends ControlSplitNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    public abstract double probability(AbstractBeginNode successor);

    /**
     * Attempts to set the probability for the given successor to the passed value (which has to be
     * in the range of 0.0 and 1.0). Returns whether setting the probability was successful.
     */
    public abstract boolean setProbability(AbstractBeginNode successor, double value);

    /**
     * Primary successor of the control split. Data dependencies on the node have to be scheduled in
     * the primary successor. Returns null if data dependencies are not expected.
     *
     * @return the primary successor
     */
    public abstract AbstractBeginNode getPrimarySuccessor();

    /**
     * Returns the number of successors.
     */
    public abstract int getSuccessorCount();
}
