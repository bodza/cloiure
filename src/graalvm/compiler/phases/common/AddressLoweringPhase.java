package graalvm.compiler.phases.common;

import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;

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
        assert lowering != null;
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
