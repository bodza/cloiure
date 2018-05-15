package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.CompilerOptions;

public interface TruffleInliningPolicy {

    boolean isAllowed(TruffleInliningProfile profile, int currentNodeCount, CompilerOptions options);

    double calculateScore(TruffleInliningProfile profile);

}
