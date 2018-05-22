package giraaff.hotspot.replacements.profiling;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.debug.GraalError;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.aot.LoadMethodCountersNode;
import giraaff.hotspot.nodes.profiling.ProfileBranchNode;
import giraaff.hotspot.nodes.profiling.ProfileInvokeNode;
import giraaff.hotspot.nodes.profiling.ProfileNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.MethodCountersPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;

public class ProfileSnippets implements Snippets
{
    @NodeIntrinsic(ForeignCallNode.class)
    public static native void methodInvocationEvent(@ConstantNodeParameter ForeignCallDescriptor descriptor, MethodCountersPointer counters);

    @Snippet
    protected static int notificationMask(int freqLog, int stepLog)
    {
        int stepMask = (1 << stepLog) - 1;
        int frequencyMask = (1 << freqLog) - 1;
        return frequencyMask & ~stepMask;
    }

    @Snippet
    public static void profileMethodEntry(MethodCountersPointer counters, int step, int stepLog, @ConstantParameter int freqLog)
    {
        int counterValue = counters.readInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterOffset) + HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterIncrement * step;
        counters.writeInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterOffset, counterValue);
        if (freqLog >= 0)
        {
            final int mask = notificationMask(freqLog, stepLog);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, (counterValue & (mask << HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterShift)) == 0))
            {
                methodInvocationEvent(HotSpotBackend.INVOCATION_EVENT, counters);
            }
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void methodBackedgeEvent(@ConstantNodeParameter ForeignCallDescriptor descriptor, MethodCountersPointer counters, int bci, int targetBci);

    @Snippet
    public static void profileBackedge(MethodCountersPointer counters, int step, int stepLog, @ConstantParameter int freqLog, int bci, int targetBci)
    {
        int counterValue = counters.readInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).backedgeCounterOffset) + HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterIncrement * step;
        counters.writeInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).backedgeCounterOffset, counterValue);
        final int mask = notificationMask(freqLog, stepLog);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, (counterValue & (mask << HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterShift)) == 0))
        {
            methodBackedgeEvent(HotSpotBackend.BACKEDGE_EVENT, counters, bci, targetBci);
        }
    }

    @Snippet
    public static void profileConditionalBackedge(MethodCountersPointer counters, int step, int stepLog, @ConstantParameter int freqLog, boolean branchCondition, int bci, int targetBci)
    {
        if (branchCondition)
        {
            profileBackedge(counters, step, stepLog, freqLog, bci, targetBci);
        }
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo profileMethodEntry = snippet(ProfileSnippets.class, "profileMethodEntry");
        private final SnippetInfo profileBackedge = snippet(ProfileSnippets.class, "profileBackedge");
        private final SnippetInfo profileConditionalBackedge = snippet(ProfileSnippets.class, "profileConditionalBackedge");

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        public void lower(ProfileNode profileNode, LoweringTool tool)
        {
            StructuredGraph graph = profileNode.graph();
            LoadMethodCountersNode counters = graph.unique(new LoadMethodCountersNode(profileNode.getProfiledMethod()));
            ConstantNode step = ConstantNode.forInt(profileNode.getStep(), graph);
            ConstantNode stepLog = ConstantNode.forInt(CodeUtil.log2(profileNode.getStep()), graph);

            if (profileNode instanceof ProfileBranchNode)
            {
                // Backedge event
                ProfileBranchNode profileBranchNode = (ProfileBranchNode) profileNode;
                SnippetInfo snippet = profileBranchNode.hasCondition() ? profileConditionalBackedge : profileBackedge;
                Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
                ConstantNode bci = ConstantNode.forInt(profileBranchNode.bci(), graph);
                ConstantNode targetBci = ConstantNode.forInt(profileBranchNode.targetBci(), graph);
                args.add("counters", counters);
                args.add("step", step);
                args.add("stepLog", stepLog);
                args.addConst("freqLog", profileBranchNode.getNotificationFreqLog());
                if (profileBranchNode.hasCondition())
                {
                    args.add("branchCondition", profileBranchNode.branchCondition());
                }
                args.add("bci", bci);
                args.add("targetBci", targetBci);

                SnippetTemplate template = template(profileNode, args);
                template.instantiate(providers.getMetaAccess(), profileNode, SnippetTemplate.DEFAULT_REPLACER, args);
            }
            else if (profileNode instanceof ProfileInvokeNode)
            {
                ProfileInvokeNode profileInvokeNode = (ProfileInvokeNode) profileNode;
                // Method invocation event
                Arguments args = new Arguments(profileMethodEntry, graph.getGuardsStage(), tool.getLoweringStage());
                args.add("counters", counters);
                args.add("step", step);
                args.add("stepLog", stepLog);
                args.addConst("freqLog", profileInvokeNode.getNotificationFreqLog());
                SnippetTemplate template = template(profileNode, args);
                template.instantiate(providers.getMetaAccess(), profileNode, SnippetTemplate.DEFAULT_REPLACER, args);
            }
            else
            {
                throw new GraalError("Unsupported profile node type: " + profileNode);
            }

            if (!profileNode.isDeleted())
            {
                GraphUtil.killWithUnusedFloatingInputs(profileNode);
            }
        }
    }
}
