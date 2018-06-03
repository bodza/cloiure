package giraaff.core.common.cfg;

import java.util.ArrayList;
import java.util.List;

// @class Loop
public abstract class Loop<T extends AbstractBlockBase<T>>
{
    // @field
    private final Loop<T> parent;
    // @field
    private final List<Loop<T>> children;

    // @field
    private final int depth;
    // @field
    private final int index;
    // @field
    private final T header;
    // @field
    private final List<T> blocks;
    // @field
    private final List<T> exits;

    // @cons
    protected Loop(Loop<T> __parent, int __index, T __header)
    {
        super();
        this.parent = __parent;
        if (__parent != null)
        {
            this.depth = __parent.getDepth() + 1;
        }
        else
        {
            this.depth = 1;
        }
        this.index = __index;
        this.header = __header;
        this.blocks = new ArrayList<>();
        this.children = new ArrayList<>();
        this.exits = new ArrayList<>();
    }

    public abstract long numBackedges();

    public Loop<T> getParent()
    {
        return parent;
    }

    public List<Loop<T>> getChildren()
    {
        return children;
    }

    public int getDepth()
    {
        return depth;
    }

    public int getIndex()
    {
        return index;
    }

    public T getHeader()
    {
        return header;
    }

    public List<T> getBlocks()
    {
        return blocks;
    }

    public List<T> getExits()
    {
        return exits;
    }

    public void addExit(T __t)
    {
        exits.add(__t);
    }

    /**
     * Determines if one loop is a transitive parent of another loop.
     *
     * @param childLoop The loop for which parentLoop might be a transitive parent loop.
     * @param parentLoop The loop which might be a transitive parent loop of child loop.
     * @return {@code true} if parentLoop is a (transitive) parent loop of childLoop, {@code false} otherwise
     */
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
        return index + depth * 31;
    }
}
