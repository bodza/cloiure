package giraaff.hotspot;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.meta.TriState;
import jdk.vm.ci.runtime.JVMCICompiler;

import giraaff.api.runtime.GraalJVMCICompiler;
import giraaff.bytecode.Bytecode;
import giraaff.code.CompilationResult;
import giraaff.core.GraalCompiler;
import giraaff.core.common.GraalOptions;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.phases.OnStackReplacementPhase;
import giraaff.java.GraphBuilderPhase;
import giraaff.lir.asm.CompilationResultBuilderFactory;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.AllowAssumptions;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.OptimisticOptimizations.Optimization;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;

// @class HotSpotGraalCompiler
public final class HotSpotGraalCompiler implements GraalJVMCICompiler
{
    private final HotSpotGraalRuntime graalRuntime;

    // @cons
    public HotSpotGraalCompiler(HotSpotGraalRuntime graalRuntime)
    {
        super();
        this.graalRuntime = graalRuntime;
    }

    @Override
    public HotSpotGraalRuntime getGraalRuntime()
    {
        return graalRuntime;
    }

    @Override
    public CompilationRequestResult compileMethod(CompilationRequest request)
    {
        return compileMethod(request, true);
    }

    CompilationRequestResult compileMethod(CompilationRequest request, boolean installAsDefault)
    {
        return new CompilationTask(this, (HotSpotCompilationRequest) request, true, installAsDefault).runCompilation();
    }

    public StructuredGraph createGraph(ResolvedJavaMethod method, int entryBCI, boolean useProfilingInfo)
    {
        HotSpotBackend backend = graalRuntime.getBackend();
        HotSpotProviders providers = backend.getProviders();
        final boolean isOSR = entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
        StructuredGraph graph = method.isNative() || isOSR ? null : getIntrinsicGraph(method, providers);

        if (graph == null)
        {
            SpeculationLog speculationLog = method.getSpeculationLog();
            if (speculationLog != null)
            {
                speculationLog.collectFailedSpeculations();
            }
            graph = new StructuredGraph.Builder(AllowAssumptions.ifTrue(GraalOptions.optAssumptions)).method(method).entryBCI(entryBCI).speculationLog(speculationLog).useProfilingInfo(useProfilingInfo).build();
        }
        return graph;
    }

    public CompilationResult compile(ResolvedJavaMethod method, int entryBCI, boolean useProfilingInfo)
    {
        final boolean isOSR = entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;

        ProfilingInfo profilingInfo = useProfilingInfo ? method.getProfilingInfo(!isOSR, isOSR) : DefaultProfilingInfo.get(TriState.FALSE);
        OptimisticOptimizations optimisticOpts = getOptimisticOpts(profilingInfo);

        // Cut off never executed code profiles if there is code, e.g. after the osr loop, that is never executed.
        if (isOSR && !GraalOptions.deoptAfterOSR)
        {
            optimisticOpts.remove(Optimization.RemoveNeverExecutedCode);
        }

        CompilationResult result =  new CompilationResult();
        result.setEntryBCI(entryBCI);

        HotSpotBackend backend = graalRuntime.getBackend();
        HotSpotProviders providers = backend.getProviders();
        PhaseSuite<HighTierContext> graphBuilderSuite = configGraphBuilderSuite(providers.getSuites().getDefaultGraphBuilderSuite(), isOSR);

        StructuredGraph graph = createGraph(method, entryBCI, useProfilingInfo);
        GraalCompiler.compileGraph(graph, method, providers, backend, graphBuilderSuite, optimisticOpts, profilingInfo, getSuites(providers), getLIRSuites(providers), result, CompilationResultBuilderFactory.Default);

        if (!isOSR && useProfilingInfo)
        {
            profilingInfo.setCompilerIRSize(StructuredGraph.class, graph.getNodeCount());
        }

        return result;
    }

    /**
     * Gets a graph produced from the intrinsic for a given method that can be compiled and
     * installed for the method.
     *
     * @return an intrinsic graph that can be compiled and installed for {@code method} or null
     */
    public StructuredGraph getIntrinsicGraph(ResolvedJavaMethod method, HotSpotProviders providers)
    {
        Replacements replacements = providers.getReplacements();
        Bytecode subst = replacements.getSubstitutionBytecode(method);
        if (subst != null)
        {
            ResolvedJavaMethod substMethod = subst.getMethod();
            StructuredGraph graph = new StructuredGraph.Builder(AllowAssumptions.YES).method(substMethod).build();
            Plugins plugins = new Plugins(providers.getGraphBuilderPlugins());
            GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);
            IntrinsicContext initialReplacementContext = new IntrinsicContext(method, substMethod, subst.getOrigin(), CompilationContext.ROOT_COMPILATION);
            new GraphBuilderPhase.Instance(providers.getMetaAccess(), providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), config, OptimisticOptimizations.NONE, initialReplacementContext).apply(graph);
            return graph;
        }
        return null;
    }

    protected OptimisticOptimizations getOptimisticOpts(ProfilingInfo profilingInfo)
    {
        return new OptimisticOptimizations(profilingInfo);
    }

    protected Suites getSuites(HotSpotProviders providers)
    {
        return providers.getSuites().getDefaultSuites();
    }

    protected LIRSuites getLIRSuites(HotSpotProviders providers)
    {
        return providers.getSuites().getDefaultLIRSuites();
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
}
