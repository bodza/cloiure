package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.Lowerable;

@NodeInfo
public abstract class ArrayRangeWriteBarrier extends WriteBarrier implements Lowerable {

    public static final NodeClass<ArrayRangeWriteBarrier> TYPE = NodeClass.create(ArrayRangeWriteBarrier.class);
    @Input(InputType.Association) AddressNode address;
    @Input ValueNode length;

    private final int elementStride;

    protected ArrayRangeWriteBarrier(NodeClass<? extends ArrayRangeWriteBarrier> c, AddressNode address, ValueNode length, int elementStride) {
        super(c);
        this.address = address;
        this.length = length;
        this.elementStride = elementStride;
    }

    public AddressNode getAddress() {
        return address;
    }

    public ValueNode getLength() {
        return length;
    }

    public int getElementStride() {
        return elementStride;
    }
}
