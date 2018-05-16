package graalvm.compiler.hotspot;

import java.util.Map;

import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.core.CompilationWrapper.ExceptionAction;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.debug.DiagnosticsOutputDirectory;
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
     * Opens a debug context for compiling {@code compilable}. The {@link DebugContext#close()}
     * method should be called on the returned object once the compilation is finished.
     *
     * @param compilationOptions the options used to configure the compilation debug context
     * @param compilationId a system wide unique compilation id
     * @param compilable the input to the compilation
     */
    DebugContext openDebugContext(OptionValues compilationOptions, CompilationIdentifier compilationId, Object compilable, Iterable<DebugHandlersFactory> factories);

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
     * Gets a directory into which diagnostics such crash reports and dumps should be written.
     */
    DiagnosticsOutputDirectory getOutputDirectory();

    /**
     * Gets the map used to count compilation problems at each {@link ExceptionAction} level.
     */
    Map<ExceptionAction, Integer> getCompilationProblemsPerAction();

    /**
     * Returns the unique compiler configuration name that is in use. Useful for users to find out
     * which configuration is in use.
     */
    String getCompilerConfigurationName();
}
