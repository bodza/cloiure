package giraaff.replacements.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// A helper class to allow elimination of byte code instrumentation that could interfere with escape analysis.
///
// @class VirtualizableInvokeMacroNode
public final class VirtualizableInvokeMacroNode extends MacroStateSplitNode implements Virtualizable
{
    // @def
    public static final NodeClass<VirtualizableInvokeMacroNode> TYPE = NodeClass.create(VirtualizableInvokeMacroNode.class);

    // @cons VirtualizableInvokeMacroNode
    public VirtualizableInvokeMacroNode(CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(TYPE, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        for (ValueNode __arg : this.___arguments)
        {
            ValueNode __alias = __tool.getAlias(__arg);
            if (__alias instanceof VirtualObjectNode)
            {
                __tool.delete();
            }
        }
    }
}
