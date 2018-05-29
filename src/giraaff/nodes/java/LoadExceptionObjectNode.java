package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractStateSplit;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class LoadExceptionObjectNode
public final class LoadExceptionObjectNode extends AbstractStateSplit implements Lowerable
{
    public static final NodeClass<LoadExceptionObjectNode> TYPE = NodeClass.create(LoadExceptionObjectNode.class);

    // @cons
    public LoadExceptionObjectNode(Stamp stamp)
    {
        super(TYPE, stamp);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
