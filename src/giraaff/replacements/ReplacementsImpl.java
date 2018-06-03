package giraaff.replacements;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.api.replacements.MethodSubstitution;
import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;
import giraaff.bytecode.ResolvedJavaMethodBytecode;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.graph.Node;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.java.GraphBuilderPhase;
import giraaff.java.GraphBuilderPhase.Instance;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext.CompilationContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.MethodSubstitutionPlugin;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.ConvertDeoptimizeToGuardPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.tiers.PhaseContext;
import giraaff.phases.util.Providers;
import giraaff.util.GraalError;
import giraaff.word.Word;
import giraaff.word.WordOperationPlugin;

// @class ReplacementsImpl
public class ReplacementsImpl implements Replacements, InlineInvokePlugin
{
    // @field
    public final Providers providers;
    // @field
    public final SnippetReflectionProvider snippetReflection;
    // @field
    public final TargetDescription target;
    // @field
    private GraphBuilderConfiguration.Plugins graphBuilderPlugins;

    /**
     * The preprocessed replacement graphs.
     */
    // @field
    protected final ConcurrentMap<ResolvedJavaMethod, StructuredGraph> graphs;

    /**
     * The default {@link BytecodeProvider} to use for accessing the bytecode of a replacement if
     * the replacement doesn't provide another {@link BytecodeProvider}.
     */
    // @field
    protected final BytecodeProvider defaultBytecodeProvider;

    public void setGraphBuilderPlugins(GraphBuilderConfiguration.Plugins __plugins)
    {
        this.graphBuilderPlugins = __plugins;
    }

    @Override
    public GraphBuilderConfiguration.Plugins getGraphBuilderPlugins()
    {
        return graphBuilderPlugins;
    }

    // @def
    private static final int MAX_GRAPH_INLINING_DEPTH = 100; // more than enough

    /**
     * Determines whether a given method should be inlined based on whether it has a substitution or
     * whether the inlining context is already within a substitution.
     *
     * @return an object specifying how {@code method} is to be inlined or null if it should not be
     *         inlined based on substitution related criteria
     */
    @Override
    public InlineInfo shouldInlineInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        Bytecode __subst = getSubstitutionBytecode(__method);
        if (__subst != null)
        {
            if (__b.parsingIntrinsic() || GraalOptions.inlineDuringParsing || GraalOptions.inlineIntrinsicsDuringParsing)
            {
                // forced inlining of intrinsics
                return InlineInfo.createIntrinsicInlineInfo(__subst.getMethod(), __method, __subst.getOrigin());
            }
            return null;
        }
        if (__b.parsingIntrinsic())
        {
            // force inlining when parsing replacements
            return InlineInfo.createIntrinsicInlineInfo(__method, null, defaultBytecodeProvider);
        }
        return null;
    }

    @Override
    public void notifyNotInlined(GraphBuilderContext __b, ResolvedJavaMethod __method, Invoke __invoke)
    {
        if (__b.parsingIntrinsic())
        {
            IntrinsicContext __intrinsic = __b.getIntrinsic();
            if (!__intrinsic.isCallToOriginal(__method))
            {
                throw new GraalError("All non-recursive calls in the intrinsic %s must be inlined or intrinsified: found call to %s", __intrinsic.getIntrinsicMethod().format("%H.%n(%p)"), __method.format("%h.%n(%p)"));
            }
        }
    }

    // @cons
    public ReplacementsImpl(Providers __providers, SnippetReflectionProvider __snippetReflection, BytecodeProvider __bytecodeProvider, TargetDescription __target)
    {
        super();
        this.providers = __providers.copyWith(this);
        this.snippetReflection = __snippetReflection;
        this.target = __target;
        this.graphs = new ConcurrentHashMap<>();
        this.defaultBytecodeProvider = __bytecodeProvider;
    }

    @Override
    public StructuredGraph getSnippet(ResolvedJavaMethod __method, Object[] __args)
    {
        return getSnippet(__method, null, __args);
    }

    @Override
    public StructuredGraph getSnippet(ResolvedJavaMethod __method, ResolvedJavaMethod __recursiveEntry, Object[] __args)
    {
        StructuredGraph __graph = GraalOptions.useSnippetGraphCache ? graphs.get(__method) : null;
        if (__graph == null)
        {
            StructuredGraph __newGraph = makeGraph(defaultBytecodeProvider, __method, __args, __recursiveEntry);
            if (!GraalOptions.useSnippetGraphCache || __args != null)
            {
                return __newGraph;
            }
            __newGraph.freeze();
            if (__graph != null)
            {
                graphs.replace(__method, __graph, __newGraph);
            }
            else
            {
                graphs.putIfAbsent(__method, __newGraph);
            }
            __graph = graphs.get(__method);
        }
        return __graph;
    }

    @Override
    public boolean hasSubstitution(ResolvedJavaMethod __method, int __invokeBci)
    {
        InvocationPlugin __plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(__method);
        return __plugin != null && (!__plugin.inlineOnly() || __invokeBci >= 0);
    }

    @Override
    public BytecodeProvider getDefaultReplacementBytecodeProvider()
    {
        return defaultBytecodeProvider;
    }

    @Override
    public Bytecode getSubstitutionBytecode(ResolvedJavaMethod __method)
    {
        InvocationPlugin __plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(__method);
        if (__plugin instanceof MethodSubstitutionPlugin)
        {
            MethodSubstitutionPlugin __msPlugin = (MethodSubstitutionPlugin) __plugin;
            ResolvedJavaMethod __substitute = __msPlugin.getSubstitute(providers.getMetaAccess());
            return __msPlugin.getBytecodeProvider().getBytecode(__substitute);
        }
        return null;
    }

    @Override
    public StructuredGraph getSubstitution(ResolvedJavaMethod __method, int __invokeBci)
    {
        StructuredGraph __result;
        InvocationPlugin __plugin = graphBuilderPlugins.getInvocationPlugins().lookupInvocation(__method);
        if (__plugin != null && (!__plugin.inlineOnly() || __invokeBci >= 0))
        {
            MetaAccessProvider __metaAccess = providers.getMetaAccess();
            if (__plugin instanceof MethodSubstitutionPlugin)
            {
                MethodSubstitutionPlugin __msPlugin = (MethodSubstitutionPlugin) __plugin;
                ResolvedJavaMethod __substitute = __msPlugin.getSubstitute(__metaAccess);
                StructuredGraph __graph = GraalOptions.useSnippetGraphCache ? graphs.get(__substitute) : null;
                if (__graph == null)
                {
                    __graph = makeGraph(__msPlugin.getBytecodeProvider(), __substitute, null, __method);
                    if (!GraalOptions.useSnippetGraphCache)
                    {
                        return __graph;
                    }
                    __graph.freeze();
                    graphs.putIfAbsent(__substitute, __graph);
                    __graph = graphs.get(__substitute);
                }
                __result = __graph;
            }
            else
            {
                Bytecode __code = new ResolvedJavaMethodBytecode(__method);
                ConstantReflectionProvider __constantReflection = providers.getConstantReflection();
                ConstantFieldProvider __constantFieldProvider = providers.getConstantFieldProvider();
                StampProvider __stampProvider = providers.getStampProvider();
                __result = new IntrinsicGraphBuilder(__metaAccess, __constantReflection, __constantFieldProvider, __stampProvider, __code, __invokeBci).buildGraph(__plugin);
            }
        }
        else
        {
            __result = null;
        }
        return __result;
    }

    /**
     * Creates a preprocessed graph for a snippet or method substitution.
     *
     * @param bytecodeProvider how to access the bytecode of {@code method}
     * @param method the snippet or method substitution for which a graph will be created
     * @param original the original method if {@code method} is a {@linkplain MethodSubstitution substitution} otherwise null
     */
    public StructuredGraph makeGraph(BytecodeProvider __bytecodeProvider, ResolvedJavaMethod __method, Object[] __args, ResolvedJavaMethod __original)
    {
        return createGraphMaker(__method, __original).makeGraph(__bytecodeProvider, __args);
    }

    /**
     * Can be overridden to return an object that specializes various parts of graph preprocessing.
     */
    protected GraphMaker createGraphMaker(ResolvedJavaMethod __substitute, ResolvedJavaMethod __original)
    {
        return new GraphMaker(this, __substitute, __original);
    }

    /**
     * Creates and preprocesses a graph for a replacement.
     */
    // @class ReplacementsImpl.GraphMaker
    public static final class GraphMaker
    {
        /**
         * The replacements object that the graphs are created for.
         */
        // @field
        protected final ReplacementsImpl replacements;

        /**
         * The method for which a graph is being created.
         */
        // @field
        protected final ResolvedJavaMethod method;

        /**
         * The original method which {@link #method} is substituting. Calls to {@link #method} or
         * {@link #substitutedMethod} will be replaced with a forced inline of {@link #substitutedMethod}.
         */
        // @field
        protected final ResolvedJavaMethod substitutedMethod;

        // @cons
        protected GraphMaker(ReplacementsImpl __replacements, ResolvedJavaMethod __substitute, ResolvedJavaMethod __substitutedMethod)
        {
            super();
            this.replacements = __replacements;
            this.method = __substitute;
            this.substitutedMethod = __substitutedMethod;
        }

        public StructuredGraph makeGraph(BytecodeProvider __bytecodeProvider, Object[] __args)
        {
            StructuredGraph __graph = buildInitialGraph(__bytecodeProvider, method, __args);

            finalizeGraph(__graph);

            return __graph;
        }

        /**
         * Does final processing of a snippet graph.
         */
        protected void finalizeGraph(StructuredGraph __graph)
        {
            new ConvertDeoptimizeToGuardPhase().apply(__graph, null);

            new DeadCodeEliminationPhase(Optionality.Required).apply(__graph);
        }

        /**
         * Filter nodes which have side effects and shouldn't be deleted from snippets when
         * converting deoptimizations to guards. Currently this only allows exception constructors
         * to be eliminated to cover the case when Java assertions are in the inlined code.
         *
         * @return true for nodes that have side effects and are unsafe to delete
         */
        private boolean hasSideEffect(Node __node)
        {
            if (__node instanceof StateSplit)
            {
                if (((StateSplit) __node).hasSideEffect())
                {
                    if (__node instanceof Invoke)
                    {
                        CallTargetNode __callTarget = ((Invoke) __node).callTarget();
                        if (__callTarget instanceof MethodCallTargetNode)
                        {
                            ResolvedJavaMethod __targetMethod = ((MethodCallTargetNode) __callTarget).targetMethod();
                            if (__targetMethod.isConstructor())
                            {
                                ResolvedJavaType __throwableType = replacements.providers.getMetaAccess().lookupJavaType(Throwable.class);
                                return !__throwableType.isAssignableFrom(__targetMethod.getDeclaringClass());
                            }
                        }
                    }
                    // not an exception constructor call
                    return true;
                }
            }
            // not a StateSplit
            return false;
        }

        /**
         * Builds the initial graph for a replacement.
         */
        protected StructuredGraph buildInitialGraph(BytecodeProvider __bytecodeProvider, final ResolvedJavaMethod __methodToParse, Object[] __args)
        {
            // replacements cannot have optimistic assumptions since they have to be valid for the entire run of the VM
            final StructuredGraph __graph = new StructuredGraph.Builder().method(__methodToParse).build();

            // replacements are not user code so they do not participate in unsafe access tracking
            __graph.disableUnsafeAccessTracking();

            MetaAccessProvider __metaAccess = replacements.providers.getMetaAccess();

            Plugins __plugins = new Plugins(replacements.graphBuilderPlugins);
            GraphBuilderConfiguration __config = GraphBuilderConfiguration.getSnippetDefault(__plugins);
            if (__args != null)
            {
                __plugins.prependParameterPlugin(new ConstantBindingParameterPlugin(__args, __metaAccess, replacements.snippetReflection));
            }

            IntrinsicContext __initialIntrinsicContext = null;
            Snippet __snippetAnnotation = method.getAnnotation(Snippet.class);
            if (__snippetAnnotation == null)
            {
                // post-parse inlined intrinsic
                __initialIntrinsicContext = new IntrinsicContext(substitutedMethod, method, __bytecodeProvider, CompilationContext.INLINE_AFTER_PARSING);
            }
            else
            {
                // snippet
                ResolvedJavaMethod __original = substitutedMethod != null ? substitutedMethod : method;
                __initialIntrinsicContext = new IntrinsicContext(__original, method, __bytecodeProvider, CompilationContext.INLINE_AFTER_PARSING, __snippetAnnotation.allowPartialIntrinsicArgumentMismatch());
            }

            createGraphBuilder(__metaAccess, replacements.providers.getStampProvider(), replacements.providers.getConstantReflection(), replacements.providers.getConstantFieldProvider(), __config, OptimisticOptimizations.NONE, __initialIntrinsicContext).apply(__graph);

            new CanonicalizerPhase().apply(__graph, new PhaseContext(replacements.providers));
            return __graph;
        }

        protected Instance createGraphBuilder(MetaAccessProvider __metaAccess, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, GraphBuilderConfiguration __graphBuilderConfig, OptimisticOptimizations __optimisticOpts, IntrinsicContext __initialIntrinsicContext)
        {
            return new GraphBuilderPhase.Instance(__metaAccess, __stampProvider, __constantReflection, __constantFieldProvider, __graphBuilderConfig, __optimisticOpts, __initialIntrinsicContext);
        }
    }
}
