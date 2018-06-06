package giraaff.core.common.cfg;

import java.util.ArrayList;
import java.util.List;

// @class Loop
public abstract class Loop<T extends AbstractBlockBase<T>>
{
    // @field
    private final Loop<T> ___parent;
    // @field
    private final List<Loop<T>> ___children;

    // @field
    private final int ___depth;
    // @field
    private final int ___index;
    // @field
    private final T ___header;
    // @field
    private final List<T> ___blocks;
    // @field
    private final List<T> ___exits;

    // @cons Loop
    protected Loop(Loop<T> __parent, int __index, T __header)
    {
        super();
        this.___parent = __parent;
        if (__parent != null)
        {
            this.___depth = __parent.getDepth() + 1;
        }
        else
        {
            this.___depth = 1;
        }
        this.___index = __index;
        this.___header = __header;
        this.___blocks = new ArrayList<>();
        this.___children = new ArrayList<>();
        this.___exits = new ArrayList<>();
    }

    public abstract long numBackedges();

    public Loop<T> getParent()
    {
        return this.___parent;
    }

    public List<Loop<T>> getChildren()
    {
        return this.___children;
    }

    public int getDepth()
    {
        return this.___depth;
    }

    public int getIndex()
    {
        return this.___index;
    }

    public T getHeader()
    {
        return this.___header;
    }

    public List<T> getBlocks()
    {
        return this.___blocks;
    }

    public List<T> getExits()
    {
        return this.___exits;
    }

    public void addExit(T __t)
    {
        this.___exits.add(__t);
    }

    ///
    // Determines if one loop is a transitive parent of another loop.
    //
    // @param childLoop The loop for which parentLoop might be a transitive parent loop.
    // @param parentLoop The loop which might be a transitive parent loop of child loop.
    // @return {@code true} if parentLoop is a (transitive) parent loop of childLoop, {@code false} otherwise
    ///
    public static <T extends AbstractBlockBase<T>> boolean transitiveParentLoop(Loop<T> __childLoop, Loop<T> __parentLoop)
    {
        Loop<T> __curr = __childLoop;
        while (__curr != null)
        {
            if (__curr == __parentLoop)
            {
                return true;
            }
            __curr = __curr.getParent();
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.___index + this.___depth * 31;
    }
}
