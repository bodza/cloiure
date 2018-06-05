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

///
// This phase will find branches which always end with a {@link DeoptimizeNode} and replace their
// {@link ControlSplitNode ControlSplitNodes} with {@link FixedGuardNode FixedGuardNodes}.
//
// This is useful because {@link FixedGuardNode FixedGuardNodes} will be lowered to {@link GuardNode
// GuardNodes} which can later be optimized more aggressively than control-flow constructs.
//
// This is currently only done for branches that start from a {@link IfNode}. If it encounters a
// branch starting at an other kind of {@link ControlSplitNode}, it will only bring the
// {@link DeoptimizeNode} as close to the {@link ControlSplitNode} as possible.
///
// @class ConvertDeoptimizeToGuardPhase
public final class ConvertDeoptimizeToGuardPhase extends BasePhase<PhaseContext>
{
    @Override
    protected void run(final StructuredGraph __graph, PhaseContext __context)
    {
        for (DeoptimizeNode __d : __graph.getNodes(DeoptimizeNode.TYPE))
        {
            if (__d.getAction() == DeoptimizationAction.None)
            {
                continue;
            }
            propagateFixed(__d, __d, __context != null ? __context.getLowerer() : null);
        }

        if (__context != null)
        {
            for (FixedGuardNode __fixedGuard : __graph.getNodes(FixedGuardNode.TYPE))
            {
                trySplitFixedGuard(__fixedGuard, __context);
            }
        }

        new DeadCodeEliminationPhase(Optionality.Optional).apply(__graph);
    }

    private void trySplitFixedGuard(FixedGuardNode __fixedGuard, PhaseContext __context)
    {
        LogicNode __condition = __fixedGuard.condition();
        if (__condition instanceof CompareNode)
        {
            CompareNode __compare = (CompareNode) __condition;
            ValueNode __x = __compare.getX();
            ValuePhiNode __xPhi = (__x instanceof ValuePhiNode) ? (ValuePhiNode) __x : null;
            if (__x instanceof ConstantNode || __xPhi != null)
            {
                ValueNode __y = __compare.getY();
                ValuePhiNode __yPhi = (__y instanceof ValuePhiNode) ? (ValuePhiNode) __y : null;
                if (__y instanceof ConstantNode || __yPhi != null)
                {
                    processFixedGuardAndPhis(__fixedGuard, __context, __compare, __x, __xPhi, __y, __yPhi);
                }
            }
        }
    }

    private void processFixedGuardAndPhis(FixedGuardNode __fixedGuard, PhaseContext __context, CompareNode __compare, ValueNode __x, ValuePhiNode __xPhi, ValueNode __y, ValuePhiNode __yPhi)
    {
        AbstractBeginNode __pred = AbstractBeginNode.prevBegin(__fixedGuard);
        if (__pred instanceof AbstractMergeNode)
        {
            AbstractMergeNode __merge = (AbstractMergeNode) __pred;
            if (__xPhi != null && __xPhi.merge() != __merge)
            {
                return;
            }
            if (__yPhi != null && __yPhi.merge() != __merge)
            {
                return;
            }

            processFixedGuardAndMerge(__fixedGuard, __context, __compare, __x, __xPhi, __y, __yPhi, __merge);
        }
    }

    private void processFixedGuardAndMerge(FixedGuardNode __fixedGuard, PhaseContext __context, CompareNode __compare, ValueNode __x, ValuePhiNode __xPhi, ValueNode __y, ValuePhiNode __yPhi, AbstractMergeNode __merge)
    {
        List<EndNode> __mergePredecessors = __merge.cfgPredecessors().snapshot();
        for (int __i = 0; __i < __mergePredecessors.size(); ++__i)
        {
            AbstractEndNode __mergePredecessor = __mergePredecessors.get(__i);
            if (!__mergePredecessor.isAlive())
            {
                break;
            }
            Constant __xs;
            if (__xPhi == null)
            {
                __xs = __x.asConstant();
            }
            else
            {
                __xs = __xPhi.valueAt(__mergePredecessor).asConstant();
            }
            Constant __ys;
            if (__yPhi == null)
            {
                __ys = __y.asConstant();
            }
            else
            {
                __ys = __yPhi.valueAt(__mergePredecessor).asConstant();
            }
            if (__xs != null && __ys != null && __compare.condition().foldCondition(__xs, __ys, __context.getConstantReflection()) == __fixedGuard.isNegated())
            {
                propagateFixed(__mergePredecessor, __fixedGuard, __context.getLowerer());
            }
        }
    }

    private void propagateFixed(FixedNode __from, StaticDeoptimizingNode __deopt, LoweringProvider __loweringProvider)
    {
        Node __current = __from;
        while (__current != null)
        {
            if (GraalOptions.guardPriorities && __current instanceof FixedGuardNode)
            {
                FixedGuardNode __otherGuard = (FixedGuardNode) __current;
                if (__otherGuard.computePriority().isHigherPriorityThan(__deopt.computePriority()))
                {
                    moveAsDeoptAfter(__otherGuard, __deopt);
                    return;
                }
            }
            else if (__current instanceof AbstractBeginNode)
            {
                if (__current instanceof AbstractMergeNode)
                {
                    AbstractMergeNode __mergeNode = (AbstractMergeNode) __current;
                    FixedNode __next = __mergeNode.next();
                    while (__mergeNode.isAlive())
                    {
                        AbstractEndNode __end = __mergeNode.forwardEnds().first();
                        propagateFixed(__end, __deopt, __loweringProvider);
                    }
                    propagateFixed(__next, __deopt, __loweringProvider);
                    return;
                }
                else if (__current.predecessor() instanceof IfNode)
                {
                    IfNode __ifNode = (IfNode) __current.predecessor();
                    // prioritize the source position of the IfNode
                    StructuredGraph __graph = __ifNode.graph();
                    LogicNode __conditionNode = __ifNode.condition();
                    boolean __negateGuardCondition = __current == __ifNode.trueSuccessor();
                    FixedGuardNode __guard = __graph.add(new FixedGuardNode(__conditionNode, __deopt.getReason(), __deopt.getAction(), __deopt.getSpeculation(), __negateGuardCondition));

                    FixedWithNextNode __pred = (FixedWithNextNode) __ifNode.predecessor();
                    AbstractBeginNode __survivingSuccessor;
                    if (__negateGuardCondition)
                    {
                        __survivingSuccessor = __ifNode.falseSuccessor();
                    }
                    else
                    {
                        __survivingSuccessor = __ifNode.trueSuccessor();
                    }
                    __graph.removeSplitPropagate(__ifNode, __survivingSuccessor);

                    Node __newGuard = __guard;
                    if (__survivingSuccessor instanceof LoopExitNode)
                    {
                        __newGuard = ProxyNode.forGuard(__guard, (LoopExitNode) __survivingSuccessor, __graph);
                    }
                    __survivingSuccessor.replaceAtUsages(InputType.Guard, __newGuard);

                    FixedNode __next = __pred.next();
                    __pred.setNext(__guard);
                    __guard.setNext(__next);
                    SimplifierTool __simplifierTool = GraphUtil.getDefaultSimplifier(null, null, null, false, __graph.getAssumptions(), __loweringProvider);
                    __survivingSuccessor.simplify(__simplifierTool);
                    return;
                }
                else if (__current.predecessor() == null || __current.predecessor() instanceof ControlSplitNode)
                {
                    moveAsDeoptAfter((AbstractBeginNode) __current, __deopt);
                    return;
                }
            }
            __current = __current.predecessor();
        }
    }

    private static void moveAsDeoptAfter(FixedWithNextNode __node, StaticDeoptimizingNode __deopt)
    {
        FixedNode __next = __node.next();
        if (__next != __deopt.asNode())
        {
            __node.setNext(__node.graph().add(new DeoptimizeNode(__deopt.getAction(), __deopt.getReason(), __deopt.getSpeculation())));
            GraphUtil.killCFG(__next);
        }
    }
}
