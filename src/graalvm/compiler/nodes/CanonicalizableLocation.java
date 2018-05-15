package graalvm.compiler.nodes;

import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.memory.address.AddressNode;

public interface CanonicalizableLocation {
    ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool);
}
