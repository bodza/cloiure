package giraaff.phases.common;

import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

// @class AddressLoweringPhase
public final class AddressLoweringPhase extends Phase
{
    // @class AddressLoweringPhase.AddressLowering
    public abstract static class AddressLowering
    {
        @SuppressWarnings("unused")
        public void preProcess(StructuredGraph __graph)
        {
        }

        @SuppressWarnings("unused")
        public void postProcess(AddressNode __lowered)
        {
        }

        public abstract AddressNode lower(ValueNode base, ValueNode offset);
    }

    // @field
    private final AddressLowering lowering;

    // @cons
    public AddressLoweringPhase(AddressLowering __lowering)
    {
        super();
        this.lowering = __lowering;
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        lowering.preProcess(__graph);
        for (Node __node : __graph.getNodes())
        {
            AddressNode __lowered;
            if (__node instanceof OffsetAddressNode)
            {
                OffsetAddressNode __address = (OffsetAddressNode) __node;
                __lowered = lowering.lower(__address.getBase(), __address.getOffset());
                lowering.postProcess(__lowered);
            }
            else
            {
                continue;
            }
            __node.replaceAtUsages(__lowered);
            GraphUtil.killWithUnusedFloatingInputs(__node);
        }
    }
}
