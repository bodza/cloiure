package giraaff.nodes.spi;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.graph.Node;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

/**
 * Provides a capability for replacing a higher node with one or more lower level nodes.
 */
public interface LoweringProvider extends ArrayOffsetProvider
{
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
    default Integer smallestCompareWidth()
    {
        // most platforms only support 32 and 64 bit compares
        return 32;
    }
}
