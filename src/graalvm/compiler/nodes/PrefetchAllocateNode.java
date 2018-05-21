package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.memory.address.AddressNode.Address;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class PrefetchAllocateNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<PrefetchAllocateNode> TYPE = NodeClass.create(PrefetchAllocateNode.class);
    @Input(Association) AddressNode address;

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
