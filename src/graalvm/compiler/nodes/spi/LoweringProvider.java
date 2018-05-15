package graalvm.compiler.nodes.spi;

import graalvm.compiler.core.common.spi.ArrayOffsetProvider;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

import jdk.vm.ci.meta.JavaKind;

/**
 * Provides a capability for replacing a higher node with one or more lower level nodes.
 */
public interface LoweringProvider extends ArrayOffsetProvider {

    void lower(Node n, LoweringTool tool);

    /**
     * Reconstructs the array index from an address node that was created as a lowering of an
     * indexed access to an array.
     *
     * @param elementKind the {@link JavaKind} of the array elements
     * @param address an {@link AddressNode} pointing to an element in an array
     * @return a node that gives the index of the element
     */
    ValueNode reconstructArrayIndex(JavaKind elementKind, AddressNode address);

    /**
     * Indicates the smallest width for comparing an integer value on the target platform.
     */
    default Integer smallestCompareWidth() {
        // most platforms only support 32 and 64 bit compares
        return 32;
    }
}
