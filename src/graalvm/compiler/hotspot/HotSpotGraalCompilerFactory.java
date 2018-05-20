package graalvm.compiler.hotspot;

import static graalvm.compiler.hotspot.HotSpotGraalOptionValues.GRAAL_OPTION_PROPERTY_PREFIX;

import java.io.PrintStream;

import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.options.OptionsParser;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

import jdk.vm.ci.hotspot.HotSpotJVMCICompilerFactory;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.runtime.JVMCIRuntime;

public final class HotSpotGraalCompilerFactory extends HotSpotJVMCICompilerFactory
{
    private static boolean compileGraalWithC1Only;

    private IsGraalPredicate isGraalPredicate;

    private final HotSpotGraalJVMCIServiceLocator locator;

    HotSpotGraalCompilerFactory(HotSpotGraalJVMCIServiceLocator locator)
    {
        this.locator = locator;
    }

    @Override
    public String getCompilerName()
    {
        return "graal";
    }

    /**
     * Initialized when this factory is {@linkplain #onSelection() selected}.
     */
    private OptionValues options;

    @Override
    public void onSelection()
    {
        options = HotSpotGraalOptionValues.HOTSPOT_OPTIONS;
        initializeGraalCompilePolicyFields(options);
        isGraalPredicate = compileGraalWithC1Only ? new IsGraalPredicate() : null;
        /*
         * Exercise this code path early to encourage loading now. This doesn't solve problem of
         * deadlock during class loading but seems to eliminate it in practice.
         */
        adjustCompilationLevelInternal(Object.class, "hashCode", "()I", CompilationLevel.FullOptimization);
        adjustCompilationLevelInternal(Object.class, "hashCode", "()I", CompilationLevel.Simple);
    }

    private static void initializeGraalCompilePolicyFields(OptionValues options)
    {
        compileGraalWithC1Only = Options.CompileGraalWithC1Only.getValue(options);
    }

    @Override
    public void printProperties(PrintStream out)
    {
        out.println("[Graal properties]");
        options.printHelp(OptionsParser.getOptionsLoader(), out, GRAAL_OPTION_PROPERTY_PREFIX);
    }

    static class Options
    {
        @Option(help = "In tiered mode compile Graal and JVMCI using optimized first tier code.", type = OptionType.Expert)
        public static final OptionKey<Boolean> CompileGraalWithC1Only = new OptionKey<>(true);
    }

    @Override
    public HotSpotGraalCompiler createCompiler(JVMCIRuntime runtime)
    {
        CompilerConfigurationFactory factory = CompilerConfigurationFactory.selectFactory(null, options);
        if (isGraalPredicate != null)
        {
            isGraalPredicate.onCompilerConfigurationFactorySelection(factory);
        }
        HotSpotGraalCompiler compiler = createCompiler("VM", runtime, options, factory);
        // Only the HotSpotGraalRuntime associated with the compiler created via
        // jdk.vm.ci.runtime.JVMCIRuntime.getCompiler() is registered for receiving
        // VM events.
        locator.onCompilerCreation(compiler);
        return compiler;
    }

    /**
     * Creates a new {@link HotSpotGraalRuntime} object and a new {@link HotSpotGraalCompiler} and
     * returns the latter.
     *
     * @param runtimeNameQualifier a qualifier to be added to the {@linkplain GraalRuntime#getName()
     *            name} of the {@linkplain HotSpotGraalCompiler#getGraalRuntime() runtime} created
     *            by this method
     * @param runtime the JVMCI runtime on which the {@link HotSpotGraalRuntime} is built
     * @param compilerConfigurationFactory factory for the {@link CompilerConfiguration}
     */
    public static HotSpotGraalCompiler createCompiler(String runtimeNameQualifier, JVMCIRuntime runtime, OptionValues options, CompilerConfigurationFactory compilerConfigurationFactory)
    {
        HotSpotJVMCIRuntime jvmciRuntime = (HotSpotJVMCIRuntime) runtime;
        HotSpotGraalRuntime graalRuntime = new HotSpotGraalRuntime(runtimeNameQualifier, jvmciRuntime, compilerConfigurationFactory, options);
        return new HotSpotGraalCompiler(jvmciRuntime, graalRuntime, graalRuntime.getOptions());
    }

    @Override
    public CompilationLevelAdjustment getCompilationLevelAdjustment()
    {
        if (compileGraalWithC1Only)
        {
            // We only decide using the class declaring the method
            // so no need to have the method name and signature
            // symbols converted to a String.
            return CompilationLevelAdjustment.ByHolder;
        }
        return CompilationLevelAdjustment.None;
    }

    @Override
    public CompilationLevel adjustCompilationLevel(Class<?> declaringClass, String name, String signature, boolean isOsr, CompilationLevel level)
    {
        return adjustCompilationLevelInternal(declaringClass, name, signature, level);
    }

    private CompilationLevel adjustCompilationLevelInternal(Class<?> declaringClass, String name, String signature, CompilationLevel level)
    {
        if (compileGraalWithC1Only)
        {
            if (level.ordinal() > CompilationLevel.Simple.ordinal())
            {
                if (isGraalPredicate.apply(declaringClass))
                {
                    return CompilationLevel.Simple;
                }
            }
        }
        return level;
    }
}
