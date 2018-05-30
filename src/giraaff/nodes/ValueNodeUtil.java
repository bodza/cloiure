package giraaff.nodes;

import giraaff.nodes.memory.MemoryNode;

// @class ValueNodeUtil
public final class ValueNodeUtil
{
    // @cons
    private ValueNodeUtil()
    {
        super();
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
