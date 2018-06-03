package giraaff.phases.common;

import java.util.List;

import jdk.vm.ci.code.BytecodeFrame;

import org.graalvm.collections.EconomicMap;

import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;
import giraaff.phases.graph.ReentrantNodeIterator;
import giraaff.phases.graph.ReentrantNodeIterator.NodeIteratorClosure;
import giraaff.util.GraalError;

/**
 * This phase transfers {@link FrameState} nodes from {@link StateSplit} nodes to
 * {@link DeoptimizingNode DeoptimizingNodes}.
 *
 * This allow to enter the {@link GuardsStage#AFTER_FSA AFTER_FSA} stage of the graph where no new
 * node that may cause deoptimization can be introduced anymore.
 *
 * This Phase processes the graph in post order, assigning the {@link FrameState} from the last
 * {@link StateSplit} node to {@link DeoptimizingNode DeoptimizingNodes}.
 */
// @class FrameStateAssignmentPhase
public final class FrameStateAssignmentPhase extends Phase
{
    // @class FrameStateAssignmentPhase.FrameStateAssignmentClosure
    private static final class FrameStateAssignmentClosure extends NodeIteratorClosure<FrameState>
    {
        @Override
        protected FrameState processNode(FixedNode __node, FrameState __previousState)
        {
            FrameState __currentState = __previousState;
            if (__node instanceof DeoptimizingNode.DeoptBefore)
            {
                DeoptimizingNode.DeoptBefore __deopt = (DeoptimizingNode.DeoptBefore) __node;
                if (__deopt.canDeoptimize() && __deopt.stateBefore() == null)
                {
                    GraalError.guarantee(__currentState != null, "no FrameState at DeoptimizingNode %s", __deopt);
                    __deopt.setStateBefore(__currentState);
                }
            }

            if (__node instanceof StateSplit)
            {
                StateSplit __stateSplit = (StateSplit) __node;
                FrameState __stateAfter = __stateSplit.stateAfter();
                if (__stateAfter != null)
                {
                    if (__stateAfter.bci == BytecodeFrame.INVALID_FRAMESTATE_BCI)
                    {
                        __currentState = null;
                    }
                    else
                    {
                        __currentState = __stateAfter;
                    }
                    __stateSplit.setStateAfter(null);
                }
            }

            if (__node instanceof DeoptimizingNode.DeoptDuring)
            {
                DeoptimizingNode.DeoptDuring __deopt = (DeoptimizingNode.DeoptDuring) __node;
                if (__deopt.canDeoptimize())
                {
                    GraalError.guarantee(__currentState != null, "no FrameState at DeoptimizingNode %s", __deopt);
                    __deopt.computeStateDuring(__currentState);
                }
            }

            if (__node instanceof DeoptimizingNode.DeoptAfter)
            {
                DeoptimizingNode.DeoptAfter __deopt = (DeoptimizingNode.DeoptAfter) __node;
                if (__deopt.canDeoptimize() && __deopt.stateAfter() == null)
                {
                    GraalError.guarantee(__currentState != null, "no FrameState at DeoptimizingNode %s", __deopt);
                    __deopt.setStateAfter(__currentState);
                }
            }

            return __currentState;
        }

        @Override
        protected FrameState merge(AbstractMergeNode __merge, List<FrameState> __states)
        {
            FrameState __singleFrameState = singleFrameState(__states);
            return __singleFrameState == null ? __merge.stateAfter() : __singleFrameState;
        }

        @Override
        protected FrameState afterSplit(AbstractBeginNode __node, FrameState __oldState)
        {
            return __oldState;
        }

        @Override
        protected EconomicMap<LoopExitNode, FrameState> processLoop(LoopBeginNode __loop, FrameState __initialState)
        {
            return ReentrantNodeIterator.processLoop(this, __loop, __initialState).exitStates;
        }
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        if (__graph.getGuardsStage().areFrameStatesAtSideEffects())
        {
            ReentrantNodeIterator.apply(new FrameStateAssignmentClosure(), __graph.start(), null);
            __graph.setGuardsStage(GuardsStage.AFTER_FSA);
            __graph.getNodes(FrameState.TYPE).filter(__state -> __state.hasNoUsages()).forEach(GraphUtil::killWithUnusedFloatingInputs);
        }
    }

    private static boolean hasFloatingDeopts(StructuredGraph __graph)
    {
        for (Node __n : __graph.getNodes())
        {
            if (__n instanceof DeoptimizingNode && GraphUtil.isFloatingNode(__n))
            {
                DeoptimizingNode __deoptimizingNode = (DeoptimizingNode) __n;
                if (__deoptimizingNode.canDeoptimize())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static FrameState singleFrameState(List<FrameState> __states)
    {
        FrameState __singleState = __states.get(0);
        for (int __i = 1; __i < __states.size(); ++__i)
        {
            if (__states.get(__i) != __singleState)
            {
                return null;
            }
        }
        if (__singleState != null && __singleState.bci != BytecodeFrame.INVALID_FRAMESTATE_BCI)
        {
            return __singleState;
        }
        return null;
    }
}
