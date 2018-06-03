package giraaff.nodes;

import giraaff.nodes.spi.NodeWithState;

///
// Interface implemented by nodes which may need {@linkplain FrameState deoptimization information}.
///
// @iface DeoptimizingNode
public interface DeoptimizingNode extends NodeWithState
{
    ///
    // Determines if this node needs deoptimization information.
    ///
    boolean canDeoptimize();

    ///
    // Interface for nodes that need a {@link FrameState} for deoptimizing to a point before their execution.
    ///
    // @iface DeoptimizingNode.DeoptBefore
    public interface DeoptBefore extends DeoptimizingNode
    {
        ///
        // Sets the {@link FrameState} describing the program state before the execution of this node.
        ///
        void setStateBefore(FrameState __state);

        FrameState stateBefore();
    }

    ///
    // Interface for nodes that need a {@link FrameState} for deoptimizing to a point after their execution.
    ///
    // @iface DeoptimizingNode.DeoptAfter
    public interface DeoptAfter extends DeoptimizingNode, StateSplit
    {
    }

    ///
    // Interface for nodes that need a special {@link FrameState} for deoptimizing during their
    // execution (e.g. {@link Invoke}).
    ///
    // @iface DeoptimizingNode.DeoptDuring
    public interface DeoptDuring extends DeoptimizingNode, StateSplit
    {
        FrameState stateDuring();

        ///
        // Sets the {@link FrameState} describing the program state during the execution of this node.
        ///
        void setStateDuring(FrameState __state);

        ///
        // Compute the {@link FrameState} describing the program state during the execution of this
        // node from an input {@link FrameState} describing the program state after finishing the
        // execution of this node.
        ///
        void computeStateDuring(FrameState __stateAfter);
    }
}
