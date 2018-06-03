package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Queue;

// @class NodeWorkList
public abstract class NodeWorkList implements Iterable<Node>
{
    // @field
    protected final Queue<Node> ___worklist = new ArrayDeque<>();

    // @cons
    protected NodeWorkList(Graph __graph, boolean __fill)
    {
        super();
        if (__fill)
        {
            for (Node __node : __graph.getNodes())
            {
                this.___worklist.add(__node);
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

    public abstract void add(Node __node);

    public abstract boolean contains(Node __node);
}
