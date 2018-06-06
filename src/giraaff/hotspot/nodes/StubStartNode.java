package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.hotspot.stubs.Stub;
import giraaff.nodes.StartNode;

///
// Start node for a {@link Stub}'s graph.
///
// @class StubStartNode
public final class StubStartNode extends StartNode
{
    // @def
    public static final NodeClass<StubStartNode> TYPE = NodeClass.create(StubStartNode.class);

    // @field
    protected final Stub ___stub;

    // @cons StubStartNode
    public StubStartNode(Stub __stub)
    {
        super(TYPE);
        this.___stub = __stub;
    }

    public Stub getStub()
    {
        return this.___stub;
    }
}
