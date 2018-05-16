package graalvm.compiler.graph;

import java.util.ConcurrentModificationException;

class NodeUsageWithModCountIterator extends NodeUsageIterator
{
    NodeUsageWithModCountIterator(Node n)
    {
        super(n);
    }

    private final int expectedModCount = node.usageModCount();

    @Override
    public boolean hasNext()
    {
        if (expectedModCount != node.usageModCount())
        {
            throw new ConcurrentModificationException();
        }
        return super.hasNext();
    }

    @Override
    public Node next()
    {
        if (expectedModCount != node.usageModCount())
        {
            throw new ConcurrentModificationException();
        }
        return super.next();
    }
}
