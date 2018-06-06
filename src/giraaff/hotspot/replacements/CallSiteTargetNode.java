package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.nodes.MacroStateSplitNode;

// @class CallSiteTargetNode
public final class CallSiteTargetNode extends MacroStateSplitNode implements Canonicalizable, Lowerable
{
    // @def
    public static final NodeClass<CallSiteTargetNode> TYPE = NodeClass.create(CallSiteTargetNode.class);

    // @cons CallSiteTargetNode
    public CallSiteTargetNode(CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode __receiver)
    {
        super(TYPE, __invokeKind, __targetMethod, __bci, __returnStamp, __receiver);
    }

    private ValueNode getCallSite()
    {
        return this.___arguments.get(0);
    }

    public static ConstantNode tryFold(ValueNode __callSite, MetaAccessProvider __metaAccess, Assumptions __assumptions)
    {
        if (__callSite != null && __callSite.isConstant() && !__callSite.isNullConstant())
        {
            HotSpotObjectConstant __c = (HotSpotObjectConstant) __callSite.asConstant();
            JavaConstant __target = __c.getCallSiteTarget(__assumptions);
            if (__target != null)
            {
                return ConstantNode.forConstant(__target, __metaAccess);
            }
        }
        return null;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        ConstantNode __target = tryFold(getCallSite(), __tool.getMetaAccess(), graph().getAssumptions());
        if (__target != null)
        {
            return __target;
        }

        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        ConstantNode __target = tryFold(getCallSite(), __tool.getMetaAccess(), graph().getAssumptions());

        if (__target != null)
        {
            graph().replaceFixedWithFloating(this, __target);
        }
        else
        {
            InvokeNode __invoke = createInvoke();
            graph().replaceFixedWithFixed(this, __invoke);
            __invoke.lower(__tool);
        }
    }
}
