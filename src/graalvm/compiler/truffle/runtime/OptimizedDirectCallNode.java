package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerOptions;
import com.oracle.truffle.api.impl.DefaultCompilerOptions;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TruffleExperimentalSplitting;

/**
 * A call node with a constant {@link CallTarget} that can be optimized by Graal.
 *
 * Note: {@code PartialEvaluator} looks up this class and a number of its methods by name.
 */
@NodeInfo
public final class OptimizedDirectCallNode extends DirectCallNode {

    private int callCount;
    private boolean inliningForced;
    @CompilationFinal private ValueProfile exceptionProfile;

    @CompilationFinal private OptimizedCallTarget splitCallTarget;

    private final GraalTruffleRuntime runtime;

    public OptimizedDirectCallNode(GraalTruffleRuntime runtime, OptimizedCallTarget target) {
        super(target);
        assert target.getSourceCallTarget() == null;
        this.runtime = runtime;
    }

    @Override
    public Object call(Object[] arguments) {
        if (CompilerDirectives.inInterpreter()) {
            onInterpreterCall();
        }
        try {
            return callProxy(this, getCurrentCallTarget(), arguments, true);
        } catch (Throwable t) {
            if (exceptionProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                exceptionProfile = ValueProfile.createClassProfile();
            }
            Throwable profiledT = exceptionProfile.profile(t);
            OptimizedCallTarget.runtime().getTvmci().onThrowable(this, null, profiledT, null);
            throw OptimizedCallTarget.rethrow(profiledT);
        }
    }

    // Note: {@code PartialEvaluator} looks up this method by name and signature.
    public static Object callProxy(Node callNode, CallTarget callTarget, Object[] arguments, boolean direct) {
        try {
            if (direct) {
                return ((OptimizedCallTarget) callTarget).callDirect(arguments);
            } else {
                return callTarget.call(arguments);
            }
        } finally {
            // this assertion is needed to keep the values from being cleared as non-live locals
            assert callNode != null & callTarget != null;
        }
    }

    @Override
    public boolean isInlinable() {
        return true;
    }

    @Override
    public void forceInlining() {
        inliningForced = true;
    }

    @Override
    public boolean isInliningForced() {
        return inliningForced;
    }

    @Override
    public boolean isCallTargetCloningAllowed() {
        return getCallTarget().getRootNode().isCloningAllowed();
    }

    @Override
    public OptimizedCallTarget getCallTarget() {
        return (OptimizedCallTarget) super.getCallTarget();
    }

    public int getCallCount() {
        return callCount;
    }

    public CompilerOptions getCompilerOptions() {
        RootNode rootNode = getRootNode();
        return rootNode != null ? rootNode.getCompilerOptions() : DefaultCompilerOptions.INSTANCE;
    }

    @Override
    public OptimizedCallTarget getCurrentCallTarget() {
        return (OptimizedCallTarget) super.getCurrentCallTarget();
    }

    public int getKnownCallSiteCount() {
        return getCurrentCallTarget().getKnownCallSiteCount();
    }

    @Override
    public OptimizedCallTarget getClonedCallTarget() {
        return splitCallTarget;
    }

    private void onInterpreterCall() {
        int calls = ++callCount;
        if (calls == 1) {
            getCurrentCallTarget().incrementKnownCallSites();
        }
        TruffleSplittingStrategy.beforeCall(this, runtime.getTvmci());
    }

    /** Used by the splitting strategy to install new targets. */
    void split() {
        CompilerAsserts.neverPartOfCompilation();

        // Synchronize with atomic() as replace() also takes the same lock
        // and we only want to take one lock to avoid deadlocks.
        atomic(() -> {
            if (splitCallTarget != null) {
                return;
            }

            assert isCallTargetCloningAllowed();
            OptimizedCallTarget currentTarget = getCallTarget();

            OptimizedCallTarget splitTarget = getCallTarget().cloneUninitialized();
            splitTarget.setCallSiteForSplit(this);

            if (callCount >= 1) {
                currentTarget.decrementKnownCallSites();
                if (TruffleCompilerOptions.getValue(TruffleExperimentalSplitting)) {
                    currentTarget.removeKnownCallSite(this);
                }
            }
            splitTarget.incrementKnownCallSites();
            if (TruffleCompilerOptions.getValue(TruffleExperimentalSplitting)) {
                splitTarget.addKnownCallNode(this);
            }

            if (getParent() != null) {
                // dummy replace to report the split, irrelevant if this node is not adopted
                replace(this, "Split call node");
            }
            splitCallTarget = splitTarget;
            runtime.getListener().onCompilationSplit(this);
        });
    }

    @Override
    public boolean cloneCallTarget() {
        TruffleSplittingStrategy.forceSplitting(this, runtime.getTvmci());
        return true;
    }
}
