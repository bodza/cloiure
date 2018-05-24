package giraaff.hotspot;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.runtime.GraalRuntime;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.options.OptionValues;
import giraaff.runtime.RuntimeProvider;

/**
 * Configuration information for the HotSpot Graal runtime.
 */
public interface HotSpotGraalRuntimeProvider extends GraalRuntime, RuntimeProvider
{
    default TargetDescription getTarget()
    {
        return getHostBackend().getTarget();
    }

    HotSpotProviders getHostProviders();

    @Override
    default String getName()
    {
        return getClass().getSimpleName();
    }

    @Override
    HotSpotBackend getHostBackend();

    GraalHotSpotVMConfig getVMConfig();

    /**
     * Gets the option values associated with this runtime.
     */
    OptionValues getOptions();

    /**
     * This runtime has been requested to shutdown.
     */
    boolean isShutdown();

    /**
     * Returns the unique compiler configuration name that is in use. Useful for users to find out
     * which configuration is in use.
     */
    String getCompilerConfigurationName();
}
