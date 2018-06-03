package giraaff.api.runtime;

import jdk.vm.ci.runtime.JVMCICompiler;

///
// Graal specific extension of the {@link JVMCICompiler} interface.
///
// @iface GraalJVMCICompiler
public interface GraalJVMCICompiler extends JVMCICompiler
{
    GraalRuntime getGraalRuntime();
}
