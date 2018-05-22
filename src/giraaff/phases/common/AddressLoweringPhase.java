package giraaff.phases.common;

import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

public class AddressLoweringPhase extends Phase
{
    public abstract static class AddressLowering
    {
        @SuppressWarnings("unused")
        public void preProcess(StructuredGraph graph)
        {
        }

        @SuppressWarnings("unused")
        public void postProcess(AddressNode lowered)
        {
        }

        public abstract AddressNode lower(ValueNode base, ValueNode offset);
    }

    private final AddressLowering lowering;

    public AddressLoweringPhase(AddressLowering lowering)
    {
        this.lowering = lowering;
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        lowering.preProcess(graph);
        for (Node node : graph.getNodes())
        {
            AddressNode lowered;
            if (node instanceof OffsetAddressNode)
            {
                OffsetAddressNode address = (OffsetAddressNode) node;
                lowered = lowering.lower(address.getBase(), address.getOffset());
                lowering.postProcess(lowered);
            }
            else
            {
                continue;
            }
            node.replaceAtUsages(lowered);
            GraphUtil.killWithUnusedFloatingInputs(node);
        }
    }
}
