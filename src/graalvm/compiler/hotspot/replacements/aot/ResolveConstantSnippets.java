package graalvm.compiler.hotspot.replacements.aot;

import static graalvm.compiler.nodes.extended.BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.probability;
import static graalvm.compiler.replacements.SnippetTemplate.DEFAULT_REPLACER;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.aot.EncodedSymbolNode;
import graalvm.compiler.hotspot.nodes.aot.InitializeKlassNode;
import graalvm.compiler.hotspot.nodes.aot.InitializeKlassStubCall;
import graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode;
import graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode;
import graalvm.compiler.hotspot.nodes.aot.ResolveConstantNode;
import graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall;
import graalvm.compiler.hotspot.nodes.aot.ResolveDynamicConstantNode;
import graalvm.compiler.hotspot.nodes.aot.ResolveDynamicStubCall;
import graalvm.compiler.hotspot.nodes.aot.ResolveMethodAndLoadCountersNode;
import graalvm.compiler.hotspot.nodes.aot.ResolveMethodAndLoadCountersStubCall;
import graalvm.compiler.hotspot.nodes.type.MethodPointerStamp;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.hotspot.word.MethodCountersPointer;
import graalvm.compiler.hotspot.word.MethodPointer;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;

public class ResolveConstantSnippets implements Snippets
{
    @Snippet
    public static Object resolveObjectConstant(Object constant)
    {
        Object result = LoadConstantIndirectlyNode.loadObject(constant);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result == null))
        {
            result = ResolveConstantStubCall.resolveObject(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static Object resolveDynamicConstant(Object constant)
    {
        Object result = LoadConstantIndirectlyNode.loadObject(constant);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result == null))
        {
            result = ResolveDynamicStubCall.resolveInvoke(constant);
        }
        return result;
    }

    @Snippet
    public static KlassPointer resolveKlassConstant(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = ResolveConstantStubCall.resolveKlass(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer method, KlassPointer klassHint)
    {
        MethodCountersPointer result = LoadMethodCountersIndirectlyNode.loadMethodCounters(method);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = ResolveMethodAndLoadCountersStubCall.resolveMethodAndLoadCounters(method, klassHint, EncodedSymbolNode.encode(method));
        }
        return result;
    }

    @Snippet
    public static KlassPointer initializeKlass(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant, HotSpotConstantLoadAction.INITIALIZE);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = InitializeKlassStubCall.initializeKlass(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static KlassPointer pureInitializeKlass(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant, HotSpotConstantLoadAction.INITIALIZE);
        if (probability(VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = ResolveConstantStubCall.resolveKlass(constant, EncodedSymbolNode.encode(constant), HotSpotConstantLoadAction.INITIALIZE);
        }
        return result;
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo resolveObjectConstant = snippet(ResolveConstantSnippets.class, "resolveObjectConstant");
        private final SnippetInfo resolveDynamicConstant = snippet(ResolveConstantSnippets.class, "resolveDynamicConstant");
        private final SnippetInfo resolveKlassConstant = snippet(ResolveConstantSnippets.class, "resolveKlassConstant");
        private final SnippetInfo resolveMethodAndLoadCounters = snippet(ResolveConstantSnippets.class, "resolveMethodAndLoadCounters");
        private final SnippetInfo initializeKlass = snippet(ResolveConstantSnippets.class, "initializeKlass");
        private final SnippetInfo pureInitializeKlass = snippet(ResolveConstantSnippets.class, "pureInitializeKlass");

        public Templates(OptionValues options, Iterable<DebugHandlersFactory> factories, HotSpotProviders providers, TargetDescription target)
        {
            super(options, factories, providers, providers.getSnippetReflection(), target);
        }

        public void lower(ResolveDynamicConstantNode resolveConstantNode, LoweringTool tool)
        {
            StructuredGraph graph = resolveConstantNode.graph();

            ValueNode value = resolveConstantNode.value();
            assert value.isConstant() : "Expected a constant: " + value;
            SnippetInfo snippet = resolveDynamicConstant;

            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("constant", value);

            SnippetTemplate template = template(resolveConstantNode, args);
            template.instantiate(providers.getMetaAccess(), resolveConstantNode, DEFAULT_REPLACER, args);

            assert resolveConstantNode.hasNoUsages();
            if (!resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveConstantNode);
            }
        }

        public void lower(ResolveConstantNode resolveConstantNode, LoweringTool tool)
        {
            StructuredGraph graph = resolveConstantNode.graph();

            ValueNode value = resolveConstantNode.value();
            assert value.isConstant() : "Expected a constant: " + value;
            Constant constant = value.asConstant();
            SnippetInfo snippet = null;

            if (constant instanceof HotSpotMetaspaceConstant)
            {
                HotSpotMetaspaceConstant hotspotMetaspaceConstant = (HotSpotMetaspaceConstant) constant;
                if (hotspotMetaspaceConstant.asResolvedJavaType() != null)
                {
                    if (resolveConstantNode.action() == HotSpotConstantLoadAction.RESOLVE)
                    {
                        snippet = resolveKlassConstant;
                    }
                    else
                    {
                        assert resolveConstantNode.action() == HotSpotConstantLoadAction.INITIALIZE;
                        snippet = pureInitializeKlass;
                    }
                }
            }
            else if (constant instanceof HotSpotObjectConstant)
            {
                snippet = resolveObjectConstant;
                HotSpotObjectConstant hotspotObjectConstant = (HotSpotObjectConstant) constant;
                assert hotspotObjectConstant.isInternedString();
            }
            if (snippet == null)
            {
                throw new GraalError("Unsupported constant type: " + constant);
            }

            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("constant", value);

            SnippetTemplate template = template(resolveConstantNode, args);
            template.instantiate(providers.getMetaAccess(), resolveConstantNode, DEFAULT_REPLACER, args);

            assert resolveConstantNode.hasNoUsages();
            if (!resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveConstantNode);
            }
        }

        public void lower(InitializeKlassNode initializeKlassNode, LoweringTool tool)
        {
            StructuredGraph graph = initializeKlassNode.graph();

            ValueNode value = initializeKlassNode.value();
            assert value.isConstant() : "Expected a constant: " + value;
            Constant constant = value.asConstant();

            if (constant instanceof HotSpotMetaspaceConstant)
            {
                Arguments args = new Arguments(initializeKlass, graph.getGuardsStage(), tool.getLoweringStage());
                args.add("constant", value);

                SnippetTemplate template = template(initializeKlassNode, args);
                template.instantiate(providers.getMetaAccess(), initializeKlassNode, DEFAULT_REPLACER, args);
                assert initializeKlassNode.hasNoUsages();
                if (!initializeKlassNode.isDeleted())
                {
                    GraphUtil.killWithUnusedFloatingInputs(initializeKlassNode);
                }
            }
            else
            {
                throw new GraalError("Unsupported constant type: " + constant);
            }
        }

        public void lower(ResolveMethodAndLoadCountersNode resolveMethodAndLoadCountersNode, LoweringTool tool)
        {
            StructuredGraph graph = resolveMethodAndLoadCountersNode.graph();
            ConstantNode method = ConstantNode.forConstant(MethodPointerStamp.methodNonNull(), resolveMethodAndLoadCountersNode.getMethod().getEncoding(), tool.getMetaAccess(), graph);
            Arguments args = new Arguments(resolveMethodAndLoadCounters, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("method", method);
            args.add("klassHint", resolveMethodAndLoadCountersNode.getHub());
            SnippetTemplate template = template(resolveMethodAndLoadCountersNode, args);
            template.instantiate(providers.getMetaAccess(), resolveMethodAndLoadCountersNode, DEFAULT_REPLACER, args);

            assert resolveMethodAndLoadCountersNode.hasNoUsages();
            if (!resolveMethodAndLoadCountersNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveMethodAndLoadCountersNode);
            }
        }
    }
}
