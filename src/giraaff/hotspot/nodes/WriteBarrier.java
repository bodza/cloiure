package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class WriteBarrier
public abstract class WriteBarrier extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<WriteBarrier> TYPE = NodeClass.create(WriteBarrier.class);

    // @cons
    protected WriteBarrier(NodeClass<? extends WriteBarrier> c)
    {
        super(c, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }
}
