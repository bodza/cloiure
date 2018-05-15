package graalvm.compiler.nodes;

import graalvm.compiler.graph.NodeInterface;

public interface ValueNodeInterface extends NodeInterface {
    @Override
    ValueNode asNode();
}
