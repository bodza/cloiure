package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class WriteBarrier
public abstract class WriteBarrier extends FixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<WriteBarrier> TYPE = NodeClass.create(WriteBarrier.class);

    // @cons WriteBarrier
    protected WriteBarrier(NodeClass<? extends WriteBarrier> __c)
    {
        super(__c, StampFactory.forVoid());
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }
}
