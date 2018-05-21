package graalvm.compiler.nodes.debug;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class ControlFlowAnchorNode extends FixedWithNextNode implements LIRLowerable, ControlFlowAnchored
{
    public static final NodeClass<ControlFlowAnchorNode> TYPE = NodeClass.create(ControlFlowAnchorNode.class);

    private static class Unique
    {
    }

    protected Unique unique;

    public ControlFlowAnchorNode()
    {
        super(TYPE, StampFactory.forVoid());
        this.unique = new Unique();
    }

    /**
     * Used by MacroSubstitution.
     */
    public ControlFlowAnchorNode(@SuppressWarnings("unused") Invoke invoke)
    {
        this();
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // do nothing
    }

    @Override
    protected void afterClone(Node other)
    {
    }
}
