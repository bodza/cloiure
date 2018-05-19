package graalvm.compiler.hotspot;

import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider.getArrayIndexScale;
import static graalvm.compiler.core.common.GraalOptions.GeneratePIC;
import static graalvm.compiler.core.common.GraalOptions.HotSpotPrintInlining;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableMapCursor;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.target.Backend;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.TTY;
import graalvm.compiler.hotspot.CompilerConfigurationFactory.BackendMap;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.options.EnumOptionKey;
import graalvm.compiler.options.OptionDescriptor;
import graalvm.compiler.options.OptionDescriptors;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.options.OptionsParser;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.replacements.SnippetCounter;
import graalvm.compiler.replacements.SnippetCounter.Group;
import graalvm.compiler.runtime.RuntimeProvider;
import graalvm.compiler.serviceprovider.GraalServices;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.stack.StackIntrospection;
import jdk.vm.ci.common.InitTimer;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIBackend;

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

    /**
     * The options can be {@linkplain #setOptionValues(String[], String[]) updated} by external
     * interfaces such as JMX. This comes with the risk that inconsistencies can arise as an
     * {@link OptionValues} object can be cached by various parts of Graal instead of always
     * obtaining them from this object. However, concurrent updates are never lost.
     */
    private AtomicReference<OptionValues> optionsRef = new AtomicReference<>();

    private final HotSpotGraalCompiler compiler;

    /**
     * @param nameQualifier a qualifier to be added to this runtime's {@linkplain #getName() name}
     * @param compilerConfigurationFactory factory for the compiler configuration
     *            {@link CompilerConfigurationFactory#selectFactory(String, OptionValues)}
     */
    @SuppressWarnings("try")
    HotSpotGraalRuntime(String nameQualifier, HotSpotJVMCIRuntime jvmciRuntime, CompilerConfigurationFactory compilerConfigurationFactory, OptionValues initialOptions)
    {
        this.runtimeName = getClass().getSimpleName() + ":" + nameQualifier;
        HotSpotVMConfigStore store = jvmciRuntime.getConfigStore();
        config = GeneratePIC.getValue(initialOptions) ? new AOTGraalHotSpotVMConfig(store) : new GraalHotSpotVMConfig(store);

        // Only set HotSpotPrintInlining if it still has its default value (false).
        if (GraalOptions.HotSpotPrintInlining.getValue(initialOptions) == false && config.printInlining)
        {
            optionsRef.set(new OptionValues(initialOptions, HotSpotPrintInlining, true));
        }
        else
        {
            optionsRef.set(initialOptions);
        }
        OptionValues options = optionsRef.get();

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
        return optionsRef.get();
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
            return (T) optionsRef.get();
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

        if (snippetCounterGroups != null)
        {
            for (Group group : snippetCounterGroups)
            {
                TTY.out().out().println(group);
            }
        }
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

    /**
     * Sets or updates this object's {@linkplain #getOptions() options} from {@code names} and
     * {@code values}.
     *
     * @param values the values to set. The empty string represents {@code null} which resets an
     *            option to its default value. For string type options, a non-empty value must be
     *            enclosed in double quotes.
     * @return an array of Strings where the element at index i is {@code names[i]} if setting the
     *         denoted option succeeded, {@code null} if the option is unknown otherwise an error
     *         message describing the failure to set the option
     */
    public String[] setOptionValues(String[] names, String[] values)
    {
        EconomicMap<String, OptionDescriptor> optionDescriptors = getOptionDescriptors();
        EconomicMap<OptionKey<?>, Object> newValues = EconomicMap.create(names.length);
        EconomicSet<OptionKey<?>> resetValues = EconomicSet.create(names.length);
        String[] result = new String[names.length];
        for (int i = 0; i < names.length; i++)
        {
            String name = names[i];
            OptionDescriptor option = optionDescriptors.get(name);
            if (option != null)
            {
                String svalue = values[i];
                Class<?> optionValueType = option.getOptionValueType();
                OptionKey<?> optionKey = option.getOptionKey();
                if (svalue == null || svalue.isEmpty() && !(optionKey instanceof EnumOptionKey))
                {
                    resetValues.add(optionKey);
                    result[i] = name;
                }
                else
                {
                    String valueToParse;
                    if (optionValueType == String.class)
                    {
                        if (svalue.length() < 2 || svalue.charAt(0) != '"' || svalue.charAt(svalue.length() - 1) != '"')
                        {
                            result[i] = "Invalid value for String option '" + name + "': must be the empty string or be enclosed in double quotes: " + svalue;
                            continue;
                        }
                        else
                        {
                            valueToParse = svalue.substring(1, svalue.length() - 1);
                        }
                    }
                    else
                    {
                        valueToParse = svalue;
                    }
                    try
                    {
                        OptionsParser.parseOption(name, valueToParse, newValues, OptionsParser.getOptionsLoader());
                        result[i] = name;
                    }
                    catch (IllegalArgumentException e)
                    {
                        result[i] = e.getMessage();
                        continue;
                    }
                }
            }
            else
            {
                result[i] = null;
            }
        }

        OptionValues currentOptions;
        OptionValues newOptions;
        do
        {
            currentOptions = optionsRef.get();
            UnmodifiableMapCursor<OptionKey<?>, Object> cursor = currentOptions.getMap().getEntries();
            while (cursor.advance())
            {
                OptionKey<?> key = cursor.getKey();
                if (!resetValues.contains(key) && !newValues.containsKey(key))
                {
                    newValues.put(key, OptionValues.decodeNull(cursor.getValue()));
                }
            }
            newOptions = new OptionValues(newValues);
        } while (!optionsRef.compareAndSet(currentOptions, newOptions));

        return result;
    }

    /**
     * Gets the values for the options corresponding to {@code names} encoded as strings. The empty
     * string represents {@code null}. For string type options, non-{@code null} values will be
     * enclosed in double quotes.
     *
     * @param names a list of option names
     * @return the values for each named option. If an element in {@code names} does not denote an
     *         existing option, the corresponding element in the returned array will be {@code null}
     */
    public String[] getOptionValues(String... names)
    {
        String[] values = new String[names.length];
        EconomicMap<String, OptionDescriptor> optionDescriptors = getOptionDescriptors();
        for (int i = 0; i < names.length; i++)
        {
            OptionDescriptor option = optionDescriptors.get(names[i]);
            if (option != null)
            {
                OptionKey<?> optionKey = option.getOptionKey();
                Object value = optionKey.getValue(getOptions());
                String svalue;
                if (option.getOptionValueType() == String.class && value != null)
                {
                    svalue = "\"" + value + "\"";
                }
                else if (value == null)
                {
                    svalue = "";
                }
                else
                {
                    svalue = String.valueOf(value);
                }
                values[i] = svalue;
            }
            else
            {
                // null denotes the option does not exist
                values[i] = null;
            }
        }
        return values;
    }

    private static EconomicMap<String, OptionDescriptor> getOptionDescriptors()
    {
        EconomicMap<String, OptionDescriptor> result = EconomicMap.create();
        for (OptionDescriptors set : OptionsParser.getOptionsLoader())
        {
            for (OptionDescriptor option : set)
            {
                result.put(option.getName(), option);
            }
        }
        return result;
    }

    private static <T> T param(Object[] arr, int index, String name, Class<T> type, T defaultValue)
    {
        Object value = arr.length > index ? arr[index] : null;
        if (value == null || (value instanceof String && ((String) value).isEmpty()))
        {
            if (defaultValue == null)
            {
                throw new IllegalArgumentException(name + " must be specified");
            }
            value = defaultValue;
        }
        if (type.isInstance(value))
        {
            return type.cast(value);
        }
        throw new IllegalArgumentException("Expecting " + type.getName() + " for " + name + " but was " + value);
    }
}
