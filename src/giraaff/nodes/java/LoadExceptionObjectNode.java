package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractStateSplit;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class LoadExceptionObjectNode
public final class LoadExceptionObjectNode extends AbstractStateSplit implements Lowerable
{
    // @def
    public static final NodeClass<LoadExceptionObjectNode> TYPE = NodeClass.create(LoadExceptionObjectNode.class);

    // @cons LoadExceptionObjectNode
    public LoadExceptionObjectNode(Stamp __stamp)
    {
        super(TYPE, __stamp);
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
