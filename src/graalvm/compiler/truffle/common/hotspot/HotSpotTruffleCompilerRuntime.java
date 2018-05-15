package graalvm.compiler.truffle.common.hotspot;

import graalvm.compiler.truffle.common.TruffleCompilerRuntime;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public interface HotSpotTruffleCompilerRuntime extends TruffleCompilerRuntime {
    /**
     * Gets all methods denoted as a Truffle call boundary (such as being annotated by
     * {@code TruffleCallBoundary}).
     */
    Iterable<ResolvedJavaMethod> getTruffleCallBoundaryMethods();

    /**
     * Notifies this runtime once {@code installedCode} has been installed in the code cache.
     *
     * @param installedCode code that has just been installed in the code cache
     */
    void onCodeInstallation(HotSpotTruffleInstalledCode installedCode);
}
