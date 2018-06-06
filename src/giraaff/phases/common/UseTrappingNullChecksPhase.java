package giraaff.phases.common;

import java.util.List;

import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractDeoptimizeNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.DynamicDeoptimizeNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.extended.NullCheckNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.LowTierContext;

// @class UseTrappingNullChecksPhase
public final class UseTrappingNullChecksPhase extends BasePhase<LowTierContext>
{
    @Override
    protected void run(StructuredGraph __graph, LowTierContext __context)
    {
        if (!GraalOptions.useTrappingNullChecks || __context.getTarget().implicitNullCheckLimit <= 0)
        {
            return;
        }

        long __implicitNullCheckLimit = __context.getTarget().implicitNullCheckLimit;
        for (DeoptimizeNode __deopt : __graph.getNodes(DeoptimizeNode.TYPE))
        {
            tryUseTrappingNullCheck(__deopt, __deopt.predecessor(), __deopt.getReason(), __deopt.getSpeculation(), __implicitNullCheckLimit);
        }
        for (DynamicDeoptimizeNode __deopt : __graph.getNodes(DynamicDeoptimizeNode.TYPE))
        {
            tryUseTrappingNullCheck(__context.getMetaAccess(), __deopt, __implicitNullCheckLimit);
        }
    }

    private static void tryUseTrappingNullCheck(MetaAccessProvider __metaAccessProvider, DynamicDeoptimizeNode __deopt, long __implicitNullCheckLimit)
    {
        Node __predecessor = __deopt.predecessor();
        if (__predecessor instanceof AbstractMergeNode)
        {
            AbstractMergeNode __merge = (AbstractMergeNode) __predecessor;

            // Process each predecessor at the merge, unpacking the reasons and speculations as needed.
            ValueNode __reason = __deopt.getActionAndReason();
            ValuePhiNode __reasonPhi = null;
            List<ValueNode> __reasons = null;
            int __expectedPhis = 0;

            if (__reason instanceof ValuePhiNode)
            {
                __reasonPhi = (ValuePhiNode) __reason;
                if (__reasonPhi.merge() != __merge)
                {
                    return;
                }
                __reasons = __reasonPhi.values().snapshot();
                __expectedPhis++;
            }
            else if (!__reason.isConstant())
            {
                return;
            }

            ValueNode __speculation = __deopt.getSpeculation();
            ValuePhiNode __speculationPhi = null;
            List<ValueNode> __speculations = null;
            if (__speculation instanceof ValuePhiNode)
            {
                __speculationPhi = (ValuePhiNode) __speculation;
                if (__speculationPhi.merge() != __merge)
                {
                    return;
                }
                __speculations = __speculationPhi.values().snapshot();
                __expectedPhis++;
            }

            if (__merge.phis().count() != __expectedPhis)
            {
                return;
            }

            int __index = 0;
            for (AbstractEndNode __end : __merge.cfgPredecessors().snapshot())
            {
                ValueNode __thisReason = __reasons != null ? __reasons.get(__index) : __reason;
                ValueNode __thisSpeculation = __speculations != null ? __speculations.get(__index++) : __speculation;
                if (!__thisReason.isConstant() || !__thisSpeculation.isConstant() || !__thisSpeculation.asConstant().equals(JavaConstant.NULL_POINTER))
                {
                    continue;
                }
                DeoptimizationReason __deoptimizationReason = __metaAccessProvider.decodeDeoptReason(__thisReason.asJavaConstant());
                tryUseTrappingNullCheck(__deopt, __end.predecessor(), __deoptimizationReason, null, __implicitNullCheckLimit);
            }
        }
    }

    private static void tryUseTrappingNullCheck(AbstractDeoptimizeNode __deopt, Node __predecessor, DeoptimizationReason __deoptimizationReason, JavaConstant __speculation, long __implicitNullCheckLimit)
    {
        if (__deoptimizationReason != DeoptimizationReason.NullCheckException && __deoptimizationReason != DeoptimizationReason.UnreachedCode)
        {
            return;
        }
        if (__speculation != null && !__speculation.equals(JavaConstant.NULL_POINTER))
        {
            return;
        }
        if (__predecessor instanceof AbstractMergeNode)
        {
            AbstractMergeNode __merge = (AbstractMergeNode) __predecessor;
            if (__merge.phis().isEmpty())
            {
                for (AbstractEndNode __end : __merge.cfgPredecessors().snapshot())
                {
                    checkPredecessor(__deopt, __end.predecessor(), __deoptimizationReason, __implicitNullCheckLimit);
                }
            }
        }
        else if (__predecessor instanceof AbstractBeginNode)
        {
            checkPredecessor(__deopt, __predecessor, __deoptimizationReason, __implicitNullCheckLimit);
        }
    }

    private static void checkPredecessor(AbstractDeoptimizeNode __deopt, Node __predecessor, DeoptimizationReason __deoptimizationReason, long __implicitNullCheckLimit)
    {
        Node __current = __predecessor;
        AbstractBeginNode __branch = null;
        while (__current instanceof AbstractBeginNode)
        {
            __branch = (AbstractBeginNode) __current;
            if (__branch.anchored().isNotEmpty())
            {
                // some input of the deopt framestate is anchored to this branch
                return;
            }
            __current = __current.predecessor();
        }
        if (__current instanceof IfNode)
        {
            IfNode __ifNode = (IfNode) __current;
            if (__branch != __ifNode.trueSuccessor())
            {
                return;
            }
            LogicNode __condition = __ifNode.condition();
            if (__condition instanceof IsNullNode)
            {
                replaceWithTrappingNullCheck(__deopt, __ifNode, __condition, __deoptimizationReason, __implicitNullCheckLimit);
            }
        }
    }

    private static void replaceWithTrappingNullCheck(AbstractDeoptimizeNode __deopt, IfNode __ifNode, LogicNode __condition, DeoptimizationReason __deoptimizationReason, long __implicitNullCheckLimit)
    {
        IsNullNode __isNullNode = (IsNullNode) __condition;
        AbstractBeginNode __nonTrappingContinuation = __ifNode.falseSuccessor();
        AbstractBeginNode __trappingContinuation = __ifNode.trueSuccessor();

        DeoptimizingFixedWithNextNode __trappingNullCheck = null;
        FixedNode __nextNonTrapping = __nonTrappingContinuation.next();
        ValueNode __value = __isNullNode.getValue();
        if (GraalOptions.optImplicitNullChecks && __implicitNullCheckLimit > 0)
        {
            if (__nextNonTrapping instanceof FixedAccessNode)
            {
                FixedAccessNode __fixedAccessNode = (FixedAccessNode) __nextNonTrapping;
                if (__fixedAccessNode.canNullCheck())
                {
                    AddressNode __address = __fixedAccessNode.getAddress();
                    ValueNode __base = __address.getBase();
                    ValueNode __index = __address.getIndex();
                    // allow for architectures which cannot fold an
                    // intervening uncompress out of the address chain
                    if (__base != null && __base instanceof CompressionNode)
                    {
                        __base = ((CompressionNode) __base).getValue();
                    }
                    if (__index != null && __index instanceof CompressionNode)
                    {
                        __index = ((CompressionNode) __index).getValue();
                    }
                    if (((__base == __value && __index == null) || (__base == null && __index == __value)) && __address.getMaxConstantDisplacement() < __implicitNullCheckLimit)
                    {
                        // Opportunity for implicit null check as part of an existing read found!
                        __fixedAccessNode.setStateBefore(__deopt.stateBefore());
                        __fixedAccessNode.setNullCheck(true);
                        __deopt.graph().removeSplit(__ifNode, __nonTrappingContinuation);
                        __trappingNullCheck = __fixedAccessNode;
                    }
                }
            }
        }

        if (__trappingNullCheck == null)
        {
            // Need to add a null check node.
            __trappingNullCheck = __deopt.graph().add(new NullCheckNode(__value));
            __deopt.graph().replaceSplit(__ifNode, __trappingNullCheck, __nonTrappingContinuation);
        }

        __trappingNullCheck.setStateBefore(__deopt.stateBefore());

        // We now have the pattern StandardOp.NullCheck/BeginNode/... It's possible some node is using the
        // BeginNode as a guard input, so replace guard users of the Begin with the StandardOp.NullCheck and
        // then remove the Begin from the graph.
        __nonTrappingContinuation.replaceAtUsages(InputType.Guard, __trappingNullCheck);

        if (__nonTrappingContinuation instanceof BeginNode)
        {
            GraphUtil.unlinkFixedNode(__nonTrappingContinuation);
            __nonTrappingContinuation.safeDelete();
        }

        GraphUtil.killCFG(__trappingContinuation);
        GraphUtil.tryKillUnused(__isNullNode);
    }
}
