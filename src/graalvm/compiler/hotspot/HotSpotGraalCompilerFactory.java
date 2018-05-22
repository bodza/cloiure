package graalvm.compiler.hotspot;

import jdk.vm.ci.hotspot.HotSpotJVMCICompilerFactory;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.runtime.JVMCIRuntime;

import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

public final class HotSpotGraalCompilerFactory extends HotSpotJVMCICompilerFactory
{
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
        options = new OptionValues(OptionValues.newOptionMap());
    }

    @Override
    public HotSpotGraalCompiler createCompiler(JVMCIRuntime runtime)
    {
        CompilerConfigurationFactory factory = CompilerConfigurationFactory.selectFactory(null, options);
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
}
