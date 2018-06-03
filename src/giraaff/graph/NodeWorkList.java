package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Queue;

// @class NodeWorkList
public abstract class NodeWorkList implements Iterable<Node>
{
    // @field
    protected final Queue<Node> worklist = new ArrayDeque<>();

    // @cons
    protected NodeWorkList(Graph __graph, boolean __fill)
    {
        super();
        if (__fill)
        {
            for (Node __node : __graph.getNodes())
            {
                this.worklist.add(__node);
            }
        }
    }

    public void addAll(Iterable<? extends Node> __nodes)
    {
        for (Node __node : __nodes)
        {
            if (__node.isAlive())
            {
                this.add(__node);
            }
        }
    }

    public abstract void add(Node node);

    public abstract boolean contains(Node node);
}
