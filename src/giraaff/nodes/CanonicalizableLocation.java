package giraaff.nodes;

import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.memory.address.AddressNode;

// @iface CanonicalizableLocation
public interface CanonicalizableLocation
{
    ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool);
}
