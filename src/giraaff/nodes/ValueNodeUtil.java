package giraaff.nodes;

import java.util.ArrayList;
import java.util.Collection;

import giraaff.graph.Node;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.memory.MemoryNode;

// @class ValueNodeUtil
public final class ValueNodeUtil
{
    // @cons
    private ValueNodeUtil()
    {
        super();
    }

    public static RuntimeException shouldNotReachHere(String msg)
    {
        throw new InternalError("should not reach here: " + msg);
    }

    public static RuntimeException shouldNotReachHere()
    {
        throw new InternalError("should not reach here");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node> Collection<T> filter(Iterable<Node> nodes, Class<T> clazz)
    {
        ArrayList<T> phis = new ArrayList<>();
        for (Node node : nodes)
        {
            if (clazz.isInstance(node))
            {
                phis.add((T) node);
            }
        }
        return phis;
    }

    /**
     * Converts a given instruction to a value string. The representation of an node as a value is
     * formed by concatenating the {@linkplain jdk.vm.ci.meta.JavaKind#getTypeChar character}
     * denoting its {@linkplain ValueNode#getStackKind kind} and its id. For example, {@code "i13"}.
     *
     * @param value the instruction to convert to a value string. If {@code value == null}, then "-"
     *            is returned.
     * @return the instruction representation as a string
     */
    public static String valueString(ValueNode value)
    {
        return (value == null) ? "-" : ("" + Character.toLowerCase(value.getStackKind().getTypeChar()) + value.toString(Verbosity.Id));
    }

    public static ValueNode asNode(MemoryNode node)
    {
        if (node == null)
        {
            return null;
        }
        else
        {
            return node.asNode();
        }
    }
}
