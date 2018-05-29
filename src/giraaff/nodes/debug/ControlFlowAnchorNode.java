package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class ControlFlowAnchorNode
public final class ControlFlowAnchorNode extends FixedWithNextNode implements LIRLowerable, ControlFlowAnchored
{
    public static final NodeClass<ControlFlowAnchorNode> TYPE = NodeClass.create(ControlFlowAnchorNode.class);

    // @class ControlFlowAnchorNode.Unique
    private static final class Unique
    {
    }

    protected Unique unique;

    // @cons
    public ControlFlowAnchorNode()
    {
        super(TYPE, StampFactory.forVoid());
        this.unique = new Unique();
    }

    /**
     * Used by MacroSubstitution.
     */
    // @cons
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
