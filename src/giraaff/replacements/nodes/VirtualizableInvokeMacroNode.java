package giraaff.replacements.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * A helper class to allow elimination of byte code instrumentation that could interfere with escape
 * analysis.
 */
public class VirtualizableInvokeMacroNode extends MacroStateSplitNode implements Virtualizable
{
    public static final NodeClass<VirtualizableInvokeMacroNode> TYPE = NodeClass.create(VirtualizableInvokeMacroNode.class);

    public VirtualizableInvokeMacroNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        for (ValueNode arg : arguments)
        {
            ValueNode alias = tool.getAlias(arg);
            if (alias instanceof VirtualObjectNode)
            {
                tool.delete();
            }
        }
    }
}
