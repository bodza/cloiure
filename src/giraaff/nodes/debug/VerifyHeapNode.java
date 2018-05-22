package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * A node for platform dependent verification of the Java heap. Intended to be used for debugging
 * heap corruption issues.
 */
public final class VerifyHeapNode extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<VerifyHeapNode> TYPE = NodeClass.create(VerifyHeapNode.class);

    public VerifyHeapNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public static void addBefore(FixedNode position)
    {
        StructuredGraph graph = position.graph();
        graph.addBeforeFixed(position, graph.add(new VerifyHeapNode()));
    }

    public static void addAfter(FixedWithNextNode position)
    {
        StructuredGraph graph = position.graph();
        graph.addAfterFixed(position, graph.add(new VerifyHeapNode()));
    }
}
