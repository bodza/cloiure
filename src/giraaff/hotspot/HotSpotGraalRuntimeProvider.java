package giraaff.hotspot;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.runtime.GraalRuntime;
import giraaff.options.OptionValues;

/**
 * Configuration information for the HotSpot Graal runtime.
 */
public interface HotSpotGraalRuntimeProvider extends GraalRuntime
{
    default TargetDescription getTarget()
    {
        return getBackend().getTarget();
    }

    HotSpotBackend getBackend();

    GraalHotSpotVMConfig getVMConfig();

    /**
     * Gets the option values associated with this runtime.
     */
    OptionValues getOptions();

    /**
     * This runtime has been requested to shutdown.
     */
    boolean isShutdown();
}
