package graalvm.compiler.hotspot.replacements.profiling;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileBranchNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileInvokeNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileNode;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.word.MethodCountersPointer;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.extended.BranchProbabilityNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;

public class ProbabilisticProfileSnippets implements Snippets
{
    @Snippet
    public static boolean shouldProfile(@ConstantParameter int probLog, int random)
    {
        int probabilityMask = (1 << probLog) - 1;
        return (random & probabilityMask) == 0;
    }

    @Snippet
    public static int notificationMask(int freqLog, int probLog, int stepLog)
    {
        int frequencyMask = (1 << freqLog) - 1;
        int stepMask = (1 << (stepLog + probLog)) - 1;
        return frequencyMask & ~stepMask;
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void methodInvocationEvent(@ConstantNodeParameter ForeignCallDescriptor descriptor, MethodCountersPointer counters);

    @Snippet
    public static void profileMethodEntryWithProbability(MethodCountersPointer counters, int random, int step, int stepLog, @ConstantParameter int freqLog, @ConstantParameter int probLog)
    {
        if (BranchProbabilityNode.probability(1.0 / (1 << probLog), shouldProfile(probLog, random)))
        {
            int counterValue = counters.readInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterOffset) + ((HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterIncrement * step) << probLog);
            counters.writeInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterOffset, counterValue);
            if (freqLog >= 0)
            {
                int mask = notificationMask(freqLog, probLog, stepLog);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, (counterValue & (mask << HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterShift)) == 0))
                {
                    methodInvocationEvent(HotSpotBackend.INVOCATION_EVENT, counters);
                }
            }
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void methodBackedgeEvent(@ConstantNodeParameter ForeignCallDescriptor descriptor, MethodCountersPointer counters, int bci, int targetBci);

    @Snippet
    public static void profileBackedgeWithProbability(MethodCountersPointer counters, int random, int step, int stepLog, @ConstantParameter int freqLog, @ConstantParameter int probLog, int bci, int targetBci)
    {
        if (BranchProbabilityNode.probability(1.0 / (1 << probLog), shouldProfile(probLog, random)))
        {
            int counterValue = counters.readInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).backedgeCounterOffset) + ((HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterIncrement * step) << probLog);
            counters.writeInt(HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).backedgeCounterOffset, counterValue);
            int mask = notificationMask(freqLog, probLog, stepLog);
            if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, (counterValue & (mask << HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).invocationCounterShift)) == 0))
            {
                methodBackedgeEvent(HotSpotBackend.BACKEDGE_EVENT, counters, bci, targetBci);
            }
        }
    }

    @Snippet
    public static void profileConditionalBackedgeWithProbability(MethodCountersPointer counters, int random, int step, int stepLog, @ConstantParameter int freqLog, @ConstantParameter int probLog, boolean branchCondition, int bci, int targetBci)
    {
        if (branchCondition)
        {
            profileBackedgeWithProbability(counters, random, step, stepLog, freqLog, probLog, bci, targetBci);
        }
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo profileMethodEntryWithProbability = snippet(ProbabilisticProfileSnippets.class, "profileMethodEntryWithProbability");
        private final SnippetInfo profileBackedgeWithProbability = snippet(ProbabilisticProfileSnippets.class, "profileBackedgeWithProbability");
        private final SnippetInfo profileConditionalBackedgeWithProbability = snippet(ProbabilisticProfileSnippets.class, "profileConditionalBackedgeWithProbability");

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
                SnippetInfo snippet = profileBranchNode.hasCondition() ? profileConditionalBackedgeWithProbability : profileBackedgeWithProbability;
                Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
                ConstantNode bci = ConstantNode.forInt(profileBranchNode.bci(), graph);
                ConstantNode targetBci = ConstantNode.forInt(profileBranchNode.targetBci(), graph);

                args.add("counters", counters);
                args.add("random", profileBranchNode.getRandom());
                args.add("step", step);
                args.add("stepLog", stepLog);
                args.addConst("freqLog", profileBranchNode.getNotificationFreqLog());
                args.addConst("probLog", profileBranchNode.getProbabilityLog());
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
                Arguments args = new Arguments(profileMethodEntryWithProbability, graph.getGuardsStage(), tool.getLoweringStage());

                args.add("counters", counters);
                args.add("random", profileInvokeNode.getRandom());
                args.add("step", step);
                args.add("stepLog", stepLog);
                args.addConst("freqLog", profileInvokeNode.getNotificationFreqLog());
                args.addConst("probLog", profileInvokeNode.getProbabilityLog());
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
