package giraaff.core.common.cfg;

// @class AbstractBlockBase
public abstract class AbstractBlockBase<T extends AbstractBlockBase<T>>
{
    // @field
    protected int id;
    // @field
    protected int domDepth;

    // @field
    protected T[] predecessors;
    // @field
    protected T[] successors;

    // @field
    private T dominator;
    // @field
    private T firstDominated;
    // @field
    private T dominatedSibling;
    // @field
    private int domNumber;
    // @field
    private int maxChildDomNumber;

    // @field
    private boolean align;
    // @field
    private int linearScanNumber;

    // @cons
    protected AbstractBlockBase()
    {
        super();
        this.id = AbstractControlFlowGraph.BLOCK_ID_INITIAL;
        this.linearScanNumber = -1;
        this.domNumber = -1;
        this.maxChildDomNumber = -1;
    }

    public void setDominatorNumber(int __domNumber)
    {
        this.domNumber = __domNumber;
    }

    public void setMaxChildDomNumber(int __maxChildDomNumber)
    {
        this.maxChildDomNumber = __maxChildDomNumber;
    }

    public int getDominatorNumber()
    {
        return domNumber;
    }

    public int getMaxChildDominatorNumber()
    {
        return this.maxChildDomNumber;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int __id)
    {
        this.id = __id;
    }

    public T[] getPredecessors()
    {
        return predecessors;
    }

    public void setPredecessors(T[] __predecessors)
    {
        this.predecessors = __predecessors;
    }

    public T[] getSuccessors()
    {
        return successors;
    }

    public void setSuccessors(T[] __successors)
    {
        this.successors = __successors;
    }

    public T getDominator()
    {
        return dominator;
    }

    public void setDominator(T __dominator)
    {
        this.dominator = __dominator;
        this.domDepth = __dominator.domDepth + 1;
    }

    /**
     * Level in the dominator tree starting with 0 for the start block.
     */
    public int getDominatorDepth()
    {
        return domDepth;
    }

    public T getFirstDominated()
    {
        return this.firstDominated;
    }

    public void setFirstDominated(T __block)
    {
        this.firstDominated = __block;
    }

    public T getDominatedSibling()
    {
        return this.dominatedSibling;
    }

    public void setDominatedSibling(T __block)
    {
        this.dominatedSibling = __block;
    }

    public int getPredecessorCount()
    {
        return getPredecessors().length;
    }

    public int getSuccessorCount()
    {
        return getSuccessors().length;
    }

    public int getLinearScanNumber()
    {
        return linearScanNumber;
    }

    public void setLinearScanNumber(int __linearScanNumber)
    {
        this.linearScanNumber = __linearScanNumber;
    }

    public boolean isAligned()
    {
        return align;
    }

    public void setAlign(boolean __align)
    {
        this.align = __align;
    }

    public abstract boolean isExceptionEntry();

    public abstract Loop<T> getLoop();

    public abstract int getLoopDepth();

    public abstract void delete();

    public abstract boolean isLoopEnd();

    public abstract boolean isLoopHeader();

    public abstract T getPostdominator();

    public abstract double probability();

    public abstract T getDominator(int distance);

    @Override
    public int hashCode()
    {
        return id;
    }
}
