package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.hotspot.stubs.Stub;
import giraaff.nodes.StartNode;

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
