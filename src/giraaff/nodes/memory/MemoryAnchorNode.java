package giraaff.nodes.memory;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.StructuralInput.Memory;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

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
