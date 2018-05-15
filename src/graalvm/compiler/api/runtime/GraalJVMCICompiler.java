package graalvm.compiler.api.runtime;

import jdk.vm.ci.runtime.JVMCICompiler;

/**
 * Graal specific extension of the {@link JVMCICompiler} interface.
 */
public interface GraalJVMCICompiler extends JVMCICompiler {

    GraalRuntime getGraalRuntime();
}
