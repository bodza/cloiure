package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Queue;

// @class NodeWorkList
public abstract class NodeWorkList implements Iterable<Node>
{
    protected final Queue<Node> worklist = new ArrayDeque<>();

    // @cons
    protected NodeWorkList(Graph graph, boolean fill)
    {
        super();
        if (fill)
        {
            for (Node node : graph.getNodes())
            {
                this.worklist.add(node);
            }
        }
    }

    public void addAll(Iterable<? extends Node> nodes)
    {
        for (Node node : nodes)
        {
            if (node.isAlive())
            {
                this.add(node);
            }
        }
    }

    public abstract void add(Node node);

    public abstract boolean contains(Node node);
}
