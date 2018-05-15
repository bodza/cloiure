package graalvm.compiler.replacements;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_256;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.replacements.nodes.MacroStateSplitNode;

import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo(size = SIZE_64, cycles = CYCLES_256)
public class StringIndexOfNode extends MacroStateSplitNode {
    public static final NodeClass<StringIndexOfNode> TYPE = NodeClass.create(StringIndexOfNode.class);

    public StringIndexOfNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments) {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }
}
