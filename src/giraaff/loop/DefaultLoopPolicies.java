package giraaff.loop;

import java.util.List;

import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.util.UnsignedLong;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.VirtualState;
import giraaff.nodes.VirtualState.VirtualClosure;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.debug.ControlFlowAnchorNode;
import giraaff.nodes.java.TypeSwitchNode;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;

public class DefaultLoopPolicies implements LoopPolicies
{
    public static class Options
    {
        public static final OptionKey<Integer> LoopUnswitchMaxIncrease = new OptionKey<>(500);
        public static final OptionKey<Integer> LoopUnswitchTrivial = new OptionKey<>(10);
        public static final OptionKey<Double> LoopUnswitchFrequencyBoost = new OptionKey<>(10.0);

        public static final OptionKey<Integer> FullUnrollMaxNodes = new OptionKey<>(300);
        public static final OptionKey<Integer> FullUnrollMaxIterations = new OptionKey<>(600);
        public static final OptionKey<Integer> ExactFullUnrollMaxNodes = new OptionKey<>(1200);
        public static final OptionKey<Integer> ExactPartialUnrollMaxNodes = new OptionKey<>(200);

        public static final OptionKey<Integer> UnrollMaxIterations = new OptionKey<>(16);
    }

    @Override
    public boolean shouldPeel(LoopEx loop, ControlFlowGraph cfg, MetaAccessProvider metaAccess)
    {
        LoopBeginNode loopBegin = loop.loopBegin();
        double entryProbability = cfg.blockFor(loopBegin.forwardEnd()).probability();
        OptionValues options = cfg.graph.getOptions();
        if (entryProbability > GraalOptions.MinimumPeelProbability.getValue(options) && loop.size() + loopBegin.graph().getNodeCount() < GraalOptions.MaximumDesiredSize.getValue(options))
        {
            // check whether we're allowed to peel this loop
            return loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean shouldFullUnroll(LoopEx loop)
    {
        if (!loop.isCounted() || !loop.counted().isConstantMaxTripCount())
        {
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        CountedLoopInfo counted = loop.counted();
        UnsignedLong maxTrips = counted.constantMaxTripCount();
        if (maxTrips.equals(0))
        {
            return loop.canDuplicateLoop();
        }
        int maxNodes = (counted.isExactTripCount() && counted.isConstantExactTripCount()) ? Options.ExactFullUnrollMaxNodes.getValue(options) : Options.FullUnrollMaxNodes.getValue(options);
        maxNodes = Math.min(maxNodes, Math.max(0, GraalOptions.MaximumDesiredSize.getValue(options) - loop.loopBegin().graph().getNodeCount()));
        int size = Math.max(1, loop.size() - 1 - loop.loopBegin().phis().count());
        /*
         * The check below should not throw ArithmeticException because:
         * maxTrips is guaranteed to be >= 1 by the check above
         * - maxTrips * size can not overfow because:
         *   - maxTrips <= FullUnrollMaxIterations <= Integer.MAX_VALUE
         *   - 1 <= size <= Integer.MAX_VALUE
         */
        if (maxTrips.isLessOrEqualTo(Options.FullUnrollMaxIterations.getValue(options)) && maxTrips.minus(1).times(size).isLessOrEqualTo(maxNodes))
        {
            // check whether we're allowed to unroll this loop
            return loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean shouldPartiallyUnroll(LoopEx loop)
    {
        LoopBeginNode loopBegin = loop.loopBegin();
        if (!loop.isCounted())
        {
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        int maxNodes = Options.ExactPartialUnrollMaxNodes.getValue(options);
        maxNodes = Math.min(maxNodes, Math.max(0, GraalOptions.MaximumDesiredSize.getValue(options) - loop.loopBegin().graph().getNodeCount()));
        int size = Math.max(1, loop.size() - 1 - loop.loopBegin().phis().count());
        int unrollFactor = loopBegin.getUnrollFactor();
        if (unrollFactor == 1)
        {
            double loopFrequency = loopBegin.loopFrequency();
            if (loopBegin.isSimpleLoop() && loopFrequency < 5.0)
            {
                return false;
            }
            loopBegin.setLoopOrigFrequency(loopFrequency);
        }
        int maxUnroll = Options.UnrollMaxIterations.getValue(options);
        // Now correct size for the next unroll. UnrollMaxIterations == 1 means perform the
        // pre/main/post transformation but don't actually unroll the main loop.
        size += size;
        if (maxUnroll == 1 && loopBegin.isSimpleLoop() || size <= maxNodes && unrollFactor < maxUnroll)
        {
            // Will the next unroll fit?
            if ((int) loopBegin.loopOrigFrequency() < (unrollFactor * 2))
            {
                return false;
            }
            // Check whether we're allowed to unroll this loop
            for (Node node : loop.inside().nodes())
            {
                if (node instanceof ControlFlowAnchorNode)
                {
                    return false;
                }
                if (node instanceof InvokeNode)
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean shouldTryUnswitch(LoopEx loop)
    {
        LoopBeginNode loopBegin = loop.loopBegin();
        double loopFrequency = loopBegin.loopFrequency();
        if (loopFrequency <= 1.0)
        {
            return false;
        }
        OptionValues options = loop.entryPoint().getOptions();
        return loopBegin.unswitches() <= GraalOptions.LoopMaxUnswitch.getValue(options);
    }

    private static final class CountingClosure implements VirtualClosure
    {
        int count;

        @Override
        public void apply(VirtualState node)
        {
            count++;
        }
    }

    @Override
    public boolean shouldUnswitch(LoopEx loop, List<ControlSplitNode> controlSplits)
    {
        int phis = 0;
        StructuredGraph graph = loop.loopBegin().graph();
        NodeBitMap branchNodes = graph.createNodeBitMap();
        for (ControlSplitNode controlSplit : controlSplits)
        {
            for (Node successor : controlSplit.successors())
            {
                AbstractBeginNode branch = (AbstractBeginNode) successor;
                // this may count twice because of fall-through in switches
                loop.nodesInLoopBranch(branchNodes, branch);
            }
            Block postDomBlock = loop.loopsData().getCFG().blockFor(controlSplit).getPostdominator();
            if (postDomBlock != null)
            {
                phis += ((MergeNode) postDomBlock.getBeginNode()).phis().count();
            }
        }
        int inBranchTotal = branchNodes.count();

        CountingClosure stateNodesCount = new CountingClosure();
        double loopFrequency = loop.loopBegin().loopFrequency();
        OptionValues options = loop.loopBegin().getOptions();
        int maxDiff = Options.LoopUnswitchTrivial.getValue(options) + (int) (Options.LoopUnswitchFrequencyBoost.getValue(options) * (loopFrequency - 1.0 + phis));

        maxDiff = Math.min(maxDiff, Options.LoopUnswitchMaxIncrease.getValue(options));
        int remainingGraphSpace = GraalOptions.MaximumDesiredSize.getValue(options) - graph.getNodeCount();
        maxDiff = Math.min(maxDiff, remainingGraphSpace);

        loop.loopBegin().stateAfter().applyToVirtual(stateNodesCount);
        int loopTotal = loop.size() - loop.loopBegin().phis().count() - stateNodesCount.count - 1;
        int actualDiff = (loopTotal - inBranchTotal);
        ControlSplitNode firstSplit = controlSplits.get(0);
        if (firstSplit instanceof TypeSwitchNode)
        {
            int copies = firstSplit.successors().count() - 1;
            for (Node succ : firstSplit.successors())
            {
                FixedNode current = (FixedNode) succ;
                while (current instanceof FixedWithNextNode)
                {
                    current = ((FixedWithNextNode) current).next();
                }
                if (current instanceof DeoptimizeNode)
                {
                    copies--;
                }
            }
            actualDiff = actualDiff * copies;
        }

        if (actualDiff <= maxDiff)
        {
            // check whether we're allowed to unswitch this loop
            return loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }
}