package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;

// @class BeginNode
public final class BeginNode extends AbstractBeginNode implements Simplifiable
{
    // @def
    public static final NodeClass<BeginNode> TYPE = NodeClass.create(BeginNode.class);

    // @cons
    public BeginNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    // @cons
    public BeginNode(Stamp __stamp)
    {
        super(TYPE, __stamp);
    }

    public void trySimplify()
    {
        FixedNode __prev = (FixedNode) this.predecessor();
        if (__prev instanceof ControlSplitNode)
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
    public void simplify(SimplifierTool __tool)
    {
        FixedNode __prev = (FixedNode) this.predecessor();
        if (__prev == null)
        {
            // This is the start node.
        }
        else if (__prev instanceof ControlSplitNode)
        {
            // This begin node is necessary.
        }
        else
        {
            // This begin node can be removed and all guards moved up to the preceding begin node.
            prepareDelete();
            __tool.addToWorkList(next());
            graph().removeFixed(this);
        }
    }

    public static AbstractBeginNode begin(FixedNode __with)
    {
        if (__with instanceof AbstractBeginNode)
        {
            return (AbstractBeginNode) __with;
        }
        BeginNode __begin = __with.graph().add(new BeginNode());
        __begin.setNext(__with);
        return __begin;
    }
}
