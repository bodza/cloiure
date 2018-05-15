package graalvm.compiler.nodes.spi;

import graalvm.compiler.nodes.ValueNode;

public interface ArrayLengthProvider {

    /**
     * @return the length of the array described by this node, or null if it is not available
     */
    ValueNode length();
}
