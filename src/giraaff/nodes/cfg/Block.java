package giraaff.nodes.cfg;

import java.util.ArrayList;
import java.util.Iterator;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.Loop;
import giraaff.graph.Node;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.memory.MemoryCheckpoint;

public final class Block extends AbstractBlockBase<Block>
{
    public static final Block[] EMPTY_ARRAY = new Block[0];

    protected final AbstractBeginNode beginNode;

    protected FixedNode endNode;

    protected double probability;
    private Loop<Block> loop;

    protected Block postdominator;
    private LocationSet killLocations;
    private LocationSet killLocationsBetweenThisAndDominator;

    public Block(AbstractBeginNode node)
    {
        this.beginNode = node;
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

    public void setLoop(Loop<Block> loop)
    {
        this.loop = loop;
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
        Node predecessor = getBeginNode().predecessor();
        return predecessor != null && predecessor instanceof InvokeWithExceptionNode && getBeginNode() == ((InvokeWithExceptionNode) predecessor).exceptionEdge();
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
        Block b = this;
        while (true)
        {
            Block dom = b.getDominator();
            if (dom != null && dom.getPostdominator() == b)
            {
                b = dom;
            }
            else
            {
                break;
            }
        }
        return b;
    }

    @Override
    public Block getPostdominator()
    {
        return postdominator;
    }

    private class NodeIterator implements Iterator<FixedNode>
    {
        private FixedNode cur;

        NodeIterator()
        {
            cur = getBeginNode();
        }

        @Override
        public boolean hasNext()
        {
            return cur != null;
        }

        @Override
        public FixedNode next()
        {
            FixedNode result = cur;
            if (result instanceof FixedWithNextNode)
            {
                FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) result;
                FixedNode next = fixedWithNextNode.next();
                if (next instanceof AbstractBeginNode)
                {
                    next = null;
                }
                cur = next;
            }
            else
            {
                cur = null;
            }
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public Iterable<FixedNode> getNodes()
    {
        return new Iterable<FixedNode>()
        {
            @Override
            public Iterator<FixedNode> iterator()
            {
                return new NodeIterator();
            }

            @Override
            public String toString()
            {
                StringBuilder sb = new StringBuilder().append('[');
                for (FixedNode node : this)
                {
                    sb.append(node).append(", ");
                }
                if (sb.length() > 1)
                {
                    sb.setLength(sb.length() - 2);
                }
                return sb.append(']').toString();
            }
        };
    }

    @Override
    public String toString()
    {
        return toString(Verbosity.Id);
    }

    public String toString(Verbosity verbosity)
    {
        StringBuilder sb = new StringBuilder();
        sb.append('B').append(id);
        if (verbosity != Verbosity.Id)
        {
            if (isLoopHeader())
            {
                sb.append(" lh");
            }

            if (getSuccessorCount() > 0)
            {
                sb.append(" ->[");
                for (int i = 0; i < getSuccessorCount(); ++i)
                {
                    if (i != 0)
                    {
                        sb.append(',');
                    }
                    sb.append('B').append(getSuccessors()[i].getId());
                }
                sb.append(']');
            }

            if (getPredecessorCount() > 0)
            {
                sb.append(" <-[");
                for (int i = 0; i < getPredecessorCount(); ++i)
                {
                    if (i != 0)
                    {
                        sb.append(',');
                    }
                    sb.append('B').append(getPredecessors()[i].getId());
                }
                sb.append(']');
            }
        }
        return sb.toString();
    }

    @Override
    public double probability()
    {
        return probability;
    }

    public void setProbability(double probability)
    {
        this.probability = probability;
    }

    @Override
    public Block getDominator(int distance)
    {
        Block result = this;
        for (int i = 0; i < distance; ++i)
        {
            result = result.getDominator();
        }
        return result;
    }

    public boolean canKill(LocationIdentity location)
    {
        if (location.isImmutable())
        {
            return false;
        }
        return getKillLocations().contains(location);
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
        LocationSet result = new LocationSet();
        for (FixedNode node : this.getNodes())
        {
            if (node instanceof MemoryCheckpoint.Single)
            {
                LocationIdentity identity = ((MemoryCheckpoint.Single) node).getLocationIdentity();
                result.add(identity);
            }
            else if (node instanceof MemoryCheckpoint.Multi)
            {
                for (LocationIdentity identity : ((MemoryCheckpoint.Multi) node).getLocationIdentities())
                {
                    result.add(identity);
                }
            }
            if (result.isAny())
            {
                break;
            }
        }
        return result;
    }

    public boolean canKillBetweenThisAndDominator(LocationIdentity location)
    {
        if (location.isImmutable())
        {
            return false;
        }
        return this.getKillLocationsBetweenThisAndDominator().contains(location);
    }

    private LocationSet getKillLocationsBetweenThisAndDominator()
    {
        if (this.killLocationsBetweenThisAndDominator == null)
        {
            LocationSet dominatorResult = new LocationSet();
            Block stopBlock = getDominator();
            if (this.isLoopHeader())
            {
                dominatorResult.addAll(((HIRLoop) this.getLoop()).getKillLocations());
            }
            else
            {
                for (Block b : this.getPredecessors())
                {
                    if (b != stopBlock)
                    {
                        dominatorResult.addAll(b.getKillLocations());
                        if (dominatorResult.isAny())
                        {
                            break;
                        }
                        b.calcKillLocationsBetweenThisAndTarget(dominatorResult, stopBlock);
                        if (dominatorResult.isAny())
                        {
                            break;
                        }
                    }
                }
            }
            this.killLocationsBetweenThisAndDominator = dominatorResult;
        }
        return this.killLocationsBetweenThisAndDominator;
    }

    private void calcKillLocationsBetweenThisAndTarget(LocationSet result, Block stopBlock)
    {
        if (stopBlock == this || result.isAny())
        {
            // We reached the stop block => nothing to do.
            return;
        }
        else
        {
            if (stopBlock == this.getDominator())
            {
                result.addAll(this.getKillLocationsBetweenThisAndDominator());
            }
            else
            {
                // Divide and conquer: Aggregate kill locations from this to the dominator and then
                // from the dominator onwards.
                calcKillLocationsBetweenThisAndTarget(result, this.getDominator());
                result.addAll(this.getDominator().getKillLocations());
                if (result.isAny())
                {
                    return;
                }
                this.getDominator().calcKillLocationsBetweenThisAndTarget(result, stopBlock);
            }
        }
    }

    @Override
    public void delete()
    {
        // adjust successor and predecessor lists
        Block next = getSuccessors()[0];
        for (Block pred : getPredecessors())
        {
            Block[] predSuccs = pred.successors;
            Block[] newPredSuccs = new Block[predSuccs.length];
            for (int i = 0; i < predSuccs.length; ++i)
            {
                if (predSuccs[i] == this)
                {
                    newPredSuccs[i] = next;
                }
                else
                {
                    newPredSuccs[i] = predSuccs[i];
                }
            }
            pred.setSuccessors(newPredSuccs);
        }

        ArrayList<Block> newPreds = new ArrayList<>();
        for (int i = 0; i < next.getPredecessorCount(); i++)
        {
            Block curPred = next.getPredecessors()[i];
            if (curPred == this)
            {
                for (Block b : getPredecessors())
                {
                    newPreds.add(b);
                }
            }
            else
            {
                newPreds.add(curPred);
            }
        }

        next.setPredecessors(newPreds.toArray(new Block[0]));
    }

    protected void setPostDominator(Block postdominator)
    {
        this.postdominator = postdominator;
    }
}
