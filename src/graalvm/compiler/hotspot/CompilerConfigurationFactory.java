package graalvm.compiler.hotspot;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.TTY;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.lir.phases.LIRPhaseSuite;
import graalvm.compiler.options.EnumOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.serviceprovider.GraalServices;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.common.InitTimer;

/**
 * A factory that creates the {@link CompilerConfiguration} the Graal compiler will use. Each
 * factory must have a unique {@link #name} and {@link #autoSelectionPriority}. The latter imposes a
 * total ordering between factories for the purpose of auto-selecting the factory to use.
 */
public abstract class CompilerConfigurationFactory implements Comparable<CompilerConfigurationFactory>
{
    enum ShowConfigurationLevel
    {
        none,
        info,
        verbose
    }

    static class Options
    {
        @Option(help = "Names the Graal compiler configuration to use. If ommitted, the compiler configuration " +
                       "with the highest auto-selection priority is used. To see the set of available configurations, " +
                       "supply the value 'help' to this option.", type = OptionType.Expert)
        public static final OptionKey<String> CompilerConfiguration = new OptionKey<>(null);
        @Option(help = "Writes to the VM log information about the Graal compiler configuration selected.", type = OptionType.User)
        public static final OptionKey<ShowConfigurationLevel> ShowConfiguration = new EnumOptionKey<>(ShowConfigurationLevel.none);
    }

    /**
     * The name of this factory. This must be unique across all factory instances and is used when
     * selecting a factory based on the value of {@link Options#CompilerConfiguration}.
     */
    private final String name;

    /**
     * The priority of this factory. This must be unique across all factory instances and is used
     * when selecting a factory when {@link Options#CompilerConfiguration} is omitted
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
     * Selects and instantiates a {@link CompilerConfigurationFactory}. The selection algorithm is
     * as follows: if {@code name} is non-null, then select the factory with the same name else if
     * {@code Options.CompilerConfiguration.getValue()} is non-null then select the factory whose
     * name matches the value else select the factory with the highest
     * {@link #autoSelectionPriority} value.
     *
     * @param name the name of the compiler configuration to select (optional)
     */
    public static CompilerConfigurationFactory selectFactory(String name, OptionValues options)
    {
        CompilerConfigurationFactory factory = null;
        String value = name == null ? Options.CompilerConfiguration.getValue(options) : name;
        if ("help".equals(value))
        {
            System.out.println("The available Graal compiler configurations are:");
            for (CompilerConfigurationFactory candidate : getAllCandidates())
            {
                System.out.println("    " + candidate.name);
            }
            System.exit(0);
        }
        else if (value != null)
        {
            for (CompilerConfigurationFactory candidate : GraalServices.load(CompilerConfigurationFactory.class))
            {
                if (candidate.name.equals(value))
                {
                    factory = candidate;
                    break;
                }
            }
            if (factory == null)
            {
                throw new GraalError("Graal compiler configuration '%s' not found. Available configurations are: %s", value, getAllCandidates().stream().map(c -> c.name).collect(Collectors.joining(", ")));
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
        ShowConfigurationLevel level = Options.ShowConfiguration.getValue(options);
        if (level != ShowConfigurationLevel.none)
        {
            switch (level)
            {
                case info:
                {
                    printConfigInfo(factory);
                    break;
                }
                case verbose:
                {
                    printConfigInfo(factory);
                    CompilerConfiguration config = factory.createCompilerConfiguration();
                    TTY.println("High tier: " + phaseNames(config.createHighTier(options)));
                    TTY.println("Mid tier: " + phaseNames(config.createMidTier(options)));
                    TTY.println("Low tier: " + phaseNames(config.createLowTier(options)));
                    TTY.println("Pre regalloc stage: " + phaseNames(config.createPreAllocationOptimizationStage(options)));
                    TTY.println("Regalloc stage: " + phaseNames(config.createAllocationStage(options)));
                    TTY.println("Post regalloc stage: " + phaseNames(config.createPostAllocationOptimizationStage(options)));
                    config.createAllocationStage(options);
                    break;
                }
            }
        }
        return factory;
    }

    private static void printConfigInfo(CompilerConfigurationFactory factory)
    {
        URL location = factory.getClass().getResource(factory.getClass().getSimpleName() + ".class");
        TTY.printf("Using Graal compiler configuration '%s' provided by %s loaded from %s%n", factory.name, factory.getClass().getName(), location);
    }

    private static <C> List<String> phaseNames(PhaseSuite<C> suite)
    {
        Collection<BasePhase<? super C>> phases = suite.getPhases();
        List<String> res = new ArrayList<>(phases.size());
        for (BasePhase<?> phase : phases)
        {
            res.add(phase.contractorName());
        }
        Collections.sort(res);
        return res;
    }

    private static <C> List<String> phaseNames(LIRPhaseSuite<C> suite)
    {
        List<LIRPhase<C>> phases = suite.getPhases();
        List<String> res = new ArrayList<>(phases.size());
        for (LIRPhase<?> phase : phases)
        {
            res.add(phase.getClass().getName());
        }
        Collections.sort(res);
        return res;
    }
}
