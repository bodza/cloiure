package giraaff.hotspot;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.runtime.GraalRuntime;
import giraaff.core.phases.CommunityCompilerConfiguration;
import giraaff.hotspot.amd64.AMD64HotSpotBackendFactory;
import giraaff.options.OptionValues;

/**
 * Singleton class holding the instance of the {@link GraalRuntime}.
 */
// @class HotSpotGraalRuntime
public final class HotSpotGraalRuntime implements GraalRuntime
{
    private final OptionValues options;
    private final HotSpotBackend backend;

    // @cons
    public HotSpotGraalRuntime(OptionValues options)
    {
        super();
        this.options = options;
        this.backend = new AMD64HotSpotBackendFactory().createBackend(this, new CommunityCompilerConfiguration());
        this.backend.completeInitialization(options);
    }

    public final OptionValues getOptions()
    {
        return options;
    }

    public final HotSpotBackend getBackend()
    {
        return backend;
    }

    public final TargetDescription getTarget()
    {
        return getBackend().getTarget();
    }
}
