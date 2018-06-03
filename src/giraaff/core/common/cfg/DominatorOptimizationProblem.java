package giraaff.core.common.cfg;

import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class represents a dominator tree problem, i.e. a problem which can be solved by traversing
 * the dominator (sub-)tree.
 *
 * @param <E> An enum that describes the flags that can be associated with a block.
 * @param <C> An arbitrary cost type that is associated with a block. It is intended to carry
 *            information needed to calculate the solution. Note that {@code C} should not contain
 *            boolean flags. Use an enum entry in {@code E} instead.
 */
// @class DominatorOptimizationProblem
public abstract class DominatorOptimizationProblem<E extends Enum<E>, C>
{
    // @field
    private AbstractBlockBase<?>[] blocks;
    // @field
    private EnumMap<E, BitSet> flags;
    // @field
    private BlockMap<C> costs;

    // @cons
    protected DominatorOptimizationProblem(Class<E> __flagType, AbstractControlFlowGraph<?> __cfg)
    {
        super();
        this.blocks = __cfg.getBlocks();
        flags = new EnumMap<>(__flagType);
        costs = new BlockMap<>(__cfg);
    }

    public final AbstractBlockBase<?>[] getBlocks()
    {
        return blocks;
    }

    public final AbstractBlockBase<?> getBlockForId(int __id)
    {
        return blocks[__id];
    }

    /**
     * Sets a flag for a block.
     */
    public final void set(E __flag, AbstractBlockBase<?> __block)
    {
        BitSet __bitSet = flags.get(__flag);
        if (__bitSet == null)
        {
            __bitSet = new BitSet(blocks.length);
            flags.put(__flag, __bitSet);
        }
        __bitSet.set(__block.getId());
    }

    /**
     * Checks whether a flag is set for a block.
     */
    public final boolean get(E __flag, AbstractBlockBase<?> __block)
    {
        BitSet __bitSet = flags.get(__flag);
        return __bitSet == null ? false : __bitSet.get(__block.getId());
    }

    /**
     * Returns a {@linkplain Stream} of blocks for which {@code flag} is set.
     */
    public final Stream<? extends AbstractBlockBase<?>> stream(E __flag)
    {
        return Arrays.asList(getBlocks()).stream().filter(__block -> get(__flag, __block));
    }

    /**
     * Returns the cost object associated with {@code block}. Might return {@code null} if not set.
     */
    public final C getCost(AbstractBlockBase<?> __block)
    {
        return costs.get(__block);
    }

    /**
     * Sets the cost for a {@code block}.
     */
    public final void setCost(AbstractBlockBase<?> __block, C __cost)
    {
        costs.put(__block, __cost);
    }

    /**
     * Sets {@code flag} for all blocks along the dominator path from {@code block} to the root
     * until a block it finds a block where {@code flag} is already set.
     */
    public final void setDominatorPath(E __flag, AbstractBlockBase<?> __block)
    {
        BitSet __bitSet = flags.get(__flag);
        if (__bitSet == null)
        {
            __bitSet = new BitSet(blocks.length);
            flags.put(__flag, __bitSet);
        }
        for (AbstractBlockBase<?> __b = __block; __b != null && !__bitSet.get(__b.getId()); __b = __b.getDominator())
        {
            // mark block
            __bitSet.set(__b.getId());
        }
    }

    /**
     * Returns a {@link Stream} of flags associated with {@code block}.
     */
    public final Stream<E> getFlagsForBlock(AbstractBlockBase<?> __block)
    {
        return getFlags().stream().filter(__flag -> get(__flag, __block));
    }

    /**
     * Returns the {@link Set} of flags that can be set for this
     * {@linkplain DominatorOptimizationProblem problem}.
     */
    public final Set<E> getFlags()
    {
        return flags.keySet();
    }

    /**
     * Returns the name of a flag.
     */
    public String getName(E __flag)
    {
        return __flag.toString();
    }
}
