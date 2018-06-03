package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.Invoke;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.util.Providers;

///
// Represents an inlining opportunity where the current class hierarchy leads to a monomorphic
// target method, but for which an assumption has to be registered because of non-final classes.
///
// @class AssumptionInlineInfo
public final class AssumptionInlineInfo extends ExactInlineInfo
{
    // @field
    private final AssumptionResult<?> ___takenAssumption;

    // @cons
    public AssumptionInlineInfo(Invoke __invoke, ResolvedJavaMethod __concrete, AssumptionResult<?> __takenAssumption)
    {
        super(__invoke, __concrete);
        this.___takenAssumption = __takenAssumption;
    }

    @Override
    public EconomicSet<Node> inline(Providers __providers, String __reason)
    {
        this.___takenAssumption.recordTo(this.___invoke.asNode().graph().getAssumptions());
        return super.inline(__providers, __reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers __providers)
    {
        this.___takenAssumption.recordTo(this.___invoke.asNode().graph().getAssumptions());
        InliningUtil.replaceInvokeCallTarget(this.___invoke, graph(), InvokeKind.Special, this.___concrete);
    }
}
