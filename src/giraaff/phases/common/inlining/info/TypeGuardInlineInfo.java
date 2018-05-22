package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.util.Providers;

/**
 * Represents an inlining opportunity for which profiling information suggests a monomorphic
 * receiver, but for which the receiver type cannot be proven. A type check guard will be generated
 * if this inlining is performed.
 */
public class TypeGuardInlineInfo extends AbstractInlineInfo
{
    private final ResolvedJavaMethod concrete;
    private final ResolvedJavaType type;
    private Inlineable inlineableElement;

    public TypeGuardInlineInfo(Invoke invoke, ResolvedJavaMethod concrete, ResolvedJavaType type)
    {
        super(invoke);
        this.concrete = concrete;
        this.type = type;
    }

    @Override
    public int numberOfMethods()
    {
        return 1;
    }

    @Override
    public ResolvedJavaMethod methodAt(int index)
    {
        return concrete;
    }

    @Override
    public Inlineable inlineableElementAt(int index)
    {
        return inlineableElement;
    }

    @Override
    public double probabilityAt(int index)
    {
        return 1.0;
    }

    @Override
    public double relevanceAt(int index)
    {
        return 1.0;
    }

    @Override
    public void setInlinableElement(int index, Inlineable inlineableElement)
    {
        this.inlineableElement = inlineableElement;
    }

    @Override
    public EconomicSet<Node> inline(Providers providers, String reason)
    {
        createGuard(graph(), providers);
        return inline(invoke, concrete, inlineableElement, false, reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers providers)
    {
        createGuard(graph(), providers);
        InliningUtil.replaceInvokeCallTarget(invoke, graph(), InvokeKind.Special, concrete);
    }

    private void createGuard(StructuredGraph graph, Providers providers)
    {
        ValueNode nonNullReceiver = InliningUtil.nonNullReceiver(invoke);
        LoadHubNode receiverHub = graph.unique(new LoadHubNode(providers.getStampProvider(), nonNullReceiver));
        ConstantNode typeHub = ConstantNode.forConstant(receiverHub.stamp(NodeView.DEFAULT), providers.getConstantReflection().asObjectHub(type), providers.getMetaAccess(), graph);

        LogicNode typeCheck = CompareNode.createCompareNode(graph, CanonicalCondition.EQ, receiverHub, typeHub, providers.getConstantReflection(), NodeView.DEFAULT);
        FixedGuardNode guard = graph.add(new FixedGuardNode(typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile));

        ValueNode anchoredReceiver = InliningUtil.createAnchoredReceiver(graph, guard, type, nonNullReceiver, true);
        invoke.callTarget().replaceFirstInput(nonNullReceiver, anchoredReceiver);

        graph.addBeforeFixed(invoke.asNode(), guard);
    }

    @Override
    public String toString()
    {
        return "type-checked with type " + type.getName() + " and method " + concrete.format("%H.%n(%p):%r");
    }

    @Override
    public boolean shouldInline()
    {
        return concrete.shouldBeInlined();
    }
}
