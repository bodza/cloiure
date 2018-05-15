package graalvm.compiler.truffle.common;

import java.lang.reflect.Method;

/**
 * Initializes and stores the singleton {@code TruffleRuntime} instance. Separating this from
 * {@link TruffleCompilerRuntimeInstance} is necessary to support
 * {@link TruffleCompilerRuntime#getRuntimeIfAvailable()}.
 */
final class TruffleRuntimeInstance {
    /**
     * The singleton instance.
     */
    static final Object INSTANCE = init();

    /**
     * Accesses the Truffle runtime via reflection to avoid a dependency on Truffle that will expose
     * Truffle types to all classes depending on {@code graalvm.compiler.truffle.common}.
     */
    private static Object init() {
        try {
            Class<?> truffleClass = Class.forName("com.oracle.truffle.api.Truffle");
            Method getRuntime = truffleClass.getMethod("getRuntime");
            Object truffleRuntime = getRuntime.invoke(null);
            return truffleRuntime;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
