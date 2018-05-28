package giraaff.hotspot;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;

import giraaff.core.phases.CommunityCompilerConfiguration;
import giraaff.hotspot.amd64.AMD64HotSpotBackendFactory;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.options.OptionValues;

/**
 * Singleton class holding the instance of the {@link GraalRuntime}.
 */
public final class HotSpotGraalRuntime implements HotSpotGraalRuntimeProvider
{
    private final OptionValues options;
    private final HotSpotBackend backend;

    public HotSpotGraalRuntime(OptionValues options)
    {
        this.options = options;
        this.backend = new AMD64HotSpotBackendFactory().createBackend(this, new CommunityCompilerConfiguration(), HotSpotJVMCIRuntime.runtime());
        this.backend.completeInitialization(options);
    }

    @Override
    public OptionValues getOptions()
    {
        return options;
    }

    @Override
    public HotSpotBackend getBackend()
    {
        return backend;
    }

    private boolean shutdown;

    void shutdown()
    {
        shutdown = true;
    }

    @Override
    public boolean isShutdown()
    {
        return shutdown;
    }
}
