package giraaff.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.nodes.MacroStateSplitNode;

public class StringIndexOfNode extends MacroStateSplitNode
{
    public static final NodeClass<StringIndexOfNode> TYPE = NodeClass.create(StringIndexOfNode.class);

    public StringIndexOfNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments)
    {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
