package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code AbstractNewObjectNode} is the base class for the new instance and new array nodes.
 */
// @class AbstractNewObjectNode
public abstract class AbstractNewObjectNode extends DeoptimizingFixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<AbstractNewObjectNode> TYPE = NodeClass.create(AbstractNewObjectNode.class);

    // @field
    protected final boolean fillContents;

    // @cons
    protected AbstractNewObjectNode(NodeClass<? extends AbstractNewObjectNode> __c, Stamp __stamp, boolean __fillContents, FrameState __stateBefore)
    {
        super(__c, __stamp, __stateBefore);
        this.fillContents = __fillContents;
    }

    /**
     * @return <code>true</code> if the object's contents should be initialized to zero/null.
     */
    public boolean fillContents()
    {
        return fillContents;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
