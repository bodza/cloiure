package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.stubs.Stub;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.StartNode;

/**
 * Start node for a {@link Stub}'s graph.
 */
@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
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
