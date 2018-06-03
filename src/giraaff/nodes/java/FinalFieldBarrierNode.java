package giraaff.nodes.java;

import jdk.vm.ci.code.MemoryBarriers;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class FinalFieldBarrierNode
public final class FinalFieldBarrierNode extends FixedWithNextNode implements Virtualizable, Lowerable
{
    // @def
    public static final NodeClass<FinalFieldBarrierNode> TYPE = NodeClass.create(FinalFieldBarrierNode.class);

    @OptionalInput
    // @field
    private ValueNode ___value;

    // @cons
    public FinalFieldBarrierNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.___value = __value;
    }

    public ValueNode getValue()
    {
        return this.___value;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        if (this.___value != null && __tool.getAlias(this.___value) instanceof VirtualObjectNode)
        {
            __tool.delete();
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        graph().replaceFixedWithFixed(this, graph().add(new MembarNode(MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE)));
    }
}
