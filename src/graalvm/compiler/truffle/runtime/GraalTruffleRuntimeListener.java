package graalvm.compiler.truffle.runtime;

import java.util.Map;

import graalvm.compiler.truffle.common.TruffleCompilerListener.CompilationResultInfo;
import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;

/**
 * A listener for events related to the execution and compilation phases of a
 * {@link OptimizedCallTarget}. The states for a {@link OptimizedCallTarget} instance can be
 * described using the following deterministic automata: * <code>
 * <pre>
 * ( (split | (queue . unqueue))*
 *    . queue . started
 *    . (truffleTierFinished . graalTierFinished . success)
 *      | ([truffleTierFinished] . [graalTierFinished] . failed)
 *    . invalidate )*
 * </pre>
 * </code>
 * <p>
 * Note: <code>|</code> is the 'or' and <code>.</code> is the sequential operator. The
 * <code>*</code> represents the Kleene Closure.
 * </p>
 */
public interface GraalTruffleRuntimeListener {

    /**
     * Notifies this object when the target of a Truffle call node is
     * {@linkplain DirectCallNode#cloneCallTarget() cloned}.
     *
     * @param callNode the call node whose {@linkplain OptimizedDirectCallNode#getCallTarget()
     *            target} has just been cloned
     */
    default void onCompilationSplit(OptimizedDirectCallNode callNode) {
    }

    /**
     * Notifies this object after {@code target} is added to the compilation queue.
     *
     * @param target the call target that has just been enqueued for compilation
     */
    default void onCompilationQueued(OptimizedCallTarget target) {
    }

    /**
     * Notifies this object after {@code target} is removed from the compilation queue.
     *
     * @param target the call target that has just been removed from the compilation queue
     * @param source the source object that caused the compilation to be unqueued. For example the
     *            source {@link Node} object. May be {@code null}.
     * @param reason a textual description of the reason why the compilation was unqueued. May be
     *            {@code null}.
     */
    default void onCompilationDequeued(OptimizedCallTarget target, Object source, CharSequence reason) {
    }

    /**
     * Notifies this object when compilation of {@code target} is about to start.
     *
     * @param target the call target about to be compiled
     */
    default void onCompilationStarted(OptimizedCallTarget target) {
    }

    /**
     * Notifies this object when compilation of {@code target} has completed partial evaluation and
     * is about to perform compilation of the graph produced by partial evaluation.
     *
     * @param target the call target being compiled
     * @param inliningDecision the inlining plan used during partial evaluation
     * @param graph access to compiler graph info
     */
    default void onCompilationTruffleTierFinished(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph) {
    }

    /**
     * Notifies this object when Graal compilation of a call target completes. Graal compilation
     * occurs between {@link #onCompilationTruffleTierFinished} and code installation.
     *
     * @param target the call target that was compiled
     * @param graph the graph representing {@code target}
     */
    default void onCompilationGraalTierFinished(OptimizedCallTarget target, GraphInfo graph) {
    }

    /**
     * Notifies this object when compilation of {@code target} succeeds.
     *
     * @param target the call target whose compilation succeeded
     * @param inliningDecision the inlining plan used during the compilation
     * @param graph access to compiler graph info
     * @param result access to compilation result info
     */
    default void onCompilationSuccess(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph, CompilationResultInfo result) {
    }

    /**
     * Notifies this object when compilation of {@code target} fails.
     *
     * @param target the call target whose compilation failed
     * @param reason a description of the failure
     * @param bailout specifies whether the failure was a bailout or an error in the compiler. A
     *            bailout means the compiler aborted the compilation based on some of property of
     *            {@code target} (e.g., too big). A non-bailout means an unexpected error in the
     *            compiler itself.
     * @param permanentBailout specifies if a bailout is due to a condition that probably won't
     *            change if the {@code target} is compiled again. This value is meaningless if
     *            {@code bailout == false}.
     */
    default void onCompilationFailed(OptimizedCallTarget target, String reason, boolean bailout, boolean permanentBailout) {
    }

    /**
     * Notifies this object when {@code target} is invalidated.
     *
     * @param target the call target whose compiled code was just invalidated
     * @param source the source object that caused the compilation to be invalidated. For example
     *            the source {@link Node} object. May be {@code null}.
     * @param reason a textual description of the reason why the compilation was invalidated. May be
     *            {@code null}.
     */
    default void onCompilationInvalidated(OptimizedCallTarget target, Object source, CharSequence reason) {
    }

    /**
     * Notifies this object when {@code target} has just deoptimized and is now executing in the
     * Truffle interpreter instead of executing compiled code.
     *
     * @param target the call target whose compiled code was just deoptimized
     * @param frame
     */
    default void onCompilationDeoptimized(OptimizedCallTarget target, Frame frame) {
    }

    /**
     * Notifies this object the {@link GraalTruffleRuntime} is being shut down.
     */
    default void onShutdown() {
    }

    static void addASTSizeProperty(OptimizedCallTarget target, TruffleInlining inliningDecision, Map<String, Object> properties) {
        int nodeCount = target.getNonTrivialNodeCount();
        int deepNodeCount = nodeCount;
        if (inliningDecision != null) {
            deepNodeCount += inliningDecision.getInlinedNodeCount();
        }
        properties.put("ASTSize", String.format("%5d/%5d", nodeCount, deepNodeCount));
    }
}
