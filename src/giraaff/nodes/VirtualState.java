package giraaff.nodes;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;

/**
 * Base class for nodes that contain "virtual" state, like FrameState and VirtualObjectState.
 * Subclasses of this class will be treated in a special way by the scheduler.
 */
// @NodeInfo.allowedUsageTypes "State"
// @class VirtualState
public abstract class VirtualState extends Node
{
    // @cons
    protected VirtualState(NodeClass<? extends VirtualState> __c)
    {
        super(__c);
    }

    // @def
    public static final NodeClass<VirtualState> TYPE = NodeClass.create(VirtualState.class);

    // @class VirtualState.NodeClosure
    public abstract static class NodeClosure<T extends Node>
    {
        public abstract void apply(Node usage, T node);
    }

    // @iface VirtualState.VirtualClosure
    public interface VirtualClosure
    {
        void apply(VirtualState node);
    }

    public abstract VirtualState duplicateWithVirtualState();

    public abstract void applyToNonVirtual(NodeClosure<? super ValueNode> closure);

    /**
     * Performs a <b>pre-order</b> iteration over all elements reachable from this state that
     * are a subclass of {@link VirtualState}.
     */
    public abstract void applyToVirtual(VirtualClosure closure);

    public abstract boolean isPartOfThisState(VirtualState state);

    @Override
    public final StructuredGraph graph()
    {
        return (StructuredGraph) super.graph();
    }
}
