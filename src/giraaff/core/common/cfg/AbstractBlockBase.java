package giraaff.core.common.cfg;

// @class AbstractBlockBase
public abstract class AbstractBlockBase<T extends AbstractBlockBase<T>>
{
    // @field
    protected int ___id;
    // @field
    protected int ___domDepth;

    // @field
    protected T[] ___predecessors;
    // @field
    protected T[] ___successors;

    // @field
    private T ___dominator;
    // @field
    private T ___firstDominated;
    // @field
    private T ___dominatedSibling;
    // @field
    private int ___domNumber;
    // @field
    private int ___maxChildDomNumber;

    // @field
    private boolean ___align;
    // @field
    private int ___linearScanNumber;

    // @cons AbstractBlockBase
    protected AbstractBlockBase()
    {
        super();
        this.___id = AbstractControlFlowGraph.BLOCK_ID_INITIAL;
        this.___linearScanNumber = -1;
        this.___domNumber = -1;
        this.___maxChildDomNumber = -1;
    }

    public void setDominatorNumber(int __domNumber)
    {
        this.___domNumber = __domNumber;
    }

    public void setMaxChildDomNumber(int __maxChildDomNumber)
    {
        this.___maxChildDomNumber = __maxChildDomNumber;
    }

    public int getDominatorNumber()
    {
        return this.___domNumber;
    }

    public int getMaxChildDominatorNumber()
    {
        return this.___maxChildDomNumber;
    }

    public int getId()
    {
        return this.___id;
    }

    public void setId(int __id)
    {
        this.___id = __id;
    }

    public T[] getPredecessors()
    {
        return this.___predecessors;
    }

    public void setPredecessors(T[] __predecessors)
    {
        this.___predecessors = __predecessors;
    }

    public T[] getSuccessors()
    {
        return this.___successors;
    }

    public void setSuccessors(T[] __successors)
    {
        this.___successors = __successors;
    }

    public T getDominator()
    {
        return this.___dominator;
    }

    public void setDominator(T __dominator)
    {
        this.___dominator = __dominator;
        this.___domDepth = __dominator.___domDepth + 1;
    }

    ///
    // Level in the dominator tree starting with 0 for the start block.
    ///
    public int getDominatorDepth()
    {
        return this.___domDepth;
    }

    public T getFirstDominated()
    {
        return this.___firstDominated;
    }

    public void setFirstDominated(T __block)
    {
        this.___firstDominated = __block;
    }

    public T getDominatedSibling()
    {
        return this.___dominatedSibling;
    }

    public void setDominatedSibling(T __block)
    {
        this.___dominatedSibling = __block;
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
        return this.___linearScanNumber;
    }

    public void setLinearScanNumber(int __linearScanNumber)
    {
        this.___linearScanNumber = __linearScanNumber;
    }

    public boolean isAligned()
    {
        return this.___align;
    }

    public void setAlign(boolean __align)
    {
        this.___align = __align;
    }

    public abstract boolean isExceptionEntry();

    public abstract Loop<T> getLoop();

    public abstract int getLoopDepth();

    public abstract void delete();

    public abstract boolean isLoopEnd();

    public abstract boolean isLoopHeader();

    public abstract T getPostdominator();

    public abstract double probability();

    public abstract T getDominator(int __distance);

    @Override
    public int hashCode()
    {
        return this.___id;
    }
}
