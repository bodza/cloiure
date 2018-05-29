package giraaff.replacements.nodes;

import java.util.ArrayList;

import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopBeginNode;

/**
 * Placeholder node to denote to snippet preparation that the following loop must be completely unrolled.
 *
 * @see VarargsParameter
 */
// @class ExplodeLoopNode
public final class ExplodeLoopNode extends FixedWithNextNode
{
    public static final NodeClass<ExplodeLoopNode> TYPE = NodeClass.create(ExplodeLoopNode.class);

    // @cons
    public ExplodeLoopNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    public LoopBeginNode findLoopBegin()
    {
        Node currentNext = next();
        ArrayList<Node> succs = new ArrayList<>();
        while (!(currentNext instanceof LoopBeginNode))
        {
            for (Node n : currentNext.cfgSuccessors())
            {
                succs.add(n);
            }
            if (succs.size() == 1 && succs.get(0) != currentNext)
            {
                currentNext = succs.get(0);
            }
            else
            {
                return null;
            }
        }
        return (LoopBeginNode) currentNext;
    }

    /**
     * A call to this method must be placed immediately prior to the loop that is to be exploded.
     */
    @NodeIntrinsic
    public static native void explodeLoop();
}
