package graalvm.compiler.phases.common;

import static graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality.Optional;

import java.util.List;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.GuardNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.ProxyNode;
import graalvm.compiler.nodes.StaticDeoptimizingNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValuePhiNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.DeoptimizationAction;

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
 *
 */
public class ConvertDeoptimizeToGuardPhase extends BasePhase<PhaseContext>
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

        new DeadCodeEliminationPhase(Optional).apply(graph);
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
            if (GraalOptions.GuardPriorities.getValue(from.getOptions()) && current instanceof FixedGuardNode)
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
                    // Prioritize the source position of the IfNode
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
                    SimplifierTool simplifierTool = GraphUtil.getDefaultSimplifier(null, null, null, false, graph.getAssumptions(), graph.getOptions(), loweringProvider);
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
