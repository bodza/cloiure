package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import java.util.ArrayList;

import graalvm.compiler.api.replacements.Snippet.VarargsParameter;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.LoopBeginNode;

/**
 * Placeholder node to denote to snippet preparation that the following loop must be completely
 * unrolled.
 *
 * @see VarargsParameter
 */
@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class ExplodeLoopNode extends FixedWithNextNode
{
    public static final NodeClass<ExplodeLoopNode> TYPE = NodeClass.create(ExplodeLoopNode.class);

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
            assert currentNext != null : "cannot find loop after " + this;
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
