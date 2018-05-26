package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

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
    public void generate(NodeLIRBuilderTool gen)
    {
        // do nothing
    }

    @Override
    protected void afterClone(Node other)
    {
    }
}
