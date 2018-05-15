package graalvm.compiler.debug;

import jdk.vm.ci.meta.JavaMethod;

/**
 * Interface for objects used in Debug {@linkplain DebugContext#context() context} that can provide
 * a {@link JavaMethod}.
 */
public interface JavaMethodContext {
    JavaMethod asJavaMethod();
}
