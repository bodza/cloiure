package graalvm.compiler.loop;

import static graalvm.compiler.core.common.GraalOptions.LoopMaxUnswitch;
import static graalvm.compiler.core.common.GraalOptions.MaximumDesiredSize;
import static graalvm.compiler.core.common.GraalOptions.MinimumPeelProbability;

import java.util.List;

import graalvm.compiler.core.common.util.UnsignedLong;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.MergeNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.VirtualState;
import graalvm.compiler.nodes.VirtualState.VirtualClosure;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.debug.ControlFlowAnchorNode;
import graalvm.compiler.nodes.java.TypeSwitchNode;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.MetaAccessProvider;

public class DefaultLoopPolicies implements LoopPolicies {

    public static class Options {
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> LoopUnswitchMaxIncrease = new OptionKey<>(500);
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> LoopUnswitchTrivial = new OptionKey<>(10);
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Double> LoopUnswitchFrequencyBoost = new OptionKey<>(10.0);

        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> FullUnrollMaxNodes = new OptionKey<>(300);
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> FullUnrollMaxIterations = new OptionKey<>(600);
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> ExactFullUnrollMaxNodes = new OptionKey<>(1200);
        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> ExactPartialUnrollMaxNodes = new OptionKey<>(200);

        @Option(help = "", type = OptionType.Expert) public static final OptionKey<Integer> UnrollMaxIterations = new OptionKey<>(16);
    }

    @Override
    public boolean shouldPeel(LoopEx loop, ControlFlowGraph cfg, MetaAccessProvider metaAccess) {
        LoopBeginNode loopBegin = loop.loopBegin();
        double entryProbability = cfg.blockFor(loopBegin.forwardEnd()).probability();
        OptionValues options = cfg.graph.getOptions();
        if (entryProbability > MinimumPeelProbability.getValue(options) && loop.size() + loopBegin.graph().getNodeCount() < MaximumDesiredSize.getValue(options)) {
            // check whether we're allowed to peel this loop
            return loop.canDuplicateLoop();
        } else {
            return false;
        }
    }

    @Override
    public boolean shouldFullUnroll(LoopEx loop) {
        if (!loop.isCounted() || !loop.counted().isConstantMaxTripCount()) {
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        CountedLoopInfo counted = loop.counted();
        UnsignedLong maxTrips = counted.constantMaxTripCount();
        if (maxTrips.equals(0)) {
            return loop.canDuplicateLoop();
        }
        int maxNodes = (counted.isExactTripCount() && counted.isConstantExactTripCount()) ? Options.ExactFullUnrollMaxNodes.getValue(options) : Options.FullUnrollMaxNodes.getValue(options);
        maxNodes = Math.min(maxNodes, Math.max(0, MaximumDesiredSize.getValue(options) - loop.loopBegin().graph().getNodeCount()));
        int size = Math.max(1, loop.size() - 1 - loop.loopBegin().phis().count());
        /* @formatter:off
         * The check below should not throw ArithmeticException because:
         * maxTrips is guaranteed to be >= 1 by the check above
         * - maxTrips * size can not overfow because:
         *   - maxTrips <= FullUnrollMaxIterations <= Integer.MAX_VALUE
         *   - 1 <= size <= Integer.MAX_VALUE
         * @formatter:on
         */
        if (maxTrips.isLessOrEqualTo(Options.FullUnrollMaxIterations.getValue(options)) && maxTrips.minus(1).times(size).isLessOrEqualTo(maxNodes)) {
            // check whether we're allowed to unroll this loop
            return loop.canDuplicateLoop();
        } else {
            return false;
        }
    }

    @Override
    public boolean shouldPartiallyUnroll(LoopEx loop) {
        LoopBeginNode loopBegin = loop.loopBegin();
        if (!loop.isCounted()) {
            loopBegin.getDebug().log(DebugContext.VERBOSE_LEVEL, "shouldPartiallyUnroll %s isn't counted", loopBegin);
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        int maxNodes = Options.ExactPartialUnrollMaxNodes.getValue(options);
        maxNodes = Math.min(maxNodes, Math.max(0, MaximumDesiredSize.getValue(options) - loop.loopBegin().graph().getNodeCount()));
        int size = Math.max(1, loop.size() - 1 - loop.loopBegin().phis().count());
        int unrollFactor = loopBegin.getUnrollFactor();
        if (unrollFactor == 1) {
            double loopFrequency = loopBegin.loopFrequency();
            if (loopBegin.isSimpleLoop() && loopFrequency < 5.0) {
                loopBegin.getDebug().log(DebugContext.VERBOSE_LEVEL, "shouldPartiallyUnroll %s frequency too low %s ", loopBegin, loopFrequency);
                return false;
            }
            loopBegin.setLoopOrigFrequency(loopFrequency);
        }
        int maxUnroll = Options.UnrollMaxIterations.getValue(options);
        // Now correct size for the next unroll. UnrollMaxIterations == 1 means perform the
        // pre/main/post transformation but don't actually unroll the main loop.
        size += size;
        if (maxUnroll == 1 && loopBegin.isSimpleLoop() || size <= maxNodes && unrollFactor < maxUnroll) {
            // Will the next unroll fit?
            if ((int) loopBegin.loopOrigFrequency() < (unrollFactor * 2)) {
                return false;
            }
            // Check whether we're allowed to unroll this loop
            for (Node node : loop.inside().nodes()) {
                if (node instanceof ControlFlowAnchorNode) {
                    return false;
                }
                if (node instanceof InvokeNode) {
                    return false;
                }
            }
            return true;
        } else {
            loopBegin.getDebug().log(DebugContext.VERBOSE_LEVEL, "shouldPartiallyUnroll %s unrolled loop is too large %s ", loopBegin, size);
            return false;
        }
    }

    @Override
    public boolean shouldTryUnswitch(LoopEx loop) {
        LoopBeginNode loopBegin = loop.loopBegin();
        double loopFrequency = loopBegin.loopFrequency();
        if (loopFrequency <= 1.0) {
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        return loopBegin.unswitches() <= LoopMaxUnswitch.getValue(options);
    }

    private static final class CountingClosure implements VirtualClosure {
        int count;

        @Override
        public void apply(VirtualState node) {
            count++;
        }
    }

    private static class IsolatedInitialization {
        static final CounterKey UNSWITCH_SPLIT_WITH_PHIS = DebugContext.counter("UnswitchSplitWithPhis");
    }

    @Override
    public boolean shouldUnswitch(LoopEx loop, List<ControlSplitNode> controlSplits) {
        int phis = 0;
        StructuredGraph graph = loop.loopBegin().graph();
        DebugContext debug = graph.getDebug();
        NodeBitMap branchNodes = graph.createNodeBitMap();
        for (ControlSplitNode controlSplit : controlSplits) {
            for (Node successor : controlSplit.successors()) {
                AbstractBeginNode branch = (AbstractBeginNode) successor;
                // this may count twice because of fall-through in switches
                loop.nodesInLoopBranch(branchNodes, branch);
            }
            Block postDomBlock = loop.loopsData().getCFG().blockFor(controlSplit).getPostdominator();
            if (postDomBlock != null) {
                IsolatedInitialization.UNSWITCH_SPLIT_WITH_PHIS.increment(debug);
                phis += ((MergeNode) postDomBlock.getBeginNode()).phis().count();
            }
        }
        int inBranchTotal = branchNodes.count();

        CountingClosure stateNodesCount = new CountingClosure();
        double loopFrequency = loop.loopBegin().loopFrequency();
        OptionValues options = loop.loopBegin().getOptions();
        int maxDiff = Options.LoopUnswitchTrivial.getValue(options) + (int) (Options.LoopUnswitchFrequencyBoost.getValue(options) * (loopFrequency - 1.0 + phis));

        maxDiff = Math.min(maxDiff, Options.LoopUnswitchMaxIncrease.getValue(options));
        int remainingGraphSpace = MaximumDesiredSize.getValue(options) - graph.getNodeCount();
        maxDiff = Math.min(maxDiff, remainingGraphSpace);

        loop.loopBegin().stateAfter().applyToVirtual(stateNodesCount);
        int loopTotal = loop.size() - loop.loopBegin().phis().count() - stateNodesCount.count - 1;
        int actualDiff = (loopTotal - inBranchTotal);
        ControlSplitNode firstSplit = controlSplits.get(0);
        if (firstSplit instanceof TypeSwitchNode) {
            int copies = firstSplit.successors().count() - 1;
            for (Node succ : firstSplit.successors()) {
                FixedNode current = (FixedNode) succ;
                while (current instanceof FixedWithNextNode) {
                    current = ((FixedWithNextNode) current).next();
                }
                if (current instanceof DeoptimizeNode) {
                    copies--;
                }
            }
            actualDiff = actualDiff * copies;
        }

        debug.log("shouldUnswitch(%s, %s) : delta=%d (%.2f%% inside of branches), max=%d, f=%.2f, phis=%d -> %b", loop, controlSplits, actualDiff, (double) (inBranchTotal) / loopTotal * 100, maxDiff,
                        loopFrequency, phis, actualDiff <= maxDiff);
        if (actualDiff <= maxDiff) {
            // check whether we're allowed to unswitch this loop
            return loop.canDuplicateLoop();
        } else {
            return false;
        }
    }

}
