package graalvm.compiler.hotspot;

import java.util.Map;

import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetCounter.Group;
import graalvm.compiler.runtime.RuntimeProvider;

import jdk.vm.ci.code.TargetDescription;

/**
 * Configuration information for the HotSpot Graal runtime.
 */
public interface HotSpotGraalRuntimeProvider extends GraalRuntime, RuntimeProvider, Group.Factory
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
     * Determines if the VM is currently bootstrapping the JVMCI compiler.
     */
    boolean isBootstrapping();

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
