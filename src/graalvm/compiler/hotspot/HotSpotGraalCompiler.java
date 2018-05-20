package graalvm.compiler.hotspot;

import static graalvm.compiler.core.common.GraalOptions.OptAssumptions;
import static graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext.CompilationContext.ROOT_COMPILATION;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Formattable;
import java.util.Formatter;

import graalvm.compiler.api.runtime.GraalJVMCICompiler;
import graalvm.compiler.bytecode.Bytecode;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.GraalCompiler;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.phases.OnStackReplacementPhase;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.OptimisticOptimizations.Optimization;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.Suites;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotCompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.meta.TriState;
import jdk.vm.ci.runtime.JVMCICompiler;

public class HotSpotGraalCompiler implements GraalJVMCICompiler
{
    private final HotSpotJVMCIRuntimeProvider jvmciRuntime;
    private final HotSpotGraalRuntimeProvider graalRuntime;

    HotSpotGraalCompiler(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalRuntimeProvider graalRuntime, OptionValues options)
    {
        this.jvmciRuntime = jvmciRuntime;
        this.graalRuntime = graalRuntime;
    }

    @Override
    public HotSpotGraalRuntimeProvider getGraalRuntime()
    {
        return graalRuntime;
    }

    @Override
    public CompilationRequestResult compileMethod(CompilationRequest request)
    {
        return compileMethod(request, true, graalRuntime.getOptions());
    }

    CompilationRequestResult compileMethod(CompilationRequest request, boolean installAsDefault, OptionValues options)
    {
        if (graalRuntime.isShutdown())
        {
            return HotSpotCompilationRequestResult.failure(String.format("Shutdown entered"), false);
        }

        HotSpotCompilationRequest hsRequest = (HotSpotCompilationRequest) request;
        return new CompilationTask(jvmciRuntime, this, hsRequest, true, installAsDefault, options).runCompilation();
    }

    public StructuredGraph createGraph(ResolvedJavaMethod method, int entryBCI, boolean useProfilingInfo, CompilationIdentifier compilationId, OptionValues options)
    {
        HotSpotBackend backend = graalRuntime.getHostBackend();
        HotSpotProviders providers = backend.getProviders();
        final boolean isOSR = entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
        StructuredGraph graph = method.isNative() || isOSR ? null : getIntrinsicGraph(method, providers, compilationId, options);

        if (graph == null)
        {
            SpeculationLog speculationLog = method.getSpeculationLog();
            if (speculationLog != null)
            {
                speculationLog.collectFailedSpeculations();
            }
            graph = new StructuredGraph.Builder(options, AllowAssumptions.ifTrue(OptAssumptions.getValue(options))).method(method).entryBCI(entryBCI).speculationLog(speculationLog).useProfilingInfo(useProfilingInfo).compilationId(compilationId).build();
        }
        return graph;
    }

    public CompilationResult compileHelper(CompilationResultBuilderFactory crbf, CompilationResult result, StructuredGraph graph, ResolvedJavaMethod method, int entryBCI, boolean useProfilingInfo, OptionValues options)
    {
        HotSpotBackend backend = graalRuntime.getHostBackend();
        HotSpotProviders providers = backend.getProviders();
        final boolean isOSR = entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;

        Suites suites = getSuites(providers, options);
        LIRSuites lirSuites = getLIRSuites(providers, options);
        ProfilingInfo profilingInfo = useProfilingInfo ? method.getProfilingInfo(!isOSR, isOSR) : DefaultProfilingInfo.get(TriState.FALSE);
        OptimisticOptimizations optimisticOpts = getOptimisticOpts(profilingInfo, options);

        /*
         * Cut off never executed code profiles if there is code, e.g. after the osr loop, that is
         * never executed.
         */
        if (isOSR && !OnStackReplacementPhase.Options.DeoptAfterOSR.getValue(options))
        {
            optimisticOpts.remove(Optimization.RemoveNeverExecutedCode);
        }

        result.setEntryBCI(entryBCI);
        PhaseSuite<HighTierContext> graphBuilderSuite = configGraphBuilderSuite(providers.getSuites().getDefaultGraphBuilderSuite(), isOSR);
        GraalCompiler.compileGraph(graph, method, providers, backend, graphBuilderSuite, optimisticOpts, profilingInfo, suites, lirSuites, result, crbf);

        if (!isOSR && useProfilingInfo)
        {
            ProfilingInfo profile = profilingInfo;
            profile.setCompilerIRSize(StructuredGraph.class, graph.getNodeCount());
        }

        return result;
    }

    public CompilationResult compile(ResolvedJavaMethod method, int entryBCI, boolean useProfilingInfo, CompilationIdentifier compilationId, OptionValues options)
    {
        StructuredGraph graph = createGraph(method, entryBCI, useProfilingInfo, compilationId, options);
        CompilationResult result = new CompilationResult(compilationId);
        return compileHelper(CompilationResultBuilderFactory.Default, result, graph, method, entryBCI, useProfilingInfo, options);
    }

    /**
     * Gets a graph produced from the intrinsic for a given method that can be compiled and
     * installed for the method.
     *
     * @return an intrinsic graph that can be compiled and installed for {@code method} or null
     */
    public StructuredGraph getIntrinsicGraph(ResolvedJavaMethod method, HotSpotProviders providers, CompilationIdentifier compilationId, OptionValues options)
    {
        Replacements replacements = providers.getReplacements();
        Bytecode subst = replacements.getSubstitutionBytecode(method);
        if (subst != null)
        {
            ResolvedJavaMethod substMethod = subst.getMethod();
            StructuredGraph graph = new StructuredGraph.Builder(options, AllowAssumptions.YES).method(substMethod).compilationId(compilationId).build();
            Plugins plugins = new Plugins(providers.getGraphBuilderPlugins());
            GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);
            IntrinsicContext initialReplacementContext = new IntrinsicContext(method, substMethod, subst.getOrigin(), ROOT_COMPILATION);
            new GraphBuilderPhase.Instance(providers.getMetaAccess(), providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), config, OptimisticOptimizations.NONE, initialReplacementContext).apply(graph);
            return graph;
        }
        return null;
    }

    protected OptimisticOptimizations getOptimisticOpts(ProfilingInfo profilingInfo, OptionValues options)
    {
        return new OptimisticOptimizations(profilingInfo, options);
    }

    protected Suites getSuites(HotSpotProviders providers, OptionValues options)
    {
        return providers.getSuites().getDefaultSuites(options);
    }

    protected LIRSuites getLIRSuites(HotSpotProviders providers, OptionValues options)
    {
        return providers.getSuites().getDefaultLIRSuites(options);
    }

    /**
     * Reconfigures a given graph builder suite (GBS) if one of the given GBS parameter values is
     * not the default.
     *
     * @param suite the graph builder suite
     * @param isOSR specifies if extra OSR-specific post-processing is required (default is false)
     * @return a new suite derived from {@code suite} if any of the GBS parameters did not have a
     *         default value otherwise {@code suite}
     */
    protected PhaseSuite<HighTierContext> configGraphBuilderSuite(PhaseSuite<HighTierContext> suite, boolean isOSR)
    {
        if (isOSR)
        {
            PhaseSuite<HighTierContext> newGbs = suite.copy();

            // We must not clear non liveness for OSR compilations.
            GraphBuilderPhase graphBuilderPhase = (GraphBuilderPhase) newGbs.findPhase(GraphBuilderPhase.class).previous();
            GraphBuilderConfiguration graphBuilderConfig = graphBuilderPhase.getGraphBuilderConfig();
            GraphBuilderPhase newGraphBuilderPhase = new GraphBuilderPhase(graphBuilderConfig);
            newGbs.findPhase(GraphBuilderPhase.class).set(newGraphBuilderPhase);
            newGbs.appendPhase(new OnStackReplacementPhase());

            return newGbs;
        }
        return suite;
    }

    /**
     * Converts {@code method} to a String with {@link JavaMethod#format(String)} and the format
     * string {@code "%H.%n(%p)"}.
     */
    static String str(JavaMethod method)
    {
        return method.format("%H.%n(%p)");
    }

    /**
     * Wraps {@code obj} in a {@link Formatter} that standardizes formatting for certain objects.
     */
    static Formattable fmt(Object obj)
    {
        return new Formattable()
        {
            @Override
            public void formatTo(Formatter buf, int flags, int width, int precision)
            {
                if (obj instanceof Throwable)
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ((Throwable) obj).printStackTrace(new PrintStream(baos));
                    buf.format("%s", baos.toString());
                }
                else if (obj instanceof StackTraceElement[])
                {
                    for (StackTraceElement e : (StackTraceElement[]) obj)
                    {
                        buf.format("\t%s%n", e);
                    }
                }
                else if (obj instanceof JavaMethod)
                {
                    buf.format("%s", str((JavaMethod) obj));
                }
                else
                {
                    buf.format("%s", obj);
                }
            }
        };
    }
}
