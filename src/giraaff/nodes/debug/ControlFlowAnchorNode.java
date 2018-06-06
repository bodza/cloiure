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
    // @def
    public static final NodeClass<ControlFlowAnchorNode> TYPE = NodeClass.create(ControlFlowAnchorNode.class);

    // @class ControlFlowAnchorNode.Unique
    private static final class Unique
    {
    }

    // @field
    protected ControlFlowAnchorNode.Unique ___unique;

    // @cons ControlFlowAnchorNode
    public ControlFlowAnchorNode()
    {
        super(TYPE, StampFactory.forVoid());
        this.___unique = new ControlFlowAnchorNode.Unique();
    }

    ///
    // Used by MacroSubstitution.
    ///
    // @cons ControlFlowAnchorNode
    public ControlFlowAnchorNode(@SuppressWarnings("unused") Invoke __invoke)
    {
        this();
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // do nothing
    }

    @Override
    protected void afterClone(Node __other)
    {
    }
}
