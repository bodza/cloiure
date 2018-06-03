package giraaff.nodes.cfg;

import java.util.ArrayList;
import java.util.Iterator;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.Loop;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.memory.MemoryCheckpoint;

// @class Block
public final class Block extends AbstractBlockBase<Block>
{
    // @def
    public static final Block[] EMPTY_ARRAY = new Block[0];

    // @field
    protected final AbstractBeginNode beginNode;

    // @field
    protected FixedNode endNode;

    // @field
    protected double probability;
    // @field
    private Loop<Block> loop;

    // @field
    protected Block postdominator;
    // @field
    private LocationSet killLocations;
    // @field
    private LocationSet killLocationsBetweenThisAndDominator;

    // @cons
    public Block(AbstractBeginNode __node)
    {
        super();
        this.beginNode = __node;
    }

    public AbstractBeginNode getBeginNode()
    {
        return beginNode;
    }

    public FixedNode getEndNode()
    {
        return endNode;
    }

    /**
     * Return the {@link LoopExitNode} for this block if it exists.
     */
    public LoopExitNode getLoopExit()
    {
        if (beginNode instanceof BeginNode)
        {
            if (beginNode.next() instanceof LoopExitNode)
            {
                return (LoopExitNode) beginNode.next();
            }
        }
        if (beginNode instanceof LoopExitNode)
        {
            return (LoopExitNode) beginNode;
        }
        return null;
    }

    @Override
    public Loop<Block> getLoop()
    {
        return loop;
    }

    public void setLoop(Loop<Block> __loop)
    {
        this.loop = __loop;
    }

    @Override
    public int getLoopDepth()
    {
        return loop == null ? 0 : loop.getDepth();
    }

    @Override
    public boolean isLoopHeader()
    {
        return getBeginNode() instanceof LoopBeginNode;
    }

    @Override
    public boolean isLoopEnd()
    {
        return getEndNode() instanceof LoopEndNode;
    }

    @Override
    public boolean isExceptionEntry()
    {
        Node __predecessor = getBeginNode().predecessor();
        return __predecessor != null && __predecessor instanceof InvokeWithExceptionNode && getBeginNode() == ((InvokeWithExceptionNode) __predecessor).exceptionEdge();
    }

    public Block getFirstPredecessor()
    {
        return getPredecessors()[0];
    }

    public Block getFirstSuccessor()
    {
        return getSuccessors()[0];
    }

    public Block getEarliestPostDominated()
    {
        Block __b = this;
        while (true)
        {
            Block __dom = __b.getDominator();
            if (__dom != null && __dom.getPostdominator() == __b)
            {
                __b = __dom;
            }
            else
            {
                break;
            }
        }
        return __b;
    }

    @Override
    public Block getPostdominator()
    {
        return postdominator;
    }

    // @class Block.NodeIterator
    // @closure
    private final class NodeIterator implements Iterator<FixedNode>
    {
        // @field
        private FixedNode cur;

        // @cons
        NodeIterator()
        {
            super();
            cur = Block.this.getBeginNode();
        }

        @Override
        public boolean hasNext()
        {
            return cur != null;
        }

        @Override
        public FixedNode next()
        {
            FixedNode __result = cur;
            if (__result instanceof FixedWithNextNode)
            {
                FixedNode __next = ((FixedWithNextNode) __result).next();
                if (__next instanceof AbstractBeginNode)
                {
                    __next = null;
                }
                cur = __next;
            }
            else
            {
                cur = null;
            }
            return __result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public Iterable<FixedNode> getNodes()
    {
        // @closure
        return new Iterable<FixedNode>()
        {
            @Override
            public Iterator<FixedNode> iterator()
            {
                return new NodeIterator();
            }
        };
    }

    @Override
    public double probability()
    {
        return probability;
    }

    public void setProbability(double __probability)
    {
        this.probability = __probability;
    }

    @Override
    public Block getDominator(int __distance)
    {
        Block __result = this;
        for (int __i = 0; __i < __distance; ++__i)
        {
            __result = __result.getDominator();
        }
        return __result;
    }

    public boolean canKill(LocationIdentity __location)
    {
        if (__location.isImmutable())
        {
            return false;
        }
        return getKillLocations().contains(__location);
    }

    public LocationSet getKillLocations()
    {
        if (killLocations == null)
        {
            killLocations = calcKillLocations();
        }
        return killLocations;
    }

    private LocationSet calcKillLocations()
    {
        LocationSet __result = new LocationSet();
        for (FixedNode __node : this.getNodes())
        {
            if (__node instanceof MemoryCheckpoint.Single)
            {
                LocationIdentity __identity = ((MemoryCheckpoint.Single) __node).getLocationIdentity();
                __result.add(__identity);
            }
            else if (__node instanceof MemoryCheckpoint.Multi)
            {
                for (LocationIdentity __identity : ((MemoryCheckpoint.Multi) __node).getLocationIdentities())
                {
                    __result.add(__identity);
                }
            }
            if (__result.isAny())
            {
                break;
            }
        }
        return __result;
    }

    public boolean canKillBetweenThisAndDominator(LocationIdentity __location)
    {
        if (__location.isImmutable())
        {
            return false;
        }
        return this.getKillLocationsBetweenThisAndDominator().contains(__location);
    }

    private LocationSet getKillLocationsBetweenThisAndDominator()
    {
        if (this.killLocationsBetweenThisAndDominator == null)
        {
            LocationSet __dominatorResult = new LocationSet();
            Block __stopBlock = getDominator();
            if (this.isLoopHeader())
            {
                __dominatorResult.addAll(((HIRLoop) this.getLoop()).getKillLocations());
            }
            else
            {
                for (Block __b : this.getPredecessors())
                {
                    if (__b != __stopBlock)
                    {
                        __dominatorResult.addAll(__b.getKillLocations());
                        if (__dominatorResult.isAny())
                        {
                            break;
                        }
                        __b.calcKillLocationsBetweenThisAndTarget(__dominatorResult, __stopBlock);
                        if (__dominatorResult.isAny())
                        {
                            break;
                        }
                    }
                }
            }
            this.killLocationsBetweenThisAndDominator = __dominatorResult;
        }
        return this.killLocationsBetweenThisAndDominator;
    }

    private void calcKillLocationsBetweenThisAndTarget(LocationSet __result, Block __stopBlock)
    {
        if (__stopBlock == this || __result.isAny())
        {
            // We reached the stop block => nothing to do.
            return;
        }
        else
        {
            if (__stopBlock == this.getDominator())
            {
                __result.addAll(this.getKillLocationsBetweenThisAndDominator());
            }
            else
            {
                // Divide and conquer: Aggregate kill locations from this to the dominator and then
                // from the dominator onwards.
                calcKillLocationsBetweenThisAndTarget(__result, this.getDominator());
                __result.addAll(this.getDominator().getKillLocations());
                if (__result.isAny())
                {
                    return;
                }
                this.getDominator().calcKillLocationsBetweenThisAndTarget(__result, __stopBlock);
            }
        }
    }

    @Override
    public void delete()
    {
        // adjust successor and predecessor lists
        Block __next = getSuccessors()[0];
        for (Block __pred : getPredecessors())
        {
            Block[] __predSuccs = __pred.successors;
            Block[] __newPredSuccs = new Block[__predSuccs.length];
            for (int __i = 0; __i < __predSuccs.length; ++__i)
            {
                if (__predSuccs[__i] == this)
                {
                    __newPredSuccs[__i] = __next;
                }
                else
                {
                    __newPredSuccs[__i] = __predSuccs[__i];
                }
            }
            __pred.setSuccessors(__newPredSuccs);
        }

        ArrayList<Block> __newPreds = new ArrayList<>();
        for (int __i = 0; __i < __next.getPredecessorCount(); __i++)
        {
            Block __curPred = __next.getPredecessors()[__i];
            if (__curPred == this)
            {
                for (Block __b : getPredecessors())
                {
                    __newPreds.add(__b);
                }
            }
            else
            {
                __newPreds.add(__curPred);
            }
        }

        __next.setPredecessors(__newPreds.toArray(new Block[0]));
    }

    protected void setPostDominator(Block __postdominator)
    {
        this.postdominator = __postdominator;
    }
}
