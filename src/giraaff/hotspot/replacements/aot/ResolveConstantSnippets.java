package giraaff.hotspot.replacements.aot;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;

import giraaff.api.replacements.Snippet;
import giraaff.debug.GraalError;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.aot.EncodedSymbolNode;
import giraaff.hotspot.nodes.aot.InitializeKlassNode;
import giraaff.hotspot.nodes.aot.InitializeKlassStubCall;
import giraaff.hotspot.nodes.aot.LoadConstantIndirectlyNode;
import giraaff.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode;
import giraaff.hotspot.nodes.aot.ResolveConstantNode;
import giraaff.hotspot.nodes.aot.ResolveConstantStubCall;
import giraaff.hotspot.nodes.aot.ResolveDynamicConstantNode;
import giraaff.hotspot.nodes.aot.ResolveDynamicStubCall;
import giraaff.hotspot.nodes.aot.ResolveMethodAndLoadCountersNode;
import giraaff.hotspot.nodes.aot.ResolveMethodAndLoadCountersStubCall;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.hotspot.word.KlassPointer;
import giraaff.hotspot.word.MethodCountersPointer;
import giraaff.hotspot.word.MethodPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;

public class ResolveConstantSnippets implements Snippets
{
    @Snippet
    public static Object resolveObjectConstant(Object constant)
    {
        Object result = LoadConstantIndirectlyNode.loadObject(constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result == null))
        {
            result = ResolveConstantStubCall.resolveObject(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static Object resolveDynamicConstant(Object constant)
    {
        Object result = LoadConstantIndirectlyNode.loadObject(constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result == null))
        {
            result = ResolveDynamicStubCall.resolveInvoke(constant);
        }
        return result;
    }

    @Snippet
    public static KlassPointer resolveKlassConstant(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = ResolveConstantStubCall.resolveKlass(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer method, KlassPointer klassHint)
    {
        MethodCountersPointer result = LoadMethodCountersIndirectlyNode.loadMethodCounters(method);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = ResolveMethodAndLoadCountersStubCall.resolveMethodAndLoadCounters(method, klassHint, EncodedSymbolNode.encode(method));
        }
        return result;
    }

    @Snippet
    public static KlassPointer initializeKlass(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant, HotSpotConstantLoadAction.INITIALIZE);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result.isNull()))
        {
            result = InitializeKlassStubCall.initializeKlass(constant, EncodedSymbolNode.encode(constant));
        }
        return result;
    }

    @Snippet
    public static KlassPointer pureInitializeKlass(KlassPointer constant)
    {
        KlassPointer result = LoadConstantIndirectlyNode.loadKlass(constant, HotSpotConstantLoadAction.INITIALIZE);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, result.isNull()))
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

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        public void lower(ResolveDynamicConstantNode resolveConstantNode, LoweringTool tool)
        {
            StructuredGraph graph = resolveConstantNode.graph();

            ValueNode value = resolveConstantNode.value();
            SnippetInfo snippet = resolveDynamicConstant;

            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("constant", value);

            SnippetTemplate template = template(resolveConstantNode, args);
            template.instantiate(providers.getMetaAccess(), resolveConstantNode, SnippetTemplate.DEFAULT_REPLACER, args);

            if (!resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveConstantNode);
            }
        }

        public void lower(ResolveConstantNode resolveConstantNode, LoweringTool tool)
        {
            StructuredGraph graph = resolveConstantNode.graph();

            ValueNode value = resolveConstantNode.value();
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
                        snippet = pureInitializeKlass;
                    }
                }
            }
            else if (constant instanceof HotSpotObjectConstant)
            {
                snippet = resolveObjectConstant;
                HotSpotObjectConstant hotspotObjectConstant = (HotSpotObjectConstant) constant;
            }
            if (snippet == null)
            {
                throw new GraalError("Unsupported constant type: " + constant);
            }

            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("constant", value);

            SnippetTemplate template = template(resolveConstantNode, args);
            template.instantiate(providers.getMetaAccess(), resolveConstantNode, SnippetTemplate.DEFAULT_REPLACER, args);

            if (!resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveConstantNode);
            }
        }

        public void lower(InitializeKlassNode initializeKlassNode, LoweringTool tool)
        {
            StructuredGraph graph = initializeKlassNode.graph();

            ValueNode value = initializeKlassNode.value();
            Constant constant = value.asConstant();

            if (constant instanceof HotSpotMetaspaceConstant)
            {
                Arguments args = new Arguments(initializeKlass, graph.getGuardsStage(), tool.getLoweringStage());
                args.add("constant", value);

                SnippetTemplate template = template(initializeKlassNode, args);
                template.instantiate(providers.getMetaAccess(), initializeKlassNode, SnippetTemplate.DEFAULT_REPLACER, args);
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
            template.instantiate(providers.getMetaAccess(), resolveMethodAndLoadCountersNode, SnippetTemplate.DEFAULT_REPLACER, args);

            if (!resolveMethodAndLoadCountersNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(resolveMethodAndLoadCountersNode);
            }
        }
    }
}
