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
    private AbstractBlockBase<?>[] blocks;
    private EnumMap<E, BitSet> flags;
    private BlockMap<C> costs;

    // @cons
    protected DominatorOptimizationProblem(Class<E> flagType, AbstractControlFlowGraph<?> cfg)
    {
        super();
        this.blocks = cfg.getBlocks();
        flags = new EnumMap<>(flagType);
        costs = new BlockMap<>(cfg);
    }

    public final AbstractBlockBase<?>[] getBlocks()
    {
        return blocks;
    }

    public final AbstractBlockBase<?> getBlockForId(int id)
    {
        return blocks[id];
    }

    /**
     * Sets a flag for a block.
     */
    public final void set(E flag, AbstractBlockBase<?> block)
    {
        BitSet bitSet = flags.get(flag);
        if (bitSet == null)
        {
            bitSet = new BitSet(blocks.length);
            flags.put(flag, bitSet);
        }
        bitSet.set(block.getId());
    }

    /**
     * Checks whether a flag is set for a block.
     */
    public final boolean get(E flag, AbstractBlockBase<?> block)
    {
        BitSet bitSet = flags.get(flag);
        return bitSet == null ? false : bitSet.get(block.getId());
    }

    /**
     * Returns a {@linkplain Stream} of blocks for which {@code flag} is set.
     */
    public final Stream<? extends AbstractBlockBase<?>> stream(E flag)
    {
        return Arrays.asList(getBlocks()).stream().filter(block -> get(flag, block));
    }

    /**
     * Returns the cost object associated with {@code block}. Might return {@code null} if not set.
     */
    public final C getCost(AbstractBlockBase<?> block)
    {
        return costs.get(block);
    }

    /**
     * Sets the cost for a {@code block}.
     */
    public final void setCost(AbstractBlockBase<?> block, C cost)
    {
        costs.put(block, cost);
    }

    /**
     * Sets {@code flag} for all blocks along the dominator path from {@code block} to the root
     * until a block it finds a block where {@code flag} is already set.
     */
    public final void setDominatorPath(E flag, AbstractBlockBase<?> block)
    {
        BitSet bitSet = flags.get(flag);
        if (bitSet == null)
        {
            bitSet = new BitSet(blocks.length);
            flags.put(flag, bitSet);
        }
        for (AbstractBlockBase<?> b = block; b != null && !bitSet.get(b.getId()); b = b.getDominator())
        {
            // mark block
            bitSet.set(b.getId());
        }
    }

    /**
     * Returns a {@link Stream} of flags associated with {@code block}.
     */
    public final Stream<E> getFlagsForBlock(AbstractBlockBase<?> block)
    {
        return getFlags().stream().filter(flag -> get(flag, block));
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
    public String getName(E flag)
    {
        return flag.toString();
    }
}
