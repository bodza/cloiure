package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.Deque;

import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StructuredGraph;

// @class ScopedPostOrderNodeIterator
public abstract class ScopedPostOrderNodeIterator
{
    // @field
    private final Deque<FixedNode> nodeQueue;
    // @field
    private final NodeBitMap queuedNodes;
    // @field
    private final Deque<FixedNode> scopes;

    // @field
    protected FixedNode currentScopeStart;

    // @cons
    public ScopedPostOrderNodeIterator(StructuredGraph __graph)
    {
        super();
        this.queuedNodes = __graph.createNodeBitMap();
        this.nodeQueue = new ArrayDeque<>();
        this.scopes = getScopes(__graph);
    }

    public void apply()
    {
        while (!scopes.isEmpty())
        {
            queuedNodes.clearAll();
            this.currentScopeStart = scopes.pop();
            initializeScope();
            processScope();
        }
    }

    public void processScope()
    {
        FixedNode __current;
        queue(currentScopeStart);

        while ((__current = nextQueuedNode()) != null)
        {
            if (__current instanceof Invoke)
            {
                invoke((Invoke) __current);
                queueSuccessors(__current);
            }
            else if (__current instanceof LoopBeginNode)
            {
                queueLoopBeginSuccessors((LoopBeginNode) __current);
            }
            else if (__current instanceof LoopExitNode)
            {
                queueLoopExitSuccessors((LoopExitNode) __current);
            }
            else if (__current instanceof LoopEndNode)
            {
                // nothing todo
            }
            else if (__current instanceof AbstractMergeNode)
            {
                queueSuccessors(__current);
            }
            else if (__current instanceof FixedWithNextNode)
            {
                queueSuccessors(__current);
            }
            else if (__current instanceof EndNode)
            {
                queueMerge((EndNode) __current);
            }
            else if (__current instanceof ControlSinkNode)
            {
                // nothing todo
            }
            else if (__current instanceof ControlSplitNode)
            {
                queueSuccessors(__current);
            }
        }
    }

    protected void queueLoopBeginSuccessors(LoopBeginNode __node)
    {
        if (currentScopeStart == __node)
        {
            queue(__node.next());
        }
        else if (currentScopeStart instanceof LoopBeginNode)
        {
            // so we are currently processing loop A and found another loop B
            // -> queue all loop exits of B except those that also exit loop A
            for (LoopExitNode __loopExit : __node.loopExits())
            {
                if (!((LoopBeginNode) currentScopeStart).loopExits().contains(__loopExit))
                {
                    queue(__loopExit);
                }
            }
        }
        else
        {
            queue(__node.loopExits());
        }
    }

    protected void queueLoopExitSuccessors(LoopExitNode __node)
    {
        if (!(currentScopeStart instanceof LoopBeginNode) || !((LoopBeginNode) currentScopeStart).loopExits().contains(__node))
        {
            queueSuccessors(__node);
        }
    }

    protected Deque<FixedNode> getScopes(StructuredGraph __graph)
    {
        Deque<FixedNode> __result = new ArrayDeque<>();
        __result.push(__graph.start());
        for (LoopBeginNode __loopBegin : __graph.getNodes(LoopBeginNode.TYPE))
        {
            __result.push(__loopBegin);
        }
        return __result;
    }

    private void queueSuccessors(FixedNode __x)
    {
        queue(__x.successors());
    }

    private void queue(NodeIterable<? extends Node> __iter)
    {
        for (Node __node : __iter)
        {
            queue(__node);
        }
    }

    private void queue(Node __node)
    {
        if (__node != null && !queuedNodes.isMarked(__node))
        {
            queuedNodes.mark(__node);
            nodeQueue.addFirst((FixedNode) __node);
        }
    }

    private FixedNode nextQueuedNode()
    {
        if (nodeQueue.isEmpty())
        {
            return null;
        }

        return nodeQueue.removeFirst();
    }

    private void queueMerge(AbstractEndNode __end)
    {
        AbstractMergeNode __merge = __end.merge();
        if (!queuedNodes.isMarked(__merge) && visitedAllEnds(__merge))
        {
            queue(__merge);
        }
    }

    private boolean visitedAllEnds(AbstractMergeNode __merge)
    {
        for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
        {
            if (!queuedNodes.isMarked(__merge.forwardEndAt(__i)))
            {
                return false;
            }
        }
        return true;
    }

    protected abstract void initializeScope();

    protected abstract void invoke(Invoke invoke);
}
