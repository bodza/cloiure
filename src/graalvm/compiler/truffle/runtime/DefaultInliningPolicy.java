package graalvm.compiler.truffle.runtime;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TruffleInliningMaxCallerSize;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TruffleMaximumRecursiveInlining;

import graalvm.compiler.truffle.common.TruffleCompilerOptions;

import com.oracle.truffle.api.CompilerOptions;

public class DefaultInliningPolicy implements TruffleInliningPolicy {

    private static final String REASON_RECURSION = "number of recursions > " + TruffleCompilerOptions.getValue(TruffleMaximumRecursiveInlining);
    private static final String REASON_MAXIMUM_NODE_COUNT = "deepNodeCount * callSites  > " + TruffleCompilerOptions.getValue(TruffleInliningMaxCallerSize);
    private static final String REASON_MAXIMUM_TOTAL_NODE_COUNT = "totalNodeCount > " + TruffleCompilerOptions.getValue(TruffleInliningMaxCallerSize);

    @Override
    public double calculateScore(TruffleInliningProfile profile) {
        return profile.getFrequency() / profile.getDeepNodeCount();
    }

    @Override
    public boolean isAllowed(TruffleInliningProfile profile, int currentNodeCount, CompilerOptions options) {
        if (profile.isCached()) {
            profile.setFailedReason(profile.getCached().getFailedReason());
            return false;
        }
        if (profile.getRecursions() > TruffleCompilerOptions.getValue(TruffleMaximumRecursiveInlining)) {
            profile.setFailedReason(REASON_RECURSION);
            return false;
        }

        int inliningMaxCallerSize = TruffleCompilerOptions.getValue(TruffleInliningMaxCallerSize);

        if (options instanceof GraalCompilerOptions) {
            inliningMaxCallerSize = Math.max(inliningMaxCallerSize, ((GraalCompilerOptions) options).getMinInliningMaxCallerSize());
        }

        if (currentNodeCount + profile.getDeepNodeCount() > inliningMaxCallerSize) {
            profile.setFailedReason(REASON_MAXIMUM_TOTAL_NODE_COUNT);
            return false;
        }

        if (profile.isForced()) {
            return true;
        }

        int cappedCallSites = Math.min(Math.max(profile.getCallSites(), 1), 10);
        if (profile.getDeepNodeCount() * cappedCallSites > inliningMaxCallerSize) {
            profile.setFailedReason(REASON_MAXIMUM_NODE_COUNT);
            return false;
        }

        return true;
    }
}
