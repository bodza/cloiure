package giraaff.core.common.cfg;

// @class BlockMap
public final class BlockMap<T>
{
    // @field
    private final T[] data;

    @SuppressWarnings("unchecked")
    // @cons
    public BlockMap(AbstractControlFlowGraph<?> __cfg)
    {
        super();
        data = (T[]) new Object[__cfg.getBlocks().length];
    }

    public T get(AbstractBlockBase<?> __block)
    {
        return data[__block.getId()];
    }

    public void put(AbstractBlockBase<?> __block, T __value)
    {
        data[__block.getId()] = __value;
    }
}
