package giraaff.replacements.nodes;

import java.util.ArrayList;

import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopBeginNode;

///
// Placeholder node to denote to snippet preparation that the following loop must be completely unrolled.
//
// @see Snippet.VarargsParameter
///
// @class ExplodeLoopNode
public final class ExplodeLoopNode extends FixedWithNextNode
{
    // @def
    public static final NodeClass<ExplodeLoopNode> TYPE = NodeClass.create(ExplodeLoopNode.class);

    // @cons ExplodeLoopNode
    public ExplodeLoopNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    public LoopBeginNode findLoopBegin()
    {
        Node __currentNext = next();
        ArrayList<Node> __succs = new ArrayList<>();
        while (!(__currentNext instanceof LoopBeginNode))
        {
            for (Node __n : __currentNext.cfgSuccessors())
            {
                __succs.add(__n);
            }
            if (__succs.size() == 1 && __succs.get(0) != __currentNext)
            {
                __currentNext = __succs.get(0);
            }
            else
            {
                return null;
            }
        }
        return (LoopBeginNode) __currentNext;
    }

    ///
    // A call to this method must be placed immediately prior to the loop that is to be exploded.
    ///
    @Node.NodeIntrinsic
    public static native void explodeLoop();
}
