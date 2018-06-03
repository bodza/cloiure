package giraaff.nodes.spi;

import giraaff.nodes.ValueNode;

// @iface ArrayLengthProvider
public interface ArrayLengthProvider
{
    ///
    // @return the length of the array described by this node, or null if it is not available
    ///
    ValueNode length();
}
