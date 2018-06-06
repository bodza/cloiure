package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class PrefetchAllocateNode
public final class PrefetchAllocateNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<PrefetchAllocateNode> TYPE = NodeClass.create(PrefetchAllocateNode.class);

    @Node.Input(InputType.Association)
    // @field
    AddressNode ___address;

    // @cons PrefetchAllocateNode
    public PrefetchAllocateNode(ValueNode __address)
    {
        super(TYPE, StampFactory.forVoid());
        this.___address = (AddressNode) __address;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitPrefetchAllocate(__gen.operand(this.___address));
    }

    @Node.NodeIntrinsic
    public static native void prefetch(AddressNode.Address __address);
}
