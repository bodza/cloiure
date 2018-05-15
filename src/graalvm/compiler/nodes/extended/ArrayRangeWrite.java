package graalvm.compiler.nodes.extended;

import graalvm.compiler.graph.NodeInterface;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

public interface ArrayRangeWrite extends NodeInterface {
    AddressNode getAddress();

    /**
     * The length of the modified range.
     */
    ValueNode getLength();

    /**
     * Return true if the written array is an object array, false if it is a primitive array.
     */
    boolean writesObjectArray();

    /**
     * Returns whether this write is the initialization of the written location. If it is true, the
     * old value of the memory location is either uninitialized or zero. If it is false, the memory
     * location is guaranteed to contain a valid value or zero.
     */
    boolean isInitialization();

    int getElementStride();

    @Override
    FixedWithNextNode asNode();
}
