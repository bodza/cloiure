package graalvm.compiler.nodes;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;

/**
 * Base class for nodes that contain "virtual" state, like FrameState and VirtualObjectState.
 * Subclasses of this class will be treated in a special way by the scheduler.
 */
public abstract class VirtualState extends Node
{
    protected VirtualState(NodeClass<? extends VirtualState> c)
    {
        super(c);
    }

    public static final NodeClass<VirtualState> TYPE = NodeClass.create(VirtualState.class);

    public abstract static class NodeClosure<T extends Node>
    {
        public abstract void apply(Node usage, T node);
    }

    public interface VirtualClosure
    {
        void apply(VirtualState node);
    }

    public abstract VirtualState duplicateWithVirtualState();

    public abstract void applyToNonVirtual(NodeClosure<? super ValueNode> closure);

    /**
     * Performs a <b>pre-order</b> iteration over all elements reachable from this state that are a
     * subclass of {@link VirtualState}.
     */
    public abstract void applyToVirtual(VirtualClosure closure);

    public abstract boolean isPartOfThisState(VirtualState state);

    @Override
    public final StructuredGraph graph()
    {
        return (StructuredGraph) super.graph();
    }
}
