package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode;
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

///
// Represents an inlining opportunity for which profiling information suggests a monomorphic
// receiver, but for which the receiver type cannot be proven. A type check guard will be generated
// if this inlining is performed.
///
// @class TypeGuardInlineInfo
public final class TypeGuardInlineInfo extends AbstractInlineInfo
{
    // @field
    private final ResolvedJavaMethod ___concrete;
    // @field
    private final ResolvedJavaType ___type;
    // @field
    private Inlineable ___inlineableElement;

    // @cons TypeGuardInlineInfo
    public TypeGuardInlineInfo(Invoke __invoke, ResolvedJavaMethod __concrete, ResolvedJavaType __type)
    {
        super(__invoke);
        this.___concrete = __concrete;
        this.___type = __type;
    }

    @Override
    public int numberOfMethods()
    {
        return 1;
    }

    @Override
    public ResolvedJavaMethod methodAt(int __index)
    {
        return this.___concrete;
    }

    @Override
    public Inlineable inlineableElementAt(int __index)
    {
        return this.___inlineableElement;
    }

    @Override
    public double probabilityAt(int __index)
    {
        return 1.0;
    }

    @Override
    public double relevanceAt(int __index)
    {
        return 1.0;
    }

    @Override
    public void setInlinableElement(int __index, Inlineable __inlineableElement)
    {
        this.___inlineableElement = __inlineableElement;
    }

    @Override
    public EconomicSet<Node> inline(Providers __providers, String __reason)
    {
        createGuard(graph(), __providers);
        return inline(this.___invoke, this.___concrete, this.___inlineableElement, false, __reason);
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers __providers)
    {
        createGuard(graph(), __providers);
        InliningUtil.replaceInvokeCallTarget(this.___invoke, graph(), CallTargetNode.InvokeKind.Special, this.___concrete);
    }

    private void createGuard(StructuredGraph __graph, Providers __providers)
    {
        ValueNode __nonNullReceiver = InliningUtil.nonNullReceiver(this.___invoke);
        LoadHubNode __receiverHub = __graph.unique(new LoadHubNode(__providers.getStampProvider(), __nonNullReceiver));
        ConstantNode __typeHub = ConstantNode.forConstant(__receiverHub.stamp(NodeView.DEFAULT), __providers.getConstantReflection().asObjectHub(this.___type), __providers.getMetaAccess(), __graph);

        LogicNode __typeCheck = CompareNode.createCompareNode(__graph, CanonicalCondition.EQ, __receiverHub, __typeHub, __providers.getConstantReflection(), NodeView.DEFAULT);
        FixedGuardNode __guard = __graph.add(new FixedGuardNode(__typeCheck, DeoptimizationReason.TypeCheckedInliningViolated, DeoptimizationAction.InvalidateReprofile));

        ValueNode __anchoredReceiver = InliningUtil.createAnchoredReceiver(__graph, __guard, this.___type, __nonNullReceiver, true);
        this.___invoke.callTarget().replaceFirstInput(__nonNullReceiver, __anchoredReceiver);

        __graph.addBeforeFixed(this.___invoke.asNode(), __guard);
    }

    @Override
    public boolean shouldInline()
    {
        return this.___concrete.shouldBeInlined();
    }
}
