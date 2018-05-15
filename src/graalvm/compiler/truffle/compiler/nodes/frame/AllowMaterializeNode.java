package graalvm.compiler.truffle.compiler.nodes.frame;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

/**
 * Intrinsic node for materializing a Truffle frame.
 */
@NodeInfo(nameTemplate = "AllowMaterialize{p#frame/s}", cycles = CYCLES_0, size = SIZE_0)
public final class AllowMaterializeNode extends FixedWithNextNode implements IterableNodeType, Virtualizable {

    public static final NodeClass<AllowMaterializeNode> TYPE = NodeClass.create(AllowMaterializeNode.class);
    @Input ValueNode frame;

    public AllowMaterializeNode(ValueNode frame) {
        super(TYPE, frame.stamp(NodeView.DEFAULT));
        this.frame = frame;
    }

    public ValueNode getFrame() {
        return frame;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(frame);
        if (alias instanceof VirtualObjectNode) {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            tool.setEnsureVirtualized(virtual, false);
            tool.replaceWithVirtual(virtual);
        } else {
            tool.replaceWithValue(alias);
        }
    }
}
