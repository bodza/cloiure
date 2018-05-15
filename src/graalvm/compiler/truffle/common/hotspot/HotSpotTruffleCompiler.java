package graalvm.compiler.truffle.common.hotspot;

import graalvm.compiler.truffle.common.TruffleCompiler;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

public interface HotSpotTruffleCompiler extends TruffleCompiler {

    /**
     * Compiles and installs special code for
     * {@link HotSpotTruffleCompilerRuntime#getTruffleCallBoundaryMethods()}.
     */
    void installTruffleCallBoundaryMethods();

    abstract class Factory {
        public abstract HotSpotTruffleCompiler create(TruffleCompilerRuntime runtime);
    }

    HotSpotTruffleInstalledCode INVALID_CODE = new HotSpotTruffleInstalledCode(null);
}
