package giraaff.nodes.memory;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.StructuralInput;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class MemoryAnchorNode
public final class MemoryAnchorNode extends FixedWithNextNode implements LIRLowerable, MemoryNode, Canonicalizable
{
    // @def
    public static final NodeClass<MemoryAnchorNode> TYPE = NodeClass.create(MemoryAnchorNode.class);

    // @cons MemoryAnchorNode
    public MemoryAnchorNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // Nothing to emit, since this node is used for structural purposes only.
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        return __tool.allUsagesAvailable() && hasNoUsages() ? null : this;
    }

    @Node.NodeIntrinsic
    public static native StructuralInput.Memory anchor();
}
