package giraaff.hotspot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jdk.vm.ci.code.Architecture;

import org.graalvm.collections.EconomicMap;

import giraaff.debug.GraalError;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.options.OptionValues;
import giraaff.phases.BasePhase;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.serviceprovider.GraalServices;

/**
 * A factory that creates the {@link CompilerConfiguration} the Graal compiler will use.
 * Each factory must have a unique {@link #name}.
 */
public abstract class CompilerConfigurationFactory
{
    /**
     * The name of this factory. This must be unique across all factory instances.
     */
    private final String name;

    protected CompilerConfigurationFactory(String name)
    {
        this.name = name;
    }

    public abstract CompilerConfiguration createCompilerConfiguration();

    /**
     * Collect the set of available {@linkplain HotSpotBackendFactory backends} for this compiler configuration.
     */
    public BackendMap createBackendMap()
    {
        // default to backend with the same name as the compiler configuration
        return new DefaultBackendMap(name);
    }

    /**
     * Returns a name that should uniquely identify this compiler configuration.
     */
    public final String getName()
    {
        return name;
    }

    public interface BackendMap
    {
        HotSpotBackendFactory getBackendFactory(Architecture arch);
    }

    public static class DefaultBackendMap implements BackendMap
    {
        private final EconomicMap<Class<? extends Architecture>, HotSpotBackendFactory> backends = EconomicMap.create();

        public DefaultBackendMap(String backendName)
        {
            for (HotSpotBackendFactory backend : GraalServices.load(HotSpotBackendFactory.class))
            {
                if (backend.getName().equals(backendName))
                {
                    backends.put(backend.getArchitecture(), backend);
                }
            }
        }

        @Override
        public final HotSpotBackendFactory getBackendFactory(Architecture arch)
        {
            return backends.get(arch.getClass());
        }
    }

    /**
     * @return list of {@link CompilerConfigurationFactory}s
     */
    private static List<CompilerConfigurationFactory> getAllCandidates()
    {
        List<CompilerConfigurationFactory> candidates = new ArrayList<>();
        for (CompilerConfigurationFactory candidate : GraalServices.load(CompilerConfigurationFactory.class))
        {
            candidates.add(candidate);
        }
        return candidates;
    }

    /**
     * Selects and instantiates the {@link CompilerConfigurationFactory} with the given name.
     *
     * @param name the name of the compiler configuration to select
     */
    public static CompilerConfigurationFactory selectFactory(String name, OptionValues options)
    {
        CompilerConfigurationFactory factory = null;
        if (name != null)
        {
            for (CompilerConfigurationFactory candidate : GraalServices.load(CompilerConfigurationFactory.class))
            {
                if (candidate.name.equals(name))
                {
                    factory = candidate;
                    break;
                }
            }
            if (factory == null)
            {
                throw new GraalError("Graal compiler configuration '%s' not found. Available configurations are: %s", name, getAllCandidates().stream().map(c -> c.name).collect(Collectors.joining(", ")));
            }
        }
        else
        {
            throw new GraalError("No %s providers found", CompilerConfigurationFactory.class.getName());
        }
        return factory;
    }
}
