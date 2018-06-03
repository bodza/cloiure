package giraaff.hotspot;

import giraaff.phases.tiers.CompilerConfiguration;

// @iface HotSpotBackendFactory
public interface HotSpotBackendFactory
{
    HotSpotBackend createBackend(HotSpotGraalRuntime __runtime, CompilerConfiguration __compilerConfiguration);
}
