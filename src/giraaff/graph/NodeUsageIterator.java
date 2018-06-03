package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class NodeUsageIterator
final class NodeUsageIterator implements Iterator<Node>
{
    // @field
    final Node node;
    // @field
    int index = -1;
    // @field
    Node current;

    void advance()
    {
        current = null;
        index++;
        if (index == 0)
        {
            current = node.usage0;
        }
        else if (index == 1)
        {
            current = node.usage1;
        }
        else
        {
            int __relativeIndex = index - Node.INLINE_USAGE_COUNT;
            if (__relativeIndex < node.extraUsagesCount)
            {
                current = node.extraUsages[__relativeIndex];
            }
        }
    }

    // @cons
    NodeUsageIterator(Node __node)
    {
        super();
        this.node = __node;
        advance();
    }

    @Override
    public boolean hasNext()
    {
        return current != null;
    }

    @Override
    public Node next()
    {
        Node __result = current;
        if (__result == null)
        {
            throw new NoSuchElementException();
        }
        advance();
        return __result;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
