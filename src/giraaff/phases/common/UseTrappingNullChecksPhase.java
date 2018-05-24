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
import giraaff.options.OptionKey;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.LowTierContext;

public class UseTrappingNullChecksPhase extends BasePhase<LowTierContext>
{
    public static class Options
    {
        // Option "Use traps for null checks instead of explicit null-checks."
        public static final OptionKey<Boolean> UseTrappingNullChecks = new OptionKey<>(true);
    }

    @Override
    protected void run(StructuredGraph graph, LowTierContext context)
    {
        if (!Options.UseTrappingNullChecks.getValue(graph.getOptions()) || context.getTarget().implicitNullCheckLimit <= 0)
        {
            return;
        }

        long implicitNullCheckLimit = context.getTarget().implicitNullCheckLimit;
        for (DeoptimizeNode deopt : graph.getNodes(DeoptimizeNode.TYPE))
        {
            tryUseTrappingNullCheck(deopt, deopt.predecessor(), deopt.getReason(), deopt.getSpeculation(), implicitNullCheckLimit);
        }
        for (DynamicDeoptimizeNode deopt : graph.getNodes(DynamicDeoptimizeNode.TYPE))
        {
            tryUseTrappingNullCheck(context.getMetaAccess(), deopt, implicitNullCheckLimit);
        }
    }

    private static void tryUseTrappingNullCheck(MetaAccessProvider metaAccessProvider, DynamicDeoptimizeNode deopt, long implicitNullCheckLimit)
    {
        Node predecessor = deopt.predecessor();
        if (predecessor instanceof AbstractMergeNode)
        {
            AbstractMergeNode merge = (AbstractMergeNode) predecessor;

            // Process each predecessor at the merge, unpacking the reasons and speculations as
            // needed.
            ValueNode reason = deopt.getActionAndReason();
            ValuePhiNode reasonPhi = null;
            List<ValueNode> reasons = null;
            int expectedPhis = 0;

            if (reason instanceof ValuePhiNode)
            {
                reasonPhi = (ValuePhiNode) reason;
                if (reasonPhi.merge() != merge)
                {
                    return;
                }
                reasons = reasonPhi.values().snapshot();
                expectedPhis++;
            }
            else if (!reason.isConstant())
            {
                return;
            }

            ValueNode speculation = deopt.getSpeculation();
            ValuePhiNode speculationPhi = null;
            List<ValueNode> speculations = null;
            if (speculation instanceof ValuePhiNode)
            {
                speculationPhi = (ValuePhiNode) speculation;
                if (speculationPhi.merge() != merge)
                {
                    return;
                }
                speculations = speculationPhi.values().snapshot();
                expectedPhis++;
            }

            if (merge.phis().count() != expectedPhis)
            {
                return;
            }

            int index = 0;
            for (AbstractEndNode end : merge.cfgPredecessors().snapshot())
            {
                ValueNode thisReason = reasons != null ? reasons.get(index) : reason;
                ValueNode thisSpeculation = speculations != null ? speculations.get(index++) : speculation;
                if (!thisReason.isConstant() || !thisSpeculation.isConstant() || !thisSpeculation.asConstant().equals(JavaConstant.NULL_POINTER))
                {
                    continue;
                }
                DeoptimizationReason deoptimizationReason = metaAccessProvider.decodeDeoptReason(thisReason.asJavaConstant());
                tryUseTrappingNullCheck(deopt, end.predecessor(), deoptimizationReason, null, implicitNullCheckLimit);
            }
        }
    }

    private static void tryUseTrappingNullCheck(AbstractDeoptimizeNode deopt, Node predecessor, DeoptimizationReason deoptimizationReason, JavaConstant speculation, long implicitNullCheckLimit)
    {
        if (deoptimizationReason != DeoptimizationReason.NullCheckException && deoptimizationReason != DeoptimizationReason.UnreachedCode)
        {
            return;
        }
        if (speculation != null && !speculation.equals(JavaConstant.NULL_POINTER))
        {
            return;
        }
        if (predecessor instanceof AbstractMergeNode)
        {
            AbstractMergeNode merge = (AbstractMergeNode) predecessor;
            if (merge.phis().isEmpty())
            {
                for (AbstractEndNode end : merge.cfgPredecessors().snapshot())
                {
                    checkPredecessor(deopt, end.predecessor(), deoptimizationReason, implicitNullCheckLimit);
                }
            }
        }
        else if (predecessor instanceof AbstractBeginNode)
        {
            checkPredecessor(deopt, predecessor, deoptimizationReason, implicitNullCheckLimit);
        }
    }

    private static void checkPredecessor(AbstractDeoptimizeNode deopt, Node predecessor, DeoptimizationReason deoptimizationReason, long implicitNullCheckLimit)
    {
        Node current = predecessor;
        AbstractBeginNode branch = null;
        while (current instanceof AbstractBeginNode)
        {
            branch = (AbstractBeginNode) current;
            if (branch.anchored().isNotEmpty())
            {
                // some input of the deopt framestate is anchored to this branch
                return;
            }
            current = current.predecessor();
        }
        if (current instanceof IfNode)
        {
            IfNode ifNode = (IfNode) current;
            if (branch != ifNode.trueSuccessor())
            {
                return;
            }
            LogicNode condition = ifNode.condition();
            if (condition instanceof IsNullNode)
            {
                replaceWithTrappingNullCheck(deopt, ifNode, condition, deoptimizationReason, implicitNullCheckLimit);
            }
        }
    }

    private static void replaceWithTrappingNullCheck(AbstractDeoptimizeNode deopt, IfNode ifNode, LogicNode condition, DeoptimizationReason deoptimizationReason, long implicitNullCheckLimit)
    {
        IsNullNode isNullNode = (IsNullNode) condition;
        AbstractBeginNode nonTrappingContinuation = ifNode.falseSuccessor();
        AbstractBeginNode trappingContinuation = ifNode.trueSuccessor();

        DeoptimizingFixedWithNextNode trappingNullCheck = null;
        FixedNode nextNonTrapping = nonTrappingContinuation.next();
        ValueNode value = isNullNode.getValue();
        if (GraalOptions.OptImplicitNullChecks.getValue(ifNode.graph().getOptions()) && implicitNullCheckLimit > 0)
        {
            if (nextNonTrapping instanceof FixedAccessNode)
            {
                FixedAccessNode fixedAccessNode = (FixedAccessNode) nextNonTrapping;
                if (fixedAccessNode.canNullCheck())
                {
                    AddressNode address = fixedAccessNode.getAddress();
                    ValueNode base = address.getBase();
                    ValueNode index = address.getIndex();
                    // allow for architectures which cannot fold an
                    // intervening uncompress out of the address chain
                    if (base != null && base instanceof CompressionNode)
                    {
                        base = ((CompressionNode) base).getValue();
                    }
                    if (index != null && index instanceof CompressionNode)
                    {
                        index = ((CompressionNode) index).getValue();
                    }
                    if (((base == value && index == null) || (base == null && index == value)) && address.getMaxConstantDisplacement() < implicitNullCheckLimit)
                    {
                        // Opportunity for implicit null check as part of an existing read found!
                        fixedAccessNode.setStateBefore(deopt.stateBefore());
                        fixedAccessNode.setNullCheck(true);
                        deopt.graph().removeSplit(ifNode, nonTrappingContinuation);
                        trappingNullCheck = fixedAccessNode;
                    }
                }
            }
        }

        if (trappingNullCheck == null)
        {
            // Need to add a null check node.
            trappingNullCheck = deopt.graph().add(new NullCheckNode(value));
            deopt.graph().replaceSplit(ifNode, trappingNullCheck, nonTrappingContinuation);
        }

        trappingNullCheck.setStateBefore(deopt.stateBefore());

        /*
         * We now have the pattern NullCheck/BeginNode/... It's possible some node is using the
         * BeginNode as a guard input, so replace guard users of the Begin with the NullCheck and
         * then remove the Begin from the graph.
         */
        nonTrappingContinuation.replaceAtUsages(InputType.Guard, trappingNullCheck);

        if (nonTrappingContinuation instanceof BeginNode)
        {
            GraphUtil.unlinkFixedNode(nonTrappingContinuation);
            nonTrappingContinuation.safeDelete();
        }

        GraphUtil.killCFG(trappingContinuation);
        GraphUtil.tryKillUnused(isNullNode);
    }
}