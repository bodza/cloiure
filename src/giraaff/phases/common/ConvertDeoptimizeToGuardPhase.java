package giraaff.phases.common;

import java.util.List;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.DeoptimizationAction;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StaticDeoptimizingNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.BasePhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.tiers.PhaseContext;

/**
 * This phase will find branches which always end with a {@link DeoptimizeNode} and replace their
 * {@link ControlSplitNode ControlSplitNodes} with {@link FixedGuardNode FixedGuardNodes}.
 *
 * This is useful because {@link FixedGuardNode FixedGuardNodes} will be lowered to {@link GuardNode
 * GuardNodes} which can later be optimized more aggressively than control-flow constructs.
 *
 * This is currently only done for branches that start from a {@link IfNode}. If it encounters a
 * branch starting at an other kind of {@link ControlSplitNode}, it will only bring the
 * {@link DeoptimizeNode} as close to the {@link ControlSplitNode} as possible.
 */
// @class ConvertDeoptimizeToGuardPhase
public final class ConvertDeoptimizeToGuardPhase extends BasePhase<PhaseContext>
{
    @Override
    protected void run(final StructuredGraph graph, PhaseContext context)
    {
        for (DeoptimizeNode d : graph.getNodes(DeoptimizeNode.TYPE))
        {
            if (d.getAction() == DeoptimizationAction.None)
            {
                continue;
            }
            propagateFixed(d, d, context != null ? context.getLowerer() : null);
        }

        if (context != null)
        {
            for (FixedGuardNode fixedGuard : graph.getNodes(FixedGuardNode.TYPE))
            {
                trySplitFixedGuard(fixedGuard, context);
            }
        }

        new DeadCodeEliminationPhase(Optionality.Optional).apply(graph);
    }

    private void trySplitFixedGuard(FixedGuardNode fixedGuard, PhaseContext context)
    {
        LogicNode condition = fixedGuard.condition();
        if (condition instanceof CompareNode)
        {
            CompareNode compare = (CompareNode) condition;
            ValueNode x = compare.getX();
            ValuePhiNode xPhi = (x instanceof ValuePhiNode) ? (ValuePhiNode) x : null;
            if (x instanceof ConstantNode || xPhi != null)
            {
                ValueNode y = compare.getY();
                ValuePhiNode yPhi = (y instanceof ValuePhiNode) ? (ValuePhiNode) y : null;
                if (y instanceof ConstantNode || yPhi != null)
                {
                    processFixedGuardAndPhis(fixedGuard, context, compare, x, xPhi, y, yPhi);
                }
            }
        }
    }

    private void processFixedGuardAndPhis(FixedGuardNode fixedGuard, PhaseContext context, CompareNode compare, ValueNode x, ValuePhiNode xPhi, ValueNode y, ValuePhiNode yPhi)
    {
        AbstractBeginNode pred = AbstractBeginNode.prevBegin(fixedGuard);
        if (pred instanceof AbstractMergeNode)
        {
            AbstractMergeNode merge = (AbstractMergeNode) pred;
            if (xPhi != null && xPhi.merge() != merge)
            {
                return;
            }
            if (yPhi != null && yPhi.merge() != merge)
            {
                return;
            }

            processFixedGuardAndMerge(fixedGuard, context, compare, x, xPhi, y, yPhi, merge);
        }
    }

    private void processFixedGuardAndMerge(FixedGuardNode fixedGuard, PhaseContext context, CompareNode compare, ValueNode x, ValuePhiNode xPhi, ValueNode y, ValuePhiNode yPhi, AbstractMergeNode merge)
    {
        List<EndNode> mergePredecessors = merge.cfgPredecessors().snapshot();
        for (int i = 0; i < mergePredecessors.size(); ++i)
        {
            AbstractEndNode mergePredecessor = mergePredecessors.get(i);
            if (!mergePredecessor.isAlive())
            {
                break;
            }
            Constant xs;
            if (xPhi == null)
            {
                xs = x.asConstant();
            }
            else
            {
                xs = xPhi.valueAt(mergePredecessor).asConstant();
            }
            Constant ys;
            if (yPhi == null)
            {
                ys = y.asConstant();
            }
            else
            {
                ys = yPhi.valueAt(mergePredecessor).asConstant();
            }
            if (xs != null && ys != null && compare.condition().foldCondition(xs, ys, context.getConstantReflection(), compare.unorderedIsTrue()) == fixedGuard.isNegated())
            {
                propagateFixed(mergePredecessor, fixedGuard, context.getLowerer());
            }
        }
    }

    private void propagateFixed(FixedNode from, StaticDeoptimizingNode deopt, LoweringProvider loweringProvider)
    {
        Node current = from;
        while (current != null)
        {
            if (GraalOptions.guardPriorities && current instanceof FixedGuardNode)
            {
                FixedGuardNode otherGuard = (FixedGuardNode) current;
                if (otherGuard.computePriority().isHigherPriorityThan(deopt.computePriority()))
                {
                    moveAsDeoptAfter(otherGuard, deopt);
                    return;
                }
            }
            else if (current instanceof AbstractBeginNode)
            {
                if (current instanceof AbstractMergeNode)
                {
                    AbstractMergeNode mergeNode = (AbstractMergeNode) current;
                    FixedNode next = mergeNode.next();
                    while (mergeNode.isAlive())
                    {
                        AbstractEndNode end = mergeNode.forwardEnds().first();
                        propagateFixed(end, deopt, loweringProvider);
                    }
                    propagateFixed(next, deopt, loweringProvider);
                    return;
                }
                else if (current.predecessor() instanceof IfNode)
                {
                    IfNode ifNode = (IfNode) current.predecessor();
                    // prioritize the source position of the IfNode
                    StructuredGraph graph = ifNode.graph();
                    LogicNode conditionNode = ifNode.condition();
                    boolean negateGuardCondition = current == ifNode.trueSuccessor();
                    FixedGuardNode guard = graph.add(new FixedGuardNode(conditionNode, deopt.getReason(), deopt.getAction(), deopt.getSpeculation(), negateGuardCondition));

                    FixedWithNextNode pred = (FixedWithNextNode) ifNode.predecessor();
                    AbstractBeginNode survivingSuccessor;
                    if (negateGuardCondition)
                    {
                        survivingSuccessor = ifNode.falseSuccessor();
                    }
                    else
                    {
                        survivingSuccessor = ifNode.trueSuccessor();
                    }
                    graph.removeSplitPropagate(ifNode, survivingSuccessor);

                    Node newGuard = guard;
                    if (survivingSuccessor instanceof LoopExitNode)
                    {
                        newGuard = ProxyNode.forGuard(guard, (LoopExitNode) survivingSuccessor, graph);
                    }
                    survivingSuccessor.replaceAtUsages(InputType.Guard, newGuard);

                    FixedNode next = pred.next();
                    pred.setNext(guard);
                    guard.setNext(next);
                    SimplifierTool simplifierTool = GraphUtil.getDefaultSimplifier(null, null, null, false, graph.getAssumptions(), loweringProvider);
                    survivingSuccessor.simplify(simplifierTool);
                    return;
                }
                else if (current.predecessor() == null || current.predecessor() instanceof ControlSplitNode)
                {
                    moveAsDeoptAfter((AbstractBeginNode) current, deopt);
                    return;
                }
            }
            current = current.predecessor();
        }
    }

    private static void moveAsDeoptAfter(FixedWithNextNode node, StaticDeoptimizingNode deopt)
    {
        FixedNode next = node.next();
        if (next != deopt.asNode())
        {
            node.setNext(node.graph().add(new DeoptimizeNode(deopt.getAction(), deopt.getReason(), deopt.getSpeculation())));
            GraphUtil.killCFG(next);
        }
    }
}
