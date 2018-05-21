package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.stubs.Stub;
import graalvm.compiler.nodes.StartNode;

/**
 * Start node for a {@link Stub}'s graph.
 */
public final class StubStartNode extends StartNode
{
    public static final NodeClass<StubStartNode> TYPE = NodeClass.create(StubStartNode.class);
    protected final Stub stub;

    public StubStartNode(Stub stub)
    {
        super(TYPE);
        this.stub = stub;
    }

    public Stub getStub()
    {
        return stub;
    }
}
