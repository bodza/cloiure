package graalvm.compiler.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;

/**
 * A marker interface for nodes that represent calls to other methods.
 */
public interface Invokable
{
    ResolvedJavaMethod getTargetMethod();

    int bci();

    default boolean isAlive()
    {
        return asFixedNode().isAlive();
    }

    FixedNode asFixedNode();

    /**
     * Called on a {@link Invokable} node after it is registered with a graph.
     *
     * To override the default functionality, code that creates an {@link Invokable} should set the
     * updating logic by calling {@link InliningLog#openUpdateScope}.
     */
    default void updateInliningLogAfterRegister(StructuredGraph newGraph)
    {
        InliningLog log = newGraph.getInliningLog();
        if (log.getUpdateScope() != null)
        {
            log.getUpdateScope().accept(null, this);
        }
        else
        {
            assert !log.containsLeafCallsite(this);
            log.trackNewCallsite(this);
        }
    }

    /**
     * Called on a {@link Invokable} node after it was cloned from another node.
     *
     * This call is always preceded with a call to {@link Invokable#updateInliningLogAfterRegister}.
     *
     * To override the default functionality, code that creates an {@link Invokable} should set the
     * updating logic by calling {@link InliningLog#openUpdateScope}.
     */
    default void updateInliningLogAfterClone(Node other)
    {
        if (GraalOptions.TraceInlining.getValue(asFixedNode().getOptions()))
        {
            // At this point, the invokable node was already added to the inlining log
            // in the call to updateInliningLogAfterRegister, so we need to remove it.
            InliningLog log = asFixedNode().graph().getInliningLog();
            assert other instanceof Invokable;
            if (log.getUpdateScope() != null)
            {
                // InliningLog.UpdateScope determines how to update the log.
                log.getUpdateScope().accept((Invokable) other, this);
            }
            else if (other.graph() == this.asFixedNode().graph())
            {
                // This node was cloned as part of duplication.
                // We need to add it as a sibling of the node other.
                assert log.containsLeafCallsite(this) : "Node " + this + " not contained in the log.";
                assert log.containsLeafCallsite((Invokable) other) : "Sibling " + other + " not contained in the log.";
                log.removeLeafCallsite(this);
                log.trackDuplicatedCallsite((Invokable) other, this);
            }
            else
            {
                // This node was added from a different graph.
                // The adder is responsible for providing a context.
                throw GraalError.shouldNotReachHere("No InliningLog.Update scope provided.");
            }
        }
    }
}
