package graalvm.compiler.truffle.compiler.hotspot;

import static graalvm.compiler.core.GraalCompiler.compileGraph;
import static graalvm.compiler.debug.DebugOptions.DebugStubsAndSnippets;
import static graalvm.compiler.hotspot.meta.HotSpotSuitesProvider.withNodeSourcePosition;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.getOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.code.CompilationResult;
import graalvm.compiler.core.CompilationWrapper.ExceptionAction;
import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.core.common.CompilationRequestIdentifier;
import graalvm.compiler.core.target.Backend;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugContext.Activation;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.debug.DiagnosticsOutputDirectory;
import graalvm.compiler.hotspot.HotSpotCompilationIdentifier;
import graalvm.compiler.hotspot.HotSpotCompiledCodeBuilder;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.AbstractInliningPhase;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.phases.tiers.SuitesProvider;
import graalvm.compiler.printer.GraalDebugHandlersFactory;
import graalvm.compiler.serviceprovider.GraalServices;
import graalvm.compiler.truffle.common.CompilableTruffleAST;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.common.TruffleCompilerRuntime;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompiler;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompilerRuntime;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleInstalledCode;
import graalvm.compiler.truffle.compiler.TruffleCompilerImpl;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCICompiler;

public final class HotSpotTruffleCompilerImpl extends TruffleCompilerImpl implements HotSpotTruffleCompiler {

    /**
     * The HotSpot-specific Graal runtime associated with this compiler.
     */
    private final HotSpotGraalRuntimeProvider hotspotGraalRuntime;

    public static HotSpotTruffleCompilerImpl create(TruffleCompilerRuntime runtime) {
        HotSpotGraalRuntimeProvider hotspotGraalRuntime = (HotSpotGraalRuntimeProvider) runtime.getGraalRuntime();
        Backend backend = hotspotGraalRuntime.getHostBackend();
        OptionValues options = TruffleCompilerOptions.getOptions();
        Suites suites = backend.getSuites().getDefaultSuites(options);
        LIRSuites lirSuites = backend.getSuites().getDefaultLIRSuites(options);
        GraphBuilderPhase phase = (GraphBuilderPhase) backend.getSuites().getDefaultGraphBuilderSuite().findPhase(GraphBuilderPhase.class).previous();
        Plugins plugins = phase.getGraphBuilderConfig().getPlugins();
        SnippetReflectionProvider snippetReflection = hotspotGraalRuntime.getRequiredCapability(SnippetReflectionProvider.class);
        return new HotSpotTruffleCompilerImpl(hotspotGraalRuntime, runtime, plugins, suites, lirSuites, backend, snippetReflection);
    }

    private HotSpotTruffleCompilerImpl(HotSpotGraalRuntimeProvider hotspotGraalRuntime, TruffleCompilerRuntime runtime, Plugins plugins, Suites suites, LIRSuites lirSuites, Backend backend,
                    SnippetReflectionProvider snippetReflection) {
        super(runtime, plugins, suites, lirSuites, backend, snippetReflection);
        this.hotspotGraalRuntime = hotspotGraalRuntime;
        installTruffleCallBoundaryMethods();
    }

    @Override
    public CompilationRequestIdentifier getCompilationIdentifier(CompilableTruffleAST compilable) {
        ResolvedJavaMethod rootMethod = partialEvaluator.rootForCallTarget(compilable);
        HotSpotCompilationRequest request = new HotSpotCompilationRequest((HotSpotResolvedJavaMethod) rootMethod, JVMCICompiler.INVOCATION_ENTRY_BCI, 0L);
        return new HotSpotTruffleCompilationIdentifier(request, compilable);
    }

    private volatile List<DebugHandlersFactory> factories;

    private List<DebugHandlersFactory> getDebugHandlerFactories() {
        if (factories == null) {
            // Multiple initialization by racing threads is harmless
            List<DebugHandlersFactory> list = new ArrayList<>();
            list.add(new GraalDebugHandlersFactory(snippetReflection));
            for (DebugHandlersFactory factory : DebugHandlersFactory.LOADER) {
                // Ignore other instances of GraalDebugHandlersFactory
                if (!(factory instanceof GraalDebugHandlersFactory)) {
                    list.add(factory);
                }
            }
            factories = list;
        }
        return factories;
    }

    @Override
    public String getCompilerConfigurationName() {
        return hotspotGraalRuntime.getCompilerConfigurationName();
    }

    @Override
    public DebugContext openDebugContext(OptionValues options, CompilationIdentifier compilationId, CompilableTruffleAST compilable) {
        return hotspotGraalRuntime.openDebugContext(options, compilationId, compilable, getDebugHandlerFactories());
    }

    @Override
    protected HotSpotPartialEvaluator createPartialEvaluator() {
        return new HotSpotPartialEvaluator(providers, config, snippetReflection, backend.getTarget().arch, getInstrumentation());
    }

    @Override
    protected PhaseSuite<HighTierContext> createGraphBuilderSuite() {
        return backend.getSuites().getDefaultGraphBuilderSuite().copy();
    }

    /**
     * @see #compileTruffleCallBoundaryMethod
     */
    @Override
    @SuppressWarnings("try")
    public void installTruffleCallBoundaryMethods() {
        HotSpotTruffleCompilerRuntime runtime = (HotSpotTruffleCompilerRuntime) TruffleCompilerRuntime.getRuntime();
        for (ResolvedJavaMethod method : runtime.getTruffleCallBoundaryMethods()) {
            HotSpotCompilationIdentifier compilationId = (HotSpotCompilationIdentifier) backend.getCompilationIdentifier(method);
            OptionValues options = getOptions();
            try (DebugContext debug = DebugStubsAndSnippets.getValue(options) ? hotspotGraalRuntime.openDebugContext(options, compilationId, method, getDebugHandlerFactories())
                            : DebugContext.DISABLED;
                            Activation a = debug.activate();
                            DebugContext.Scope d = debug.scope("InstallingTruffleStub")) {
                CompilationResult compResult = compileTruffleCallBoundaryMethod(method, compilationId, debug);
                CodeCacheProvider codeCache = providers.getCodeCache();
                try (DebugContext.Scope s = debug.scope("CodeInstall", codeCache, method, compResult)) {
                    CompiledCode compiledCode = HotSpotCompiledCodeBuilder.createCompiledCode(codeCache, method, compilationId.getRequest(), compResult);
                    codeCache.setDefaultCode(method, compiledCode);
                } catch (Throwable e) {
                    throw debug.handle(e);
                }
            }
        }
    }

    @Override
    protected DiagnosticsOutputDirectory getDebugOutputDirectory() {
        return hotspotGraalRuntime.getOutputDirectory();
    }

    @Override
    protected Map<ExceptionAction, Integer> getCompilationProblemsPerAction() {
        return hotspotGraalRuntime.getCompilationProblemsPerAction();
    }

    private CompilationResultBuilderFactory getTruffleCallBoundaryInstrumentationFactory(String arch) {
        for (TruffleCallBoundaryInstrumentationFactory factory : GraalServices.load(TruffleCallBoundaryInstrumentationFactory.class)) {
            if (factory.getArchitecture().equals(arch)) {
                factory.init(providers.getMetaAccess(), hotspotGraalRuntime.getVMConfig(), hotspotGraalRuntime.getHostProviders().getRegisters());
                return factory;
            }
        }
        // No specialization of OptimizedCallTarget on this platform.
        return CompilationResultBuilderFactory.Default;
    }

    /**
     * Compiles a method denoted as a
     * {@linkplain HotSpotTruffleCompilerRuntime#getTruffleCallBoundaryMethods() Truffle call
     * boundary}. The compiled code has a special entry point generated by an
     * {@link TruffleCallBoundaryInstrumentationFactory}.
     */
    private CompilationResult compileTruffleCallBoundaryMethod(ResolvedJavaMethod javaMethod, CompilationIdentifier compilationId, DebugContext debug) {
        Suites newSuites = this.suites.copy();
        removeInliningPhase(newSuites);
        OptionValues options = TruffleCompilerOptions.getOptions();
        StructuredGraph graph = new StructuredGraph.Builder(options, debug, AllowAssumptions.NO).method(javaMethod).compilationId(compilationId).build();

        MetaAccessProvider metaAccess = providers.getMetaAccess();
        Plugins plugins = new Plugins(new InvocationPlugins());
        HotSpotCodeCacheProvider codeCache = (HotSpotCodeCacheProvider) providers.getCodeCache();
        boolean infoPoints = codeCache.shouldDebugNonSafepoints();
        GraphBuilderConfiguration newConfig = GraphBuilderConfiguration.getDefault(plugins).withEagerResolving(true).withUnresolvedIsError(true).withNodeSourcePosition(infoPoints);
        new GraphBuilderPhase.Instance(metaAccess, providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), newConfig, OptimisticOptimizations.ALL,
                        null).apply(graph);
        PhaseSuite<HighTierContext> graphBuilderSuite = getGraphBuilderSuite(codeCache, backend.getSuites());
        CompilationResultBuilderFactory factory = getTruffleCallBoundaryInstrumentationFactory(backend.getTarget().arch.getName());
        return compileGraph(graph, javaMethod, providers, backend, graphBuilderSuite, OptimisticOptimizations.ALL, graph.getProfilingInfo(), newSuites, lirSuites, new CompilationResult(compilationId),
                        factory, false);
    }

    private static PhaseSuite<HighTierContext> getGraphBuilderSuite(CodeCacheProvider codeCache, SuitesProvider suitesProvider) {
        PhaseSuite<HighTierContext> graphBuilderSuite = suitesProvider.getDefaultGraphBuilderSuite();
        if (codeCache.shouldDebugNonSafepoints()) {
            graphBuilderSuite = withNodeSourcePosition(graphBuilderSuite);
        }
        return graphBuilderSuite;
    }

    private static void removeInliningPhase(Suites suites) {
        ListIterator<BasePhase<? super HighTierContext>> inliningPhase = suites.getHighTier().findPhase(AbstractInliningPhase.class);
        if (inliningPhase != null) {
            inliningPhase.remove();
        }
    }

    @Override
    protected InstalledCode createInstalledCode(CompilableTruffleAST compilable) {
        return new HotSpotTruffleInstalledCode(compilable);
    }

    @Override
    protected void afterCodeInstallation(InstalledCode installedCode) {
        if (installedCode instanceof HotSpotTruffleInstalledCode) {
            HotSpotTruffleCompilerRuntime runtime = (HotSpotTruffleCompilerRuntime) TruffleCompilerRuntime.getRuntime();
            HotSpotTruffleInstalledCode hotspotTruffleInstalledCode = (HotSpotTruffleInstalledCode) installedCode;
            runtime.onCodeInstallation(hotspotTruffleInstalledCode);
        }
    }

    /**
     * {@link HotSpotNmethod#isDefault() Default} nmethods installed by Graal remain valid and can
     * still be executed once the associated {@link HotSpotNmethod} object becomes unreachable. As
     * such, these objects must remain strongly reachable from {@code OptimizedAssumption}s they
     * depend on.
     */
    @Override
    protected boolean reachabilityDeterminesValidity(InstalledCode installedCode) {
        if (installedCode instanceof HotSpotNmethod) {
            HotSpotNmethod nmethod = (HotSpotNmethod) installedCode;
            if (nmethod.isDefault()) {
                return false;
            }
        }
        return true;
    }
}
