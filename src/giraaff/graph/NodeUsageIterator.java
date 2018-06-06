package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class NodeUsageIterator
final class NodeUsageIterator implements Iterator<Node>
{
    // @field
    final Node ___node;
    // @field
    int ___index = -1;
    // @field
    Node ___current;

    void advance()
    {
        this.___current = null;
        this.___index++;
        if (this.___index == 0)
        {
            this.___current = this.___node.___usage0;
        }
        else if (this.___index == 1)
        {
            this.___current = this.___node.___usage1;
        }
        else
        {
            int __relativeIndex = this.___index - Node.INLINE_USAGE_COUNT;
            if (__relativeIndex < this.___node.___extraUsagesCount)
            {
                this.___current = this.___node.___extraUsages[__relativeIndex];
            }
        }
    }

    // @cons NodeUsageIterator
    NodeUsageIterator(Node __node)
    {
        super();
        this.___node = __node;
        advance();
    }

    @Override
    public boolean hasNext()
    {
        return this.___current != null;
    }

    @Override
    public Node next()
    {
        Node __result = this.___current;
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
