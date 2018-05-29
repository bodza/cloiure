package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.AddressNode.Address;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class PrefetchAllocateNode
public final class PrefetchAllocateNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<PrefetchAllocateNode> TYPE = NodeClass.create(PrefetchAllocateNode.class);

    @Input(InputType.Association) AddressNode address;

    // @cons
    public PrefetchAllocateNode(ValueNode address)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = (AddressNode) address;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitPrefetchAllocate(gen.operand(address));
    }

    @NodeIntrinsic
    public static native void prefetch(Address address);
}
