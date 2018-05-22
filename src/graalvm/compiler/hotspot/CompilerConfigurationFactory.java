package graalvm.compiler.hotspot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jdk.vm.ci.code.Architecture;

import org.graalvm.collections.EconomicMap;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.lir.phases.LIRPhaseSuite;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.serviceprovider.GraalServices;

/**
 * A factory that creates the {@link CompilerConfiguration} the Graal compiler will use. Each
 * factory must have a unique {@link #name} and {@link #autoSelectionPriority}. The latter imposes a
 * total ordering between factories for the purpose of auto-selecting the factory to use.
 */
public abstract class CompilerConfigurationFactory implements Comparable<CompilerConfigurationFactory>
{
    /**
     * The name of this factory. This must be unique across all factory instances.
     */
    private final String name;

    /**
     * The priority of this factory. This must be unique across all factory instances.
     */
    private final int autoSelectionPriority;

    protected CompilerConfigurationFactory(String name, int autoSelectionPriority)
    {
        this.name = name;
        this.autoSelectionPriority = autoSelectionPriority;
    }

    public abstract CompilerConfiguration createCompilerConfiguration();

    /**
     * Collect the set of available {@linkplain HotSpotBackendFactory backends} for this compiler
     * configuration.
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
                    Class<? extends Architecture> arch = backend.getArchitecture();
                    HotSpotBackendFactory oldEntry = backends.put(arch, backend);
                }
            }
        }

        @Override
        public final HotSpotBackendFactory getBackendFactory(Architecture arch)
        {
            return backends.get(arch.getClass());
        }
    }

    @Override
    public int compareTo(CompilerConfigurationFactory o)
    {
        if (autoSelectionPriority > o.autoSelectionPriority)
        {
            return -1;
        }
        if (autoSelectionPriority < o.autoSelectionPriority)
        {
            return 1;
        }
        return 0;
    }

    /**
     * @return sorted list of {@link CompilerConfigurationFactory}s
     */
    private static List<CompilerConfigurationFactory> getAllCandidates()
    {
        List<CompilerConfigurationFactory> candidates = new ArrayList<>();
        for (CompilerConfigurationFactory candidate : GraalServices.load(CompilerConfigurationFactory.class))
        {
            candidates.add(candidate);
        }
        Collections.sort(candidates);
        return candidates;
    }

    /**
     * Selects and instantiates a {@link CompilerConfigurationFactory}. The selection algorithm
     * is as follows: if {@code name} is non-null, then select the factory with the same name;
     * else select the factory with the highest {@link #autoSelectionPriority} value.
     *
     * @param name the name of the compiler configuration to select (optional)
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
            List<CompilerConfigurationFactory> candidates = getAllCandidates();
            if (candidates.isEmpty())
            {
                throw new GraalError("No %s providers found", CompilerConfigurationFactory.class.getName());
            }
            factory = candidates.get(0);
        }
        return factory;
    }
}
