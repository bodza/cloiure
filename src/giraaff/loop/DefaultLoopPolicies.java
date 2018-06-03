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

// @class DefaultLoopPolicies
public final class DefaultLoopPolicies implements LoopPolicies
{
    @Override
    public boolean shouldPeel(LoopEx __loop, ControlFlowGraph __cfg, MetaAccessProvider __metaAccess)
    {
        LoopBeginNode __loopBegin = __loop.loopBegin();
        if (__cfg.blockFor(__loopBegin.forwardEnd()).probability() > GraalOptions.minimumPeelProbability && __loop.size() + __loopBegin.graph().getNodeCount() < GraalOptions.maximumDesiredSize)
        {
            // check whether we're allowed to peel this loop
            return __loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean shouldFullUnroll(LoopEx __loop)
    {
        if (!__loop.isCounted() || !__loop.counted().isConstantMaxTripCount())
        {
            return false;
        }
        CountedLoopInfo __counted = __loop.counted();
        UnsignedLong __maxTrips = __counted.constantMaxTripCount();
        if (__maxTrips.equals(0))
        {
            return __loop.canDuplicateLoop();
        }
        int __maxNodes = (__counted.isExactTripCount() && __counted.isConstantExactTripCount()) ? GraalOptions.exactFullUnrollMaxNodes : GraalOptions.fullUnrollMaxNodes;
        __maxNodes = Math.min(__maxNodes, Math.max(0, GraalOptions.maximumDesiredSize - __loop.loopBegin().graph().getNodeCount()));
        int __size = Math.max(1, __loop.size() - 1 - __loop.loopBegin().phis().count());
        // The check below should not throw ArithmeticException because:
        // maxTrips is guaranteed to be >= 1 by the check above
        // - maxTrips * size can not overfow because:
        //   - maxTrips <= FullUnrollMaxIterations <= Integer.MAX_VALUE
        //   - 1 <= size <= Integer.MAX_VALUE
        if (__maxTrips.isLessOrEqualTo(GraalOptions.fullUnrollMaxIterations) && __maxTrips.minus(1).times(__size).isLessOrEqualTo(__maxNodes))
        {
            // check whether we're allowed to unroll this loop
            return __loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean shouldPartiallyUnroll(LoopEx __loop)
    {
        LoopBeginNode __loopBegin = __loop.loopBegin();
        if (!__loop.isCounted())
        {
            return false;
        }
        int __maxNodes = GraalOptions.exactPartialUnrollMaxNodes;
        __maxNodes = Math.min(__maxNodes, Math.max(0, GraalOptions.maximumDesiredSize - __loop.loopBegin().graph().getNodeCount()));
        int __size = Math.max(1, __loop.size() - 1 - __loop.loopBegin().phis().count());
        int __unrollFactor = __loopBegin.getUnrollFactor();
        if (__unrollFactor == 1)
        {
            double __loopFrequency = __loopBegin.loopFrequency();
            if (__loopBegin.isSimpleLoop() && __loopFrequency < 5.0)
            {
                return false;
            }
            __loopBegin.setLoopOrigFrequency(__loopFrequency);
        }
        int __maxUnroll = GraalOptions.unrollMaxIterations;
        // Now correct size for the next unroll. UnrollMaxIterations == 1 means perform the
        // pre/main/post transformation but don't actually unroll the main loop.
        __size += __size;
        if (__maxUnroll == 1 && __loopBegin.isSimpleLoop() || __size <= __maxNodes && __unrollFactor < __maxUnroll)
        {
            // Will the next unroll fit?
            if ((int) __loopBegin.loopOrigFrequency() < (__unrollFactor * 2))
            {
                return false;
            }
            // check whether we're allowed to unroll this loop
            for (Node __node : __loop.inside().nodes())
            {
                if (__node instanceof ControlFlowAnchorNode)
                {
                    return false;
                }
                if (__node instanceof InvokeNode)
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
    public boolean shouldTryUnswitch(LoopEx __loop)
    {
        LoopBeginNode __loopBegin = __loop.loopBegin();
        if (__loopBegin.loopFrequency() <= 1.0)
        {
            return false;
        }
        return __loopBegin.unswitches() <= GraalOptions.loopMaxUnswitch;
    }

    // @class DefaultLoopPolicies.CountingClosure
    private static final class CountingClosure implements VirtualClosure
    {
        // @field
        int ___count;

        @Override
        public void apply(VirtualState __node)
        {
            this.___count++;
        }
    }

    @Override
    public boolean shouldUnswitch(LoopEx __loop, List<ControlSplitNode> __controlSplits)
    {
        int __phis = 0;
        StructuredGraph __graph = __loop.loopBegin().graph();
        NodeBitMap __branchNodes = __graph.createNodeBitMap();
        for (ControlSplitNode __controlSplit : __controlSplits)
        {
            for (Node __successor : __controlSplit.successors())
            {
                AbstractBeginNode __branch = (AbstractBeginNode) __successor;
                // this may count twice because of fall-through in switches
                __loop.nodesInLoopBranch(__branchNodes, __branch);
            }
            Block __postDomBlock = __loop.loopsData().getCFG().blockFor(__controlSplit).getPostdominator();
            if (__postDomBlock != null)
            {
                __phis += ((MergeNode) __postDomBlock.getBeginNode()).phis().count();
            }
        }
        int __inBranchTotal = __branchNodes.count();

        CountingClosure __stateNodesCount = new CountingClosure();
        double __loopFrequency = __loop.loopBegin().loopFrequency();
        int __maxDiff = GraalOptions.loopUnswitchTrivial + (int) (GraalOptions.loopUnswitchFrequencyBoost * (__loopFrequency - 1.0 + __phis));

        __maxDiff = Math.min(__maxDiff, GraalOptions.loopUnswitchMaxIncrease);
        int __remainingGraphSpace = GraalOptions.maximumDesiredSize - __graph.getNodeCount();
        __maxDiff = Math.min(__maxDiff, __remainingGraphSpace);

        __loop.loopBegin().stateAfter().applyToVirtual(__stateNodesCount);
        int __loopTotal = __loop.size() - __loop.loopBegin().phis().count() - __stateNodesCount.___count - 1;
        int __actualDiff = (__loopTotal - __inBranchTotal);
        ControlSplitNode __firstSplit = __controlSplits.get(0);
        if (__firstSplit instanceof TypeSwitchNode)
        {
            int __copies = __firstSplit.successors().count() - 1;
            for (Node __succ : __firstSplit.successors())
            {
                FixedNode __current = (FixedNode) __succ;
                while (__current instanceof FixedWithNextNode)
                {
                    __current = ((FixedWithNextNode) __current).next();
                }
                if (__current instanceof DeoptimizeNode)
                {
                    __copies--;
                }
            }
            __actualDiff = __actualDiff * __copies;
        }

        if (__actualDiff <= __maxDiff)
        {
            // check whether we're allowed to unswitch this loop
            return __loop.canDuplicateLoop();
        }
        else
        {
            return false;
        }
    }
}
