package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.replacements.nodes.MacroStateSplitNode;

public final class CallSiteTargetNode extends MacroStateSplitNode implements Canonicalizable, Lowerable
{
    public static final NodeClass<CallSiteTargetNode> TYPE = NodeClass.create(CallSiteTargetNode.class);

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
