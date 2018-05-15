package graalvm.compiler.nodes.memory;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.StructuralInput.Memory;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(allowedUsageTypes = Memory, cycles = CYCLES_0, size = SIZE_0)
public final class MemoryAnchorNode extends FixedWithNextNode implements LIRLowerable, MemoryNode, Canonicalizable {

    public static final NodeClass<MemoryAnchorNode> TYPE = NodeClass.create(MemoryAnchorNode.class);

    public MemoryAnchorNode() {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // Nothing to emit, since this node is used for structural purposes only.
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        return tool.allUsagesAvailable() && hasNoUsages() ? null : this;
    }

    @NodeIntrinsic
    public static native Memory anchor();
}
