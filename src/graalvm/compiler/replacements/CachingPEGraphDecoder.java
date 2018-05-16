package graalvm.compiler.replacements;

import static graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext.CompilationContext.INLINE_AFTER_PARSING;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.bytecode.BytecodeProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.SourceLanguagePositionProvider;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.nodes.EncodedGraph;
import graalvm.compiler.nodes.GraphEncoder;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin;
import graalvm.compiler.nodes.graphbuilderconf.IntrinsicContext;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import graalvm.compiler.nodes.graphbuilderconf.LoopExplosionPlugin;
import graalvm.compiler.nodes.graphbuilderconf.NodePlugin;
import graalvm.compiler.nodes.graphbuilderconf.ParameterPlugin;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.ConvertDeoptimizeToGuardPhase;
import graalvm.compiler.phases.tiers.PhaseContext;
import graalvm.compiler.phases.util.Providers;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * A graph decoder that provides all necessary encoded graphs on-the-fly (by parsing the methods and
 * encoding the graphs).
 */
public class CachingPEGraphDecoder extends PEGraphDecoder
{
    protected final Providers providers;
    protected final GraphBuilderConfiguration graphBuilderConfig;
    protected final OptimisticOptimizations optimisticOpts;
    private final AllowAssumptions allowAssumptions;
    private final EconomicMap<ResolvedJavaMethod, EncodedGraph> graphCache;

    public CachingPEGraphDecoder(Architecture architecture, StructuredGraph graph, Providers providers, GraphBuilderConfiguration graphBuilderConfig, OptimisticOptimizations optimisticOpts, AllowAssumptions allowAssumptions, LoopExplosionPlugin loopExplosionPlugin, InvocationPlugins invocationPlugins, InlineInvokePlugin[] inlineInvokePlugins, ParameterPlugin parameterPlugin, NodePlugin[] nodePlugins, ResolvedJavaMethod callInlinedMethod, SourceLanguagePositionProvider sourceLanguagePositionProvider)
    {
        super(architecture, graph, providers.getMetaAccess(), providers.getConstantReflection(), providers.getConstantFieldProvider(), providers.getStampProvider(), loopExplosionPlugin, invocationPlugins, inlineInvokePlugins, parameterPlugin, nodePlugins, callInlinedMethod, sourceLanguagePositionProvider);

        this.providers = providers;
        this.graphBuilderConfig = graphBuilderConfig;
        this.optimisticOpts = optimisticOpts;
        this.allowAssumptions = allowAssumptions;
        this.graphCache = EconomicMap.create();
    }

    protected GraphBuilderPhase.Instance createGraphBuilderPhaseInstance(IntrinsicContext initialIntrinsicContext)
    {
        return new GraphBuilderPhase.Instance(providers.getMetaAccess(), providers.getStampProvider(), providers.getConstantReflection(), providers.getConstantFieldProvider(), graphBuilderConfig, optimisticOpts, initialIntrinsicContext);
    }

    @SuppressWarnings("try")
    private EncodedGraph createGraph(ResolvedJavaMethod method, ResolvedJavaMethod originalMethod, BytecodeProvider intrinsicBytecodeProvider)
    {
        StructuredGraph graphToEncode = new StructuredGraph.Builder(options, debug, allowAssumptions).useProfilingInfo(false).trackNodeSourcePosition(graphBuilderConfig.trackNodeSourcePosition()).method(method).build();
        try (DebugContext.Scope scope = debug.scope("createGraph", graphToEncode))
        {
            IntrinsicContext initialIntrinsicContext = intrinsicBytecodeProvider != null ? new IntrinsicContext(originalMethod, method, intrinsicBytecodeProvider, INLINE_AFTER_PARSING) : null;
            GraphBuilderPhase.Instance graphBuilderPhaseInstance = createGraphBuilderPhaseInstance(initialIntrinsicContext);
            graphBuilderPhaseInstance.apply(graphToEncode);

            PhaseContext context = new PhaseContext(providers);
            new CanonicalizerPhase().apply(graphToEncode, context);
            /*
             * ConvertDeoptimizeToGuardPhase reduces the number of merges in the graph, so that
             * fewer frame states will be created. This significantly reduces the number of nodes in
             * the initial graph.
             */
            new ConvertDeoptimizeToGuardPhase().apply(graphToEncode, context);

            EncodedGraph encodedGraph = GraphEncoder.encodeSingleGraph(graphToEncode, architecture);
            graphCache.put(method, encodedGraph);
            return encodedGraph;
        }
        catch (Throwable ex)
        {
            throw debug.handle(ex);
        }
    }

    @Override
    protected EncodedGraph lookupEncodedGraph(ResolvedJavaMethod method, ResolvedJavaMethod originalMethod, BytecodeProvider intrinsicBytecodeProvider, boolean trackNodeSourcePosition)
    {
        EncodedGraph result = graphCache.get(method);
        if (result == null && method.hasBytecodes())
        {
            result = createGraph(method, originalMethod, intrinsicBytecodeProvider);
        }
        return result;
    }
}
