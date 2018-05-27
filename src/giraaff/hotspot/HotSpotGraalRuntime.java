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
    private final GraalHotSpotVMConfig config;
    private final OptionValues options;
    private final HotSpotBackend backend;

    public HotSpotGraalRuntime(HotSpotJVMCIRuntime jvmciRuntime, OptionValues options)
    {
        this.config = new GraalHotSpotVMConfig(jvmciRuntime.getConfigStore());
        this.options = options;
        this.backend = new AMD64HotSpotBackendFactory().createBackend(this, new CommunityCompilerConfiguration(), jvmciRuntime);
        this.backend.completeInitialization(jvmciRuntime, options);
    }

    @Override
    public GraalHotSpotVMConfig getVMConfig()
    {
        return config;
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
