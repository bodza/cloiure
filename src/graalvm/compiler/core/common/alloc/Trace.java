package graalvm.compiler.core.common.alloc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;

/**
 * Represents a list of sequentially executed {@code AbstractBlockBase blocks}.
 */
public class Trace
{
    private final AbstractBlockBase<?>[] blocks;
    private final ArrayList<Trace> successors;
    private int id = -1;

    public Trace(Collection<AbstractBlockBase<?>> blocks)
    {
        this(blocks.toArray(new AbstractBlockBase<?>[0]));
    }

    public Trace(AbstractBlockBase<?>[] blocks)
    {
        this.blocks = blocks;
        this.successors = new ArrayList<>();
    }

    public AbstractBlockBase<?>[] getBlocks()
    {
        return blocks;
    }

    public ArrayList<Trace> getSuccessors()
    {
        return successors;
    }

    public int size()
    {
        return getBlocks().length;
    }

    @Override
    public String toString()
    {
        return "Trace" + Arrays.toString(blocks);
    }

    public int getId()
    {
        return id;
    }

    void setId(int id)
    {
        this.id = id;
    }
}
