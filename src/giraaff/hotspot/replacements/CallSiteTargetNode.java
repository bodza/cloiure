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
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.nodes.MacroStateSplitNode;

// @class CallSiteTargetNode
public final class CallSiteTargetNode extends MacroStateSplitNode implements Canonicalizable, Lowerable
{
    public static final NodeClass<CallSiteTargetNode> TYPE = NodeClass.create(CallSiteTargetNode.class);

    // @cons
    public CallSiteTargetNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode receiver)
    {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, receiver);
    }

    private ValueNode getCallSite()
    {
        return arguments.get(0);
    }

    public static ConstantNode tryFold(ValueNode callSite, MetaAccessProvider metaAccess, Assumptions assumptions)
    {
        if (callSite != null && callSite.isConstant() && !callSite.isNullConstant())
        {
            HotSpotObjectConstant c = (HotSpotObjectConstant) callSite.asConstant();
            JavaConstant target = c.getCallSiteTarget(assumptions);
            if (target != null)
            {
                return ConstantNode.forConstant(target, metaAccess);
            }
        }
        return null;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        ConstantNode target = tryFold(getCallSite(), tool.getMetaAccess(), graph().getAssumptions());
        if (target != null)
        {
            return target;
        }

        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        ConstantNode target = tryFold(getCallSite(), tool.getMetaAccess(), graph().getAssumptions());

        if (target != null)
        {
            graph().replaceFixedWithFloating(this, target);
        }
        else
        {
            InvokeNode invoke = createInvoke();
            graph().replaceFixedWithFixed(this, invoke);
            invoke.lower(tool);
        }
    }
}
