package giraaff.hotspot.replacements.aot;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;

import giraaff.api.replacements.Snippet;
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
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.util.GraalError;

// @class ResolveConstantSnippets
public final class ResolveConstantSnippets implements Snippets
{
    // @cons
    private ResolveConstantSnippets()
    {
        super();
    }

    @Snippet
    public static Object resolveObjectConstant(Object __constant)
    {
        Object __result = LoadConstantIndirectlyNode.loadObject(__constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result == null))
        {
            __result = ResolveConstantStubCall.resolveObject(__constant, EncodedSymbolNode.encode(__constant));
        }
        return __result;
    }

    @Snippet
    public static Object resolveDynamicConstant(Object __constant)
    {
        Object __result = LoadConstantIndirectlyNode.loadObject(__constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result == null))
        {
            __result = ResolveDynamicStubCall.resolveInvoke(__constant);
        }
        return __result;
    }

    @Snippet
    public static KlassPointer resolveKlassConstant(KlassPointer __constant)
    {
        KlassPointer __result = LoadConstantIndirectlyNode.loadKlass(__constant);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result.isNull()))
        {
            __result = ResolveConstantStubCall.resolveKlass(__constant, EncodedSymbolNode.encode(__constant));
        }
        return __result;
    }

    @Snippet
    public static MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer __method, KlassPointer __klassHint)
    {
        MethodCountersPointer __result = LoadMethodCountersIndirectlyNode.loadMethodCounters(__method);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result.isNull()))
        {
            __result = ResolveMethodAndLoadCountersStubCall.resolveMethodAndLoadCounters(__method, __klassHint, EncodedSymbolNode.encode(__method));
        }
        return __result;
    }

    @Snippet
    public static KlassPointer initializeKlass(KlassPointer __constant)
    {
        KlassPointer __result = LoadConstantIndirectlyNode.loadKlass(__constant, HotSpotConstantLoadAction.INITIALIZE);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result.isNull()))
        {
            __result = InitializeKlassStubCall.initializeKlass(__constant, EncodedSymbolNode.encode(__constant));
        }
        return __result;
    }

    @Snippet
    public static KlassPointer pureInitializeKlass(KlassPointer __constant)
    {
        KlassPointer __result = LoadConstantIndirectlyNode.loadKlass(__constant, HotSpotConstantLoadAction.INITIALIZE);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.VERY_SLOW_PATH_PROBABILITY, __result.isNull()))
        {
            __result = ResolveConstantStubCall.resolveKlass(__constant, EncodedSymbolNode.encode(__constant), HotSpotConstantLoadAction.INITIALIZE);
        }
        return __result;
    }

    // @class ResolveConstantSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo ___resolveObjectConstant = snippet(ResolveConstantSnippets.class, "resolveObjectConstant");
        // @field
        private final SnippetInfo ___resolveDynamicConstant = snippet(ResolveConstantSnippets.class, "resolveDynamicConstant");
        // @field
        private final SnippetInfo ___resolveKlassConstant = snippet(ResolveConstantSnippets.class, "resolveKlassConstant");
        // @field
        private final SnippetInfo ___resolveMethodAndLoadCounters = snippet(ResolveConstantSnippets.class, "resolveMethodAndLoadCounters");
        // @field
        private final SnippetInfo ___initializeKlass = snippet(ResolveConstantSnippets.class, "initializeKlass");
        // @field
        private final SnippetInfo ___pureInitializeKlass = snippet(ResolveConstantSnippets.class, "pureInitializeKlass");

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        public void lower(ResolveDynamicConstantNode __resolveConstantNode, LoweringTool __tool)
        {
            StructuredGraph __graph = __resolveConstantNode.graph();

            ValueNode __value = __resolveConstantNode.value();
            SnippetInfo __snippet = this.___resolveDynamicConstant;

            Arguments __args = new Arguments(__snippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("constant", __value);

            SnippetTemplate __template = template(__resolveConstantNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __resolveConstantNode, SnippetTemplate.DEFAULT_REPLACER, __args);

            if (!__resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(__resolveConstantNode);
            }
        }

        public void lower(ResolveConstantNode __resolveConstantNode, LoweringTool __tool)
        {
            StructuredGraph __graph = __resolveConstantNode.graph();

            ValueNode __value = __resolveConstantNode.value();
            Constant __constant = __value.asConstant();
            SnippetInfo __snippet = null;

            if (__constant instanceof HotSpotMetaspaceConstant)
            {
                HotSpotMetaspaceConstant __hotspotMetaspaceConstant = (HotSpotMetaspaceConstant) __constant;
                if (__hotspotMetaspaceConstant.asResolvedJavaType() != null)
                {
                    if (__resolveConstantNode.action() == HotSpotConstantLoadAction.RESOLVE)
                    {
                        __snippet = this.___resolveKlassConstant;
                    }
                    else
                    {
                        __snippet = this.___pureInitializeKlass;
                    }
                }
            }
            else if (__constant instanceof HotSpotObjectConstant)
            {
                __snippet = this.___resolveObjectConstant;
                HotSpotObjectConstant __hotspotObjectConstant = (HotSpotObjectConstant) __constant;
            }
            if (__snippet == null)
            {
                throw new GraalError("unsupported constant type: " + __constant);
            }

            Arguments __args = new Arguments(__snippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("constant", __value);

            SnippetTemplate __template = template(__resolveConstantNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __resolveConstantNode, SnippetTemplate.DEFAULT_REPLACER, __args);

            if (!__resolveConstantNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(__resolveConstantNode);
            }
        }

        public void lower(InitializeKlassNode __initializeKlassNode, LoweringTool __tool)
        {
            StructuredGraph __graph = __initializeKlassNode.graph();

            ValueNode __value = __initializeKlassNode.value();
            Constant __constant = __value.asConstant();

            if (__constant instanceof HotSpotMetaspaceConstant)
            {
                Arguments __args = new Arguments(this.___initializeKlass, __graph.getGuardsStage(), __tool.getLoweringStage());
                __args.add("constant", __value);

                SnippetTemplate __template = template(__initializeKlassNode, __args);
                __template.instantiate(this.___providers.getMetaAccess(), __initializeKlassNode, SnippetTemplate.DEFAULT_REPLACER, __args);
                if (!__initializeKlassNode.isDeleted())
                {
                    GraphUtil.killWithUnusedFloatingInputs(__initializeKlassNode);
                }
            }
            else
            {
                throw new GraalError("unsupported constant type: " + __constant);
            }
        }

        public void lower(ResolveMethodAndLoadCountersNode __resolveMethodAndLoadCountersNode, LoweringTool __tool)
        {
            StructuredGraph __graph = __resolveMethodAndLoadCountersNode.graph();
            ConstantNode __method = ConstantNode.forConstant(MethodPointerStamp.methodNonNull(), __resolveMethodAndLoadCountersNode.getMethod().getEncoding(), __tool.getMetaAccess(), __graph);
            Arguments __args = new Arguments(this.___resolveMethodAndLoadCounters, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("method", __method);
            __args.add("klassHint", __resolveMethodAndLoadCountersNode.getHub());
            SnippetTemplate __template = template(__resolveMethodAndLoadCountersNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __resolveMethodAndLoadCountersNode, SnippetTemplate.DEFAULT_REPLACER, __args);

            if (!__resolveMethodAndLoadCountersNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(__resolveMethodAndLoadCountersNode);
            }
        }
    }
}
