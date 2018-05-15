package graalvm.compiler.truffle.common;

import graalvm.compiler.debug.GraalError;

/**
 * Initializes and stores the singleton {@link TruffleCompilerRuntime} instance.
 */
final class TruffleCompilerRuntimeInstance {
    /**
     * The singleton instance.
     */
    static final TruffleCompilerRuntime INSTANCE = init();

    private static TruffleCompilerRuntime init() {
        Object truffleRuntime = TruffleRuntimeInstance.INSTANCE;
        if (truffleRuntime instanceof TruffleCompilerRuntime) {
            return (TruffleCompilerRuntime) truffleRuntime;
        }
        throw new GraalError("Truffle runtime %s (loader: %s) is not a %s (loader: %s)", truffleRuntime, truffleRuntime.getClass().getClassLoader(),
                        TruffleCompilerRuntime.class.getName(), TruffleCompilerRuntime.class.getClassLoader());
    }
}
