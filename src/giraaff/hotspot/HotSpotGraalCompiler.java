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
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;

// @class HotSpotGraalCompiler
public final class HotSpotGraalCompiler implements GraalJVMCICompiler
{
    // @field
    private final HotSpotGraalRuntime ___graalRuntime;

    // @cons HotSpotGraalCompiler
    public HotSpotGraalCompiler(HotSpotGraalRuntime __graalRuntime)
    {
        super();
        this.___graalRuntime = __graalRuntime;
    }

    @Override
    public HotSpotGraalRuntime getGraalRuntime()
    {
        return this.___graalRuntime;
    }

    @Override
    public CompilationRequestResult compileMethod(CompilationRequest __request)
    {
        return compileMethod(__request, true);
    }

    CompilationRequestResult compileMethod(CompilationRequest __request, boolean __installAsDefault)
    {
        return new CompilationTask(this, (HotSpotCompilationRequest) __request, true, __installAsDefault).runCompilation();
    }

    public StructuredGraph createGraph(ResolvedJavaMethod __method, int __entryBCI, boolean __useProfilingInfo)
    {
        HotSpotBackend __backend = this.___graalRuntime.getBackend();
        HotSpotProviders __providers = __backend.getProviders();
        final boolean __isOSR = __entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
        StructuredGraph __graph = __method.isNative() || __isOSR ? null : getIntrinsicGraph(__method, __providers);

        if (__graph == null)
        {
            SpeculationLog __speculationLog = __method.getSpeculationLog();
            if (__speculationLog != null)
            {
                __speculationLog.collectFailedSpeculations();
            }
            __graph = new StructuredGraph.GraphBuilder(StructuredGraph.AllowAssumptions.ifTrue(GraalOptions.optAssumptions)).method(__method).entryBCI(__entryBCI).speculationLog(__speculationLog).useProfilingInfo(__useProfilingInfo).build();
        }
        return __graph;
    }

    public CompilationResult compile(ResolvedJavaMethod __method, int __entryBCI, boolean __useProfilingInfo)
    {
        final boolean __isOSR = __entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;

        ProfilingInfo __profilingInfo = __useProfilingInfo ? __method.getProfilingInfo(!__isOSR, __isOSR) : DefaultProfilingInfo.get(TriState.FALSE);
        OptimisticOptimizations __optimisticOpts = getOptimisticOpts(__profilingInfo);

        // Cut off never executed code profiles if there is code, e.g. after the osr loop, that is never executed.
        if (__isOSR && !GraalOptions.deoptAfterOSR)
        {
            __optimisticOpts.remove(OptimisticOptimizations.Optimization.RemoveNeverExecutedCode);
        }

        CompilationResult __result =  new CompilationResult();
        __result.setEntryBCI(__entryBCI);

        HotSpotBackend __backend = this.___graalRuntime.getBackend();
        HotSpotProviders __providers = __backend.getProviders();
        PhaseSuite<HighTierContext> __graphBuilderSuite = configGraphBuilderSuite(__providers.getSuites().getDefaultGraphBuilderSuite(), __isOSR);

        StructuredGraph __graph = createGraph(__method, __entryBCI, __useProfilingInfo);
        GraalCompiler.compileGraph(__graph, __method, __providers, __backend, __graphBuilderSuite, __optimisticOpts, __profilingInfo, getSuites(__providers), getLIRSuites(__providers), __result, CompilationResultBuilderFactory.DEFAULT);

        if (!__isOSR && __useProfilingInfo)
        {
            __profilingInfo.setCompilerIRSize(StructuredGraph.class, __graph.getNodeCount());
        }

        return __result;
    }

    ///
    // Gets a graph produced from the intrinsic for a given method that can be compiled and
    // installed for the method.
    //
    // @return an intrinsic graph that can be compiled and installed for {@code method} or null
    ///
    public StructuredGraph getIntrinsicGraph(ResolvedJavaMethod __method, HotSpotProviders __providers)
    {
        Replacements __replacements = __providers.getReplacements();
        Bytecode __subst = __replacements.getSubstitutionBytecode(__method);
        if (__subst != null)
        {
            ResolvedJavaMethod __substMethod = __subst.getMethod();
            StructuredGraph __graph = new StructuredGraph.GraphBuilder(StructuredGraph.AllowAssumptions.YES).method(__substMethod).build();
            GraphBuilderConfiguration.Plugins __plugins = new GraphBuilderConfiguration.Plugins(__providers.getGraphBuilderPlugins());
            GraphBuilderConfiguration __config = GraphBuilderConfiguration.getSnippetDefault(__plugins);
            IntrinsicContext __initialReplacementContext = new IntrinsicContext(__method, __substMethod, __subst.getOrigin(), IntrinsicContext.CompilationContext.ROOT_COMPILATION);
            new GraphBuilderPhase.GraphBuilderInstance(__providers.getMetaAccess(), __providers.getStampProvider(), __providers.getConstantReflection(), __providers.getConstantFieldProvider(), __config, OptimisticOptimizations.NONE, __initialReplacementContext).apply(__graph);
            return __graph;
        }
        return null;
    }

    protected OptimisticOptimizations getOptimisticOpts(ProfilingInfo __profilingInfo)
    {
        return new OptimisticOptimizations(__profilingInfo);
    }

    protected Suites getSuites(HotSpotProviders __providers)
    {
        return __providers.getSuites().getDefaultSuites();
    }

    protected LIRSuites getLIRSuites(HotSpotProviders __providers)
    {
        return __providers.getSuites().getDefaultLIRSuites();
    }

    ///
    // Reconfigures a given graph builder suite (GBS) if one of the given GBS parameter values is
    // not the default.
    //
    // @param suite the graph builder suite
    // @param isOSR specifies if extra OSR-specific post-processing is required (default is false)
    // @return a new suite derived from {@code suite} if any of the GBS parameters did not have a
    //         default value otherwise {@code suite}
    ///
    protected PhaseSuite<HighTierContext> configGraphBuilderSuite(PhaseSuite<HighTierContext> __suite, boolean __isOSR)
    {
        if (__isOSR)
        {
            PhaseSuite<HighTierContext> __newGbs = __suite.copy();

            // We must not clear non liveness for OSR compilations.
            GraphBuilderPhase __graphBuilderPhase = (GraphBuilderPhase) __newGbs.findPhase(GraphBuilderPhase.class).previous();
            GraphBuilderConfiguration __graphBuilderConfig = __graphBuilderPhase.getGraphBuilderConfig();
            GraphBuilderPhase __newGraphBuilderPhase = new GraphBuilderPhase(__graphBuilderConfig);
            __newGbs.findPhase(GraphBuilderPhase.class).set(__newGraphBuilderPhase);
            __newGbs.appendPhase(new OnStackReplacementPhase());

            return __newGbs;
        }
        return __suite;
    }
}
