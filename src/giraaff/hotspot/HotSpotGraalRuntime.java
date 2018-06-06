package giraaff.hotspot;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.runtime.GraalRuntime;
import giraaff.core.phases.CommunityCompilerConfiguration;
import giraaff.hotspot.amd64.AMD64HotSpotBackendFactory;

///
// Singleton class holding the instance of the {@link GraalRuntime}.
///
// @class HotSpotGraalRuntime
public final class HotSpotGraalRuntime implements GraalRuntime
{
    // @field
    private final HotSpotBackend ___backend;

    // @cons HotSpotGraalRuntime
    public HotSpotGraalRuntime()
    {
        super();
        this.___backend = new AMD64HotSpotBackendFactory().createBackend(this, new CommunityCompilerConfiguration());
        this.___backend.completeInitialization();
    }

    public final HotSpotBackend getBackend()
    {
        return this.___backend;
    }

    public final TargetDescription getTarget()
    {
        return getBackend().getTarget();
    }
}
