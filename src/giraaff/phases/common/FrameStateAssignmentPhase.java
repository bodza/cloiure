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
        protected FrameState processNode(FixedNode node, FrameState previousState)
        {
            FrameState currentState = previousState;
            if (node instanceof DeoptimizingNode.DeoptBefore)
            {
                DeoptimizingNode.DeoptBefore deopt = (DeoptimizingNode.DeoptBefore) node;
                if (deopt.canDeoptimize() && deopt.stateBefore() == null)
                {
                    GraalError.guarantee(currentState != null, "no FrameState at DeoptimizingNode %s", deopt);
                    deopt.setStateBefore(currentState);
                }
            }

            if (node instanceof StateSplit)
            {
                StateSplit stateSplit = (StateSplit) node;
                FrameState stateAfter = stateSplit.stateAfter();
                if (stateAfter != null)
                {
                    if (stateAfter.bci == BytecodeFrame.INVALID_FRAMESTATE_BCI)
                    {
                        currentState = null;
                    }
                    else
                    {
                        currentState = stateAfter;
                    }
                    stateSplit.setStateAfter(null);
                }
            }

            if (node instanceof DeoptimizingNode.DeoptDuring)
            {
                DeoptimizingNode.DeoptDuring deopt = (DeoptimizingNode.DeoptDuring) node;
                if (deopt.canDeoptimize())
                {
                    GraalError.guarantee(currentState != null, "no FrameState at DeoptimizingNode %s", deopt);
                    deopt.computeStateDuring(currentState);
                }
            }

            if (node instanceof DeoptimizingNode.DeoptAfter)
            {
                DeoptimizingNode.DeoptAfter deopt = (DeoptimizingNode.DeoptAfter) node;
                if (deopt.canDeoptimize() && deopt.stateAfter() == null)
                {
                    GraalError.guarantee(currentState != null, "no FrameState at DeoptimizingNode %s", deopt);
                    deopt.setStateAfter(currentState);
                }
            }

            return currentState;
        }

        @Override
        protected FrameState merge(AbstractMergeNode merge, List<FrameState> states)
        {
            FrameState singleFrameState = singleFrameState(states);
            return singleFrameState == null ? merge.stateAfter() : singleFrameState;
        }

        @Override
        protected FrameState afterSplit(AbstractBeginNode node, FrameState oldState)
        {
            return oldState;
        }

        @Override
        protected EconomicMap<LoopExitNode, FrameState> processLoop(LoopBeginNode loop, FrameState initialState)
        {
            return ReentrantNodeIterator.processLoop(this, loop, initialState).exitStates;
        }
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        if (graph.getGuardsStage().areFrameStatesAtSideEffects())
        {
            ReentrantNodeIterator.apply(new FrameStateAssignmentClosure(), graph.start(), null);
            graph.setGuardsStage(GuardsStage.AFTER_FSA);
            graph.getNodes(FrameState.TYPE).filter(state -> state.hasNoUsages()).forEach(GraphUtil::killWithUnusedFloatingInputs);
        }
    }

    private static boolean hasFloatingDeopts(StructuredGraph graph)
    {
        for (Node n : graph.getNodes())
        {
            if (n instanceof DeoptimizingNode && GraphUtil.isFloatingNode(n))
            {
                DeoptimizingNode deoptimizingNode = (DeoptimizingNode) n;
                if (deoptimizingNode.canDeoptimize())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static FrameState singleFrameState(List<FrameState> states)
    {
        FrameState singleState = states.get(0);
        for (int i = 1; i < states.size(); ++i)
        {
            if (states.get(i) != singleState)
            {
                return null;
            }
        }
        if (singleState != null && singleState.bci != BytecodeFrame.INVALID_FRAMESTATE_BCI)
        {
            return singleState;
        }
        return null;
    }
}
