package graalvm.compiler.nodes.memory;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.StructuralInput.Memory;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class MemoryAnchorNode extends FixedWithNextNode implements LIRLowerable, MemoryNode, Canonicalizable
{
    public static final NodeClass<MemoryAnchorNode> TYPE = NodeClass.create(MemoryAnchorNode.class);

    public MemoryAnchorNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // Nothing to emit, since this node is used for structural purposes only.
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        return tool.allUsagesAvailable() && hasNoUsages() ? null : this;
    }

    @NodeIntrinsic
    public static native Memory anchor();
}
