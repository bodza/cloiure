package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
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
