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

public class FinalFieldBarrierNode extends FixedWithNextNode implements Virtualizable, Lowerable
{
    public static final NodeClass<FinalFieldBarrierNode> TYPE = NodeClass.create(FinalFieldBarrierNode.class);

    @OptionalInput private ValueNode value;

    public FinalFieldBarrierNode(ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        if (value != null && tool.getAlias(value) instanceof VirtualObjectNode)
        {
            tool.delete();
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        graph().replaceFixedWithFixed(this, graph().add(new MembarNode(MemoryBarriers.LOAD_STORE | MemoryBarriers.STORE_STORE)));
    }
}
