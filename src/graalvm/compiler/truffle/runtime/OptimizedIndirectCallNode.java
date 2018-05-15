package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ValueProfile;

/**
 * A call node with a constant {@link CallTarget} that can be optimized by Graal.
 */
@NodeInfo
public final class OptimizedIndirectCallNode extends IndirectCallNode {

    @CompilationFinal private ValueProfile exceptionProfile;

    @Override
    public Object call(CallTarget target, Object[] arguments) {

        try {
            return OptimizedDirectCallNode.callProxy(this, target, arguments, false);
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
}
