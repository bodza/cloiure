package giraaff.nodes;

import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.memory.address.AddressNode;

public interface CanonicalizableLocation
{
    ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool);
}
