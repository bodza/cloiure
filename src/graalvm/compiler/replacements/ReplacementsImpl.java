package graalvm.compiler.replacements;

import static graalvm.compiler.core.common.GraalOptions.UseSnippetGraphCache;
import static graalvm.compiler.debug.DebugContext.DEFAULT_LOG_STREAM;
import static graalvm.compiler.java.BytecodeParserOptions.InlineDuringParsing;
import static graalvm.compiler.java.BytecodeParserOptions.InlineIntrinsicsDuringParsing;
import static graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo.createIntrinsicInlineInfo;
import static graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext.CompilationContext.INLINE_AFTER_PARSING;
import static graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality.Required;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.api.replacements.SnippetTemplateCache;
import graalvm.compiler.bytecode.Bytecode;
import graalvm.compiler.bytecode.BytecodeProvider;
import graalvm.compiler.bytecode.ResolvedJavaMethodBytecode;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugContext.Description;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.TimerKey;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.java.GraphBuilderPhase.Instance;
import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin;
import graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import graalvm.compiler.nodes.graphbuilderconf.MethodSubstitutionPlugin;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.ConvertDeoptimizeToGuardPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.tiers.PhaseContext;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.word.Word;
import graalvm.compiler.word.WordOperationPlugin;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class ReplacementsImpl implements Replacements, InlineInvokePlugin {

    protected final OptionValues options;
    public final Providers providers;
    public final SnippetReflectionProvider snippetReflection;
    public final TargetDescription target;
    private GraphBuilderConfiguration.Plugins graphBuilderPlugins;
    private final DebugHandlersFactory debugHandlersFactory;

    @Override
    public OptionValues getOptions() {
        return options;
    }

    /**
     * The preprocessed replacement graphs.
     */
    protected final ConcurrentMap<ResolvedJavaMethod, StructuredGraph> graphs;

    /**
     * The default {@link BytecodeProvider} to use for accessing the bytecode of a replacement if
     * the replacement doesn't provide another {@link BytecodeProvider}.
     */
    protected final BytecodeProvider defaultBytecodeProvider;

    public void setGraphBuilderPlugins(GraphBuilderConfiguration.Plugins plugins) {
        assert this.graphBuilderPlugins == null;
        this.graphBuilderPlugins = plugins;
    }

    @Override
    public GraphBuilderConfiguration.Plugins getGraphBuilderPlugins() {
        return graphBuilderPlugins;
    }

    protected boolean hasGeneratedInvocationPluginAnnotation(ResolvedJavaMethod method) {
        return method.getAnnotation(Node.NodeIntrinsic.class) != null || method.getAnnotation(Fold.class) != null;
    }

    protected boolean hasGenericInvocationPluginAnnotation(ResolvedJavaMethod method) {
        return method.getAnnotation(Word.Operation.class) != null;
    }

    private static final int MAX_GRAPH_INLINING_DEPTH = 100; // more than enough

    /**
     * Determines whether a given method should be inlined based on whether it has a substitution or
     * whether the inlining context is already within a substitution.
     *
     * @return an object specifying how {@code method} is to be inlined or null if it should not be
     *         inlined based on substitution related criteria
     */
    @Override
    public InlineInfo shouldInlineInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args) {
        Bytecode subst = getSubstitutionBytecode(method);
        if (subst != null) {
            if (b.parsingIntrinsic() || InlineDuringParsing.getValue(b.getOptions()) || InlineIntrinsicsDuringParsing.getValue(b.getOptions())) {
                // Forced inlining of intrinsics
                return createIntrinsicInlineInfo(subst.getMethod(), method, subst.getOrigin());
            }
            return null;
        }
        if (b.parsingIntrinsic()) {
            assert b.getDepth() < MAX_GRAPH_INLINING_DEPTH : "inlining limit exceeded";

            // Force inlining when parsing replacements
            return createIntrinsicInlineInfo(method, null, defaultBytecodeProvider);
        } else {
            assert method.getAnnotation(NodeIntrinsic.class) == null : String.format("@%s method %s must only be called from within a replacement%n%s", NodeIntrinsic.class.getSimpleName(),
                            method.format("%h.%n"), b);
        }
        return null;
    }

    @Override
    public void notifyNotInlined(GraphBuilderContext b, ResolvedJavaMethod method, Invoke invoke) {
        if (b.parsingIntrinsic()) {
            IntrinsicContext intrinsic = b.getIntrinsic();
            if (!intrinsic.isCallToOriginal(method)) {
                if (hasGeneratedInvocationPluginAnnotation(method)) {
                    throw new GraalError("%s should have been handled by a %s", method.format("%H.%n(%p)"), GeneratedInvocationPlugin.class.getSimpleName());
                }
                if (hasGenericInvocationPluginAnnotation(method)) {
                    throw new GraalError("%s should have been handled by %s", method.format("%H.%n(%p)"), WordOperationPlugin.class.getSimpleName());
                }

                throw new GraalError("All non-recursive calls in the intrinsic %s must be inlined or intrinsified: found call to %s",
                                intrinsic.getIntrinsicMethod().format("%H.%n(%p)"), method.format("%h.%n(%p)"));
            }
        }
    }

    // This map is key'ed by a class name instead of a Class object so that
    // it is stable across VM executions (in support of replay compilation).
    private final EconomicMap<String, SnippetTemplateCache> snippetTemplateCache;

    public ReplacementsImpl(OptionValues options, DebugHandlersFactory debugHandlersFactory, Providers providers, SnippetReflectionProvider snippetReflection, BytecodeProvider bytecodeProvider,
                    TargetDescription target) {
        this.options = options;
        this.providers = providers.copyWith(this);
        this.snippetReflection = snippetReflection;
        this.target = target;
        this.graphs = new ConcurrentHashMap<>();
        this.snippetTemplateCache = EconomicMap.create(Equivalence.DEFAULT);
        this.defaultBytecodeProvider = bytecodeProvider;
        this.debugHandlersFactory = debugHandlersFactory;

    }

    private static final TimerKey SnippetPreparationTime = DebugContext.timer("SnippetPreparationTime");

    @Override
    public StructuredGraph getSnippet(ResolvedJavaMethod method, Object[] args, boolean trackNodeSourcePosition, NodeSourcePosition replaceePosition) {
        return getSnippet(method, null, args, trackNodeSourcePosition, replaceePosition);
    }

    private static final AtomicInteger nextDebugContextId = new AtomicInteger();

    protected DebugContext openDebugContext(String idPrefix, ResolvedJavaMethod method) {
        DebugContext outer = DebugContext.forCurrentThread();
        Description description = new Description(method, idPrefix + nextDebugContextId.incrementAndGet());
        List<DebugHandlersFactory> factories = debugHandlersFactory == null ? Collections.emptyList() : Collections.singletonList(debugHandlersFactory);
        return DebugContext.create(options, description, outer.getGlobalMetrics(), DEFAULT_LOG_STREAM, factories);
    }

    @Override
    @SuppressWarnings("try")
    public StructuredGraph getSnippet(ResolvedJavaMethod method, ResolvedJavaMethod recursiveEntry, Object[] args, boolean trackNodeSourcePosition, NodeSourcePosition replaceePosition) {
        assert method.getAnnotation(Snippet.class) != null : "Snippet must be annotated with @" + Snippet.class.getSimpleName();
        assert method.hasBytecodes() : "Snippet must not be abstract or native";

        StructuredGraph graph = UseSnippetGraphCache.getValue(options) ? graphs.get(method) : null;
        if (graph == null || (trackNodeSourcePosition && !graph.trackNodeSourcePosition())) {
            try (DebugContext debug = openDebugContext("Snippet_", method);
                            DebugCloseable a = SnippetPreparationTime.start(debug)) {
                StructuredGraph newGraph = makeGraph(debug, defaultBytecodeProvider, method, args, recursiveEntry, trackNodeSourcePosition, replaceePosition);
                DebugContext.counter("SnippetNodeCount[%#s]", method).add(newGraph.getDebug(), newGraph.getNodeCount());
                if (!UseSnippetGraphCache.getValue(options) || args != null) {
                    return newGraph;
                }
                newGraph.freeze();
                if (graph != null) {
                    graphs.replace(method, graph, newGraph);
                } else {
                    graphs.putIfAbsent(method, newGraph);
                }
                graph = graphs.get(method);
            }
        }
        assert !trackNodeSourcePosition || graph.trackNodeSourcePosition();
        return graph;
    }

    @Override
    public void registerSnippet(ResolvedJavaMethod method, boolean trackNodeSourcePosition) {
        // No initialization needed as snippet graphs are created on demand in getSnippet
    }

    @Override
    public boolean hasSubstitution(ResolvedJavaMethod method, int invokeBci) {
        InvocationPlugin plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(method);
        return plugin != null && (!plugin.inlineOnly() || invokeBci >= 0);
    }

    @Override
    public BytecodeProvider getDefaultReplacementBytecodeProvider() {
        return defaultBytecodeProvider;
    }

    @Override
    public Bytecode getSubstitutionBytecode(ResolvedJavaMethod method) {
        InvocationPlugin plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(method);
        if (plugin instanceof MethodSubstitutionPlugin) {
            MethodSubstitutionPlugin msPlugin = (MethodSubstitutionPlugin) plugin;
            ResolvedJavaMethod substitute = msPlugin.getSubstitute(providers.getMetaAccess());
            return msPlugin.getBytecodeProvider().getBytecode(substitute);
        }
        return null;
    }

    @Override
    public StructuredGraph getSubstitution(ResolvedJavaMethod method, int invokeBci, boolean trackNodeSourcePosition, NodeSourcePosition replaceePosition) {
        StructuredGraph result;
        InvocationPlugin plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(method);
        if (plugin != null && (!plugin.inlineOnly() || invokeBci >= 0)) {
            MetaAccessProvider metaAccess = providers.getMetaAccess();
            if (plugin instanceof MethodSubstitutionPlugin) {
                MethodSubstitutionPlugin msPlugin = (MethodSubstitutionPlugin) plugin;
                ResolvedJavaMethod substitute = msPlugin.getSubstitute(metaAccess);
                StructuredGraph graph = UseSnippetGraphCache.getValue(options) ? graphs.get(substitute) : null;
                if (graph == null || graph.trackNodeSourcePosition() != trackNodeSourcePosition) {
                    try (DebugContext debug = openDebugContext("Substitution_", method)) {
                        graph = makeGraph(debug, msPlugin.getBytecodeProvider(), substitute, null, method, trackNodeSourcePosition, replaceePosition);
                        if (!UseSnippetGraphCache.getValue(options)) {
                            return graph;
                        }
                        graph.freeze();
                        graphs.putIfAbsent(substitute, graph);
                        graph = graphs.get(substitute);
                    }
                }
                assert graph.isFrozen();
                result = graph;
            } else {
                Bytecode code = new ResolvedJavaMethodBytecode(method);
                ConstantReflectionProvider constantReflection = providers.getConstantReflection();
                ConstantFieldProvider constantFieldProvider = providers.getConstantFieldProvider();
                StampProvider stampProvider = providers.getStampProvider();
                try (DebugContext debug = openDebugContext("Substitution_", method)) {
                    result = new IntrinsicGraphBuilder(options, debug, metaAccess, constantReflection, constantFieldProvider, stampProvider, code, invokeBci).buildGraph(plugin);
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Creates a preprocessed graph for a snippet or method substitution.
     *
     * @param bytecodeProvider how to access the bytecode of {@code method}
     * @param method the snippet or method substitution for which a graph will be created
     * @param args
     * @param original the original method if {@code method} is a {@linkplain MethodSubstitution
     *            substitution} otherwise null
     * @param trackNodeSourcePosition
     */
    public StructuredGraph makeGraph(DebugContext debug, BytecodeProvider bytecodeProvider, ResolvedJavaMethod method, Object[] args, ResolvedJavaMethod original, boolean trackNodeSourcePosition,
                    NodeSourcePosition replaceePosition) {
        return createGraphMaker(method, original).makeGraph(debug, bytecodeProvider, args, trackNodeSourcePosition, replaceePosition);
    }

    /**
     * Can be overridden to return an object that specializes various parts of graph preprocessing.
     */
    protected GraphMaker createGraphMaker(ResolvedJavaMethod substitute, ResolvedJavaMethod original) {
        return new GraphMaker(this, substitute, original);
    }

    /**
     * Creates and preprocesses a graph for a replacement.
     */
    public static class GraphMaker {

        /** The replacements object that the graphs are created for. */
        protected final ReplacementsImpl replacements;

        /**
         * The method for which a graph is being created.
         */
        protected final ResolvedJavaMethod method;

        /**
         * The original method which {@link #method} is substituting. Calls to {@link #method} or
         * {@link #substitutedMethod} will be replaced with a forced inline of
         * {@link #substitutedMethod}.
         */
        protected final ResolvedJavaMethod substitutedMethod;

        protected GraphMaker(ReplacementsImpl replacements, ResolvedJavaMethod substitute, ResolvedJavaMethod substitutedMethod) {
            this.replacements = replacements;
            this.method = substitute;
            this.substitutedMethod = substitutedMethod;
        }

        @SuppressWarnings("try")
        public StructuredGraph makeGraph(DebugContext debug, BytecodeProvider bytecodeProvider, Object[] args, boolean trackNodeSourcePosition, NodeSourcePosition replaceePosition) {
            try (DebugContext.Scope s = debug.scope("BuildSnippetGraph", method)) {
                assert method.hasBytecodes() : method;
                StructuredGraph graph = buildInitialGraph(debug, bytecodeProvider, method, args, trackNodeSourcePosition, replaceePosition);

                finalizeGraph(graph);

                debug.dump(DebugContext.INFO_LEVEL, graph, "%s: Final", method.getName());

                return graph;
            } catch (Throwable e) {
                throw debug.handle(e);
            }
        }

        /**
         * Does final processing of a snippet graph.
         */
        protected void finalizeGraph(StructuredGraph graph) {
            if (!GraalOptions.SnippetCounters.getValue(replacements.options) || graph.getNodes().filter(SnippetCounterNode.class).isEmpty()) {
                int sideEffectCount = 0;
                assert (sideEffectCount = graph.getNodes().filter(e -> hasSideEffect(e)).count()) >= 0;
                new ConvertDeoptimizeToGuardPhase().apply(graph, null);
                assert sideEffectCount == graph.getNodes().filter(e -> hasSideEffect(e)).count() : "deleted side effecting node";

                new DeadCodeEliminationPhase(Required).apply(graph);
            } else {
                // ConvertDeoptimizeToGuardPhase will eliminate snippet counters on paths
                // that terminate in a deopt so we disable it if the graph contains
                // snippet counters. The trade off is that we miss out on guard
                // coalescing opportunities.
            }
        }

        /**
         * Filter nodes which have side effects and shouldn't be deleted from snippets when
         * converting deoptimizations to guards. Currently this only allows exception constructors
         * to be eliminated to cover the case when Java assertions are in the inlined code.
         *
         * @param node
         * @return true for nodes that have side effects and are unsafe to delete
         */
        private boolean hasSideEffect(Node node) {
            if (node instanceof StateSplit) {
                if (((StateSplit) node).hasSideEffect()) {
                    if (node instanceof Invoke) {
                        CallTargetNode callTarget = ((Invoke) node).callTarget();
                        if (callTarget instanceof MethodCallTargetNode) {
                            ResolvedJavaMethod targetMethod = ((MethodCallTargetNode) callTarget).targetMethod();
                            if (targetMethod.isConstructor()) {
                                ResolvedJavaType throwableType = replacements.providers.getMetaAccess().lookupJavaType(Throwable.class);
                                return !throwableType.isAssignableFrom(targetMethod.getDeclaringClass());
                            }
                        }
                    }
                    // Not an exception constructor call
                    return true;
                }
            }
            // Not a StateSplit
            return false;
        }

        /**
         * Builds the initial graph for a replacement.
         */
        @SuppressWarnings("try")
        protected StructuredGraph buildInitialGraph(DebugContext debug, BytecodeProvider bytecodeProvider, final ResolvedJavaMethod methodToParse, Object[] args, boolean trackNodeSourcePosition,
                        NodeSourcePosition replaceePosition) {
            // Replacements cannot have optimistic assumptions since they have
            // to be valid for the entire run of the VM.
            final StructuredGraph graph = new StructuredGraph.Builder(replacements.options, debug).method(methodToParse).trackNodeSourcePosition(trackNodeSourcePosition).callerContext(
                            replaceePosition).build();

            // Replacements are not user code so they do not participate in unsafe access
            // tracking
            graph.disableUnsafeAccessTracking();

            try (DebugContext.Scope s = debug.scope("buildInitialGraph", graph)) {
                MetaAccessProvider metaAccess = replacements.providers.getMetaAccess();

                Plugins plugins = new Plugins(replacements.graphBuilderPlugins);
                GraphBuilderConfiguration config = GraphBuilderConfiguration.getSnippetDefault(plugins);
                if (args != null) {
                    plugins.prependParameterPlugin(new ConstantBindingParameterPlugin(args, metaAccess, replacements.snippetReflection));
                }

                IntrinsicContext initialIntrinsicContext = null;
                Snippet snippetAnnotation = method.getAnnotation(Snippet.class);
                if (snippetAnnotation == null) {
                    // Post-parse inlined intrinsic
                    initialIntrinsicContext = new IntrinsicContext(substitutedMethod, method, bytecodeProvider, INLINE_AFTER_PARSING);
                } else {
                    // Snippet
                    ResolvedJavaMethod original = substitutedMethod != null ? substitutedMethod : method;
                    initialIntrinsicContext = new IntrinsicContext(original, method, bytecodeProvider, INLINE_AFTER_PARSING, snippetAnnotation.allowPartialIntrinsicArgumentMismatch());
                }

                createGraphBuilder(metaAccess, replacements.providers.getStampProvider(), replacements.providers.getConstantReflection(), replacements.providers.getConstantFieldProvider(), config,
                                OptimisticOptimizations.NONE, initialIntrinsicContext).apply(graph);

                new CanonicalizerPhase().apply(graph, new PhaseContext(replacements.providers));
            } catch (Throwable e) {
                throw debug.handle(e);
            }
            return graph;
        }

        protected Instance createGraphBuilder(MetaAccessProvider metaAccess, StampProvider stampProvider, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider,
                        GraphBuilderConfiguration graphBuilderConfig, OptimisticOptimizations optimisticOpts, IntrinsicContext initialIntrinsicContext) {
            return new GraphBuilderPhase.Instance(metaAccess, stampProvider, constantReflection, constantFieldProvider, graphBuilderConfig, optimisticOpts,
                            initialIntrinsicContext);
        }
    }

    @Override
    public void registerSnippetTemplateCache(SnippetTemplateCache templates) {
        assert snippetTemplateCache.get(templates.getClass().getName()) == null;
        snippetTemplateCache.put(templates.getClass().getName(), templates);
    }

    @Override
    public <T extends SnippetTemplateCache> T getSnippetTemplateCache(Class<T> templatesClass) {
        SnippetTemplateCache ret = snippetTemplateCache.get(templatesClass.getName());
        return templatesClass.cast(ret);
    }
}
