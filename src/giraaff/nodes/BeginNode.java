package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;

public final class BeginNode extends AbstractBeginNode implements Simplifiable
{
    public static final NodeClass<BeginNode> TYPE = NodeClass.create(BeginNode.class);

    public BeginNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    public BeginNode(Stamp stamp)
    {
        super(TYPE, stamp);
    }

    public void trySimplify()
    {
        FixedNode prev = (FixedNode) this.predecessor();
        if (prev instanceof ControlSplitNode)
        {
            // This begin node is necessary.
        }
        else
        {
            // This begin node can be removed and all guards moved up to the preceding begin node.
            prepareDelete();
            graph().removeFixed(this);
        }
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        FixedNode prev = (FixedNode) this.predecessor();
        if (prev == null)
        {
            // This is the start node.
        }
        else if (prev instanceof ControlSplitNode)
        {
            // This begin node is necessary.
        }
        else
        {
            // This begin node can be removed and all guards moved up to the preceding begin node.
            prepareDelete();
            tool.addToWorkList(next());
            graph().removeFixed(this);
        }
    }

    public static AbstractBeginNode begin(FixedNode with)
    {
        if (with instanceof AbstractBeginNode)
        {
            return (AbstractBeginNode) with;
        }
        BeginNode begin = with.graph().add(new BeginNode());
        begin.setNext(with);
        return begin;
    }
}
