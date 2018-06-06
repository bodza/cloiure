package giraaff.nodes;

import giraaff.nodes.memory.MemoryNode;

// @class ValueNodeUtil
public final class ValueNodeUtil
{
    // @cons ValueNodeUtil
    private ValueNodeUtil()
    {
        super();
    }

    public static ValueNode asNode(MemoryNode __node)
    {
        if (__node == null)
        {
            return null;
        }
        else
        {
            return __node.asNode();
        }
    }
}
