package graalvm.compiler.nodes.java;

import static jdk.vm.ci.code.MemoryBarriers.LOAD_STORE;
import static jdk.vm.ci.code.MemoryBarriers.STORE_STORE;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.MembarNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

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
        graph().replaceFixedWithFixed(this, graph().add(new MembarNode(LOAD_STORE | STORE_STORE)));
    }
}
