package giraaff.hotspot;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.stack.StackIntrospection;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;
import jdk.vm.ci.runtime.JVMCIBackend;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.GraalOptions;
import giraaff.core.target.Backend;
import giraaff.debug.GraalError;
import giraaff.hotspot.CompilerConfigurationFactory.BackendMap;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.nodes.spi.StampProvider;
import giraaff.options.OptionValues;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.replacements.SnippetCounter;
import giraaff.replacements.SnippetCounter.Group;
import giraaff.runtime.RuntimeProvider;

/**
 * Singleton class holding the instance of the {@link GraalRuntime}.
 */
public final class HotSpotGraalRuntime implements HotSpotGraalRuntimeProvider
{
    private final String runtimeName;
    private final String compilerConfigurationName;
    private final HotSpotBackend hostBackend;
    private final List<SnippetCounter.Group> snippetCounterGroups;

    private final EconomicMap<Class<? extends Architecture>, HotSpotBackend> backends = EconomicMap.create(Equivalence.IDENTITY);

    private final GraalHotSpotVMConfig config;

    private final OptionValues options;

    private final HotSpotGraalCompiler compiler;

    /**
     * @param nameQualifier a qualifier to be added to this runtime's {@linkplain #getName() name}
     * @param compilerConfigurationFactory factory for the compiler configuration
     *            {@link CompilerConfigurationFactory#selectFactory(String, OptionValues)}
     */
    HotSpotGraalRuntime(String nameQualifier, HotSpotJVMCIRuntime jvmciRuntime, CompilerConfigurationFactory compilerConfigurationFactory, OptionValues options)
    {
        this.runtimeName = getClass().getSimpleName() + ":" + nameQualifier;
        HotSpotVMConfigStore store = jvmciRuntime.getConfigStore();
        config = new GraalHotSpotVMConfig(store);

        this.options = options;

        if (config.useCMSGC)
        {
            // Graal doesn't work with the CMS collector (e.g. GR-6777)
            // and is deprecated (http://openjdk.java.net/jeps/291).
            throw new GraalError("Graal does not support the CMS collector");
        }

        snippetCounterGroups = GraalOptions.SnippetCounters.getValue(options) ? new ArrayList<>() : null;
        CompilerConfiguration compilerConfiguration = compilerConfigurationFactory.createCompilerConfiguration();
        compilerConfigurationName = compilerConfigurationFactory.getName();

        compiler = new HotSpotGraalCompiler(jvmciRuntime, this, options);

        BackendMap backendMap = compilerConfigurationFactory.createBackendMap();

        JVMCIBackend hostJvmciBackend = jvmciRuntime.getHostJVMCIBackend();
        Architecture hostArchitecture = hostJvmciBackend.getTarget().arch;

        HotSpotBackendFactory factory = backendMap.getBackendFactory(hostArchitecture);
        if (factory == null)
        {
            throw new GraalError("No backend available for host architecture \"%s\"", hostArchitecture);
        }
        hostBackend = registerBackend(factory.createBackend(this, compilerConfiguration, jvmciRuntime, null));

        for (JVMCIBackend jvmciBackend : jvmciRuntime.getJVMCIBackends().values())
        {
            if (jvmciBackend == hostJvmciBackend)
            {
                continue;
            }

            Architecture gpuArchitecture = jvmciBackend.getTarget().arch;
            factory = backendMap.getBackendFactory(gpuArchitecture);
            if (factory == null)
            {
                throw new GraalError("No backend available for specified GPU architecture \"%s\"", gpuArchitecture);
            }
            registerBackend(factory.createBackend(this, compilerConfiguration, null, hostBackend));
        }

        // Complete initialization of backends
        hostBackend.completeInitialization(jvmciRuntime, options);
        for (HotSpotBackend backend : backends.getValues())
        {
            if (backend != hostBackend)
            {
                backend.completeInitialization(jvmciRuntime, options);
            }
        }

        runtimeStartTime = System.nanoTime();
        bootstrapJVMCI = config.getFlag("BootstrapJVMCI", Boolean.class);
    }

    private HotSpotBackend registerBackend(HotSpotBackend backend)
    {
        Class<? extends Architecture> arch = backend.getTarget().arch.getClass();
        HotSpotBackend oldValue = backends.put(arch, backend);
        return backend;
    }

    @Override
    public HotSpotProviders getHostProviders()
    {
        return getHostBackend().getProviders();
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
    public Group createSnippetCounterGroup(String groupName)
    {
        if (snippetCounterGroups != null)
        {
            Group group = new Group(groupName);
            snippetCounterGroups.add(group);
            return group;
        }
        return null;
    }

    @Override
    public String getName()
    {
        return runtimeName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Class<T> clazz)
    {
        if (clazz == RuntimeProvider.class)
        {
            return (T) this;
        }
        else if (clazz == OptionValues.class)
        {
            return (T) getOptions();
        }
        else if (clazz == StackIntrospection.class)
        {
            return (T) this;
        }
        else if (clazz == SnippetReflectionProvider.class)
        {
            return (T) getHostProviders().getSnippetReflection();
        }
        else if (clazz == StampProvider.class)
        {
            return (T) getHostProviders().getStampProvider();
        }
        return null;
    }

    @Override
    public HotSpotBackend getHostBackend()
    {
        return hostBackend;
    }

    @Override
    public <T extends Architecture> Backend getBackend(Class<T> arch)
    {
        return backends.get(arch);
    }

    @Override
    public String getCompilerConfigurationName()
    {
        return compilerConfigurationName;
    }

    private long runtimeStartTime;
    private boolean shutdown;

    /**
     * Take action related to entering a new execution phase.
     *
     * @param phase the execution phase being entered
     */
    void phaseTransition(String phase)
    {
    }

    void shutdown()
    {
        shutdown = true;

        phaseTransition("final");
    }

    private final boolean bootstrapJVMCI;
    private boolean bootstrapFinished;

    public void notifyBootstrapFinished()
    {
        bootstrapFinished = true;
    }

    @Override
    public boolean isBootstrapping()
    {
        return bootstrapJVMCI && !bootstrapFinished;
    }

    @Override
    public boolean isShutdown()
    {
        return shutdown;
    }
}
