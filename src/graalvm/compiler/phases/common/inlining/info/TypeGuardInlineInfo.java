package graalvm.compiler.phases.common.inlining.info;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.extended.LoadHubNode;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.info.elem.Inlineable;
import graalvm.compiler.phases.util.Providers;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

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
        assert type.isArray() || type.isConcrete() : type;
    }

    @Override
    public int numberOfMethods()
    {
        return 1;
    }

    @Override
    public ResolvedJavaMethod methodAt(int index)
    {
        assert index == 0;
        return concrete;
    }

    @Override
    public Inlineable inlineableElementAt(int index)
    {
        assert index == 0;
        return inlineableElement;
    }

    @Override
    public double probabilityAt(int index)
    {
        assert index == 0;
        return 1.0;
    }

    @Override
    public double relevanceAt(int index)
    {
        assert index == 0;
        return 1.0;
    }

    @Override
    public void setInlinableElement(int index, Inlineable inlineableElement)
    {
        assert index == 0;
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

    @SuppressWarnings("try")
    private void createGuard(StructuredGraph graph, Providers providers)
    {
        try (DebugCloseable context = invoke.asNode().withNodeSourcePosition())
        {
            ValueNode nonNullReceiver = InliningUtil.nonNullReceiver(invoke);
            LoadHubNode receiverHub = graph.unique(new LoadHubNode(providers.getStampProvider(), nonNullReceiver));
            ConstantNode typeHub = ConstantNode.forConstant(receiverHub.stamp(NodeView.DEFAULT), providers.getConstantReflection().asObjectHub(type), providers.getMetaAccess(), graph);

            LogicNode typeCheck = CompareNode.createCompareNode(graph, CanonicalCondition.EQ, receiverHub, typeHub, providers.getConstantReflection(), NodeView.DEFAULT);
            FixedGuardNode guard = graph.add(new FixedGuardNode(typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile));
            assert invoke.predecessor() != null;

            ValueNode anchoredReceiver = InliningUtil.createAnchoredReceiver(graph, guard, type, nonNullReceiver, true);
            invoke.callTarget().replaceFirstInput(nonNullReceiver, anchoredReceiver);

            graph.addBeforeFixed(invoke.asNode(), guard);
        }
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
