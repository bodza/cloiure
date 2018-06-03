package giraaff.core.common.cfg;

// @class BlockMap
public final class BlockMap<T>
{
    // @field
    private final T[] ___data;

    @SuppressWarnings("unchecked")
    // @cons
    public BlockMap(AbstractControlFlowGraph<?> __cfg)
    {
        super();
        this.___data = (T[]) new Object[__cfg.getBlocks().length];
    }

    public T get(AbstractBlockBase<?> __block)
    {
        return this.___data[__block.getId()];
    }

    public void put(AbstractBlockBase<?> __block, T __value)
    {
        this.___data[__block.getId()] = __value;
    }
}
