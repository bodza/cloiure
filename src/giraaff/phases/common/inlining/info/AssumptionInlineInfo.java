package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.Invoke;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.util.Providers;

/**
 * Represents an inlining opportunity where the current class hierarchy leads to a monomorphic
 * target method, but for which an assumption has to be registered because of non-final classes.
 */
// @class AssumptionInlineInfo
public final class AssumptionInlineInfo extends ExactInlineInfo
{
    private final AssumptionResult<?> takenAssumption;

    // @cons
    public AssumptionInlineInfo(Invoke invoke, ResolvedJavaMethod concrete, AssumptionResult<?> takenAssumption)
    {
        super(invoke, concrete);
        this.takenAssumption = takenAssumption;
    }

    @Override
    public EconomicSet<Node> inline(Providers providers, String reason)
    {
        takenAssumption.recordTo(invoke.asNode().graph().getAssumptions());
        return super.inline(providers, reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers providers)
    {
        takenAssumption.recordTo(invoke.asNode().graph().getAssumptions());
        InliningUtil.replaceInvokeCallTarget(invoke, graph(), InvokeKind.Special, concrete);
    }

    @Override
    public String toString()
    {
        return "assumption " + concrete.format("%H.%n(%p):%r");
    }
}
