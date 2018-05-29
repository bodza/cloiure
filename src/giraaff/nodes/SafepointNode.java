package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Marks a position in the graph where a safepoint should be emitted.
 */
// @class SafepointNode
public final class SafepointNode extends DeoptimizingFixedWithNextNode implements Lowerable, LIRLowerable
{
    public static final NodeClass<SafepointNode> TYPE = NodeClass.create(SafepointNode.class);

    // @cons
    public SafepointNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.visitSafepointNode(this);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
