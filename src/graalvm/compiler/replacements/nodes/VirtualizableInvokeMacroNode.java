package graalvm.compiler.replacements.nodes;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * A helper class to allow elimination of byte code instrumentation that could interfere with escape
 * analysis.
 */
@NodeInfo
public class VirtualizableInvokeMacroNode extends MacroStateSplitNode implements Virtualizable {

    public static final NodeClass<VirtualizableInvokeMacroNode> TYPE = NodeClass.create(VirtualizableInvokeMacroNode.class);

    public VirtualizableInvokeMacroNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments) {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        for (ValueNode arg : arguments) {
            ValueNode alias = tool.getAlias(arg);
            if (alias instanceof VirtualObjectNode) {
                tool.delete();
            }
        }
    }
}
