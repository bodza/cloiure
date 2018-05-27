package giraaff.hotspot;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;

import giraaff.phases.tiers.CompilerConfiguration;

public interface HotSpotBackendFactory
{
    /**
     * Gets the class describing the architecture the backend created by this factory is associated with.
     */
    Class<? extends Architecture> getArchitecture();

    HotSpotBackend createBackend(HotSpotGraalRuntimeProvider runtime, CompilerConfiguration compilerConfiguration, HotSpotJVMCIRuntimeProvider jvmciRuntime);
}
