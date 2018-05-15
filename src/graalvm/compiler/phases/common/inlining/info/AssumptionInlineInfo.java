package graalvm.compiler.phases.common.inlining.info;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.util.Providers;

import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Represents an inlining opportunity where the current class hierarchy leads to a monomorphic
 * target method, but for which an assumption has to be registered because of non-final classes.
 */
public class AssumptionInlineInfo extends ExactInlineInfo {

    private final AssumptionResult<?> takenAssumption;

    public AssumptionInlineInfo(Invoke invoke, ResolvedJavaMethod concrete, AssumptionResult<?> takenAssumption) {
        super(invoke, concrete);
        this.takenAssumption = takenAssumption;
    }

    @Override
    public EconomicSet<Node> inline(Providers providers, String reason) {
        takenAssumption.recordTo(invoke.asNode().graph().getAssumptions());
        return super.inline(providers, reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers providers) {
        takenAssumption.recordTo(invoke.asNode().graph().getAssumptions());
        InliningUtil.replaceInvokeCallTarget(invoke, graph(), InvokeKind.Special, concrete);
    }

    @Override
    public String toString() {
        return "assumption " + concrete.format("%H.%n(%p):%r");
    }
}
