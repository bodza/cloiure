package giraaff.core.common.cfg;

// @class BlockMap
public final class BlockMap<T>
{
    private final T[] data;

    @SuppressWarnings("unchecked")
    // @cons
    public BlockMap(AbstractControlFlowGraph<?> cfg)
    {
        super();
        data = (T[]) new Object[cfg.getBlocks().length];
    }

    public T get(AbstractBlockBase<?> block)
    {
        return data[block.getId()];
    }

    public void put(AbstractBlockBase<?> block, T value)
    {
        data[block.getId()] = value;
    }
}
