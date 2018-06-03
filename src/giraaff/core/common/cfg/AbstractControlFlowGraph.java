package giraaff.core.common.cfg;

import java.util.Collection;

// @iface AbstractControlFlowGraph
public interface AbstractControlFlowGraph<T extends AbstractBlockBase<T>>
{
    // @field
    int BLOCK_ID_INITIAL = -1;
    // @field
    int BLOCK_ID_VISITED = -2;

    /**
     * Returns the list blocks contained in this control flow graph.
     *
     * It is guaranteed that the blocks are numbered and ordered according
     * to a reverse post order traversal of the control flow graph.
     */
    T[] getBlocks();

    Collection<Loop<T>> getLoops();

    T getStartBlock();

    /**
     * True if block {@code a} is dominated by block {@code b}.
     */
    static boolean isDominatedBy(AbstractBlockBase<?> __a, AbstractBlockBase<?> __b)
    {
        int __domNumberA = __a.getDominatorNumber();
        int __domNumberB = __b.getDominatorNumber();
        return __domNumberA >= __domNumberB && __domNumberA <= __b.getMaxChildDominatorNumber();
    }

    /**
     * True if block {@code a} dominates block {@code b} and {@code a} is not identical block to
     * {@code b}.
     */
    static boolean strictlyDominates(AbstractBlockBase<?> __a, AbstractBlockBase<?> __b)
    {
        return __a != __b && dominates(__a, __b);
    }

    /**
     * True if block {@code a} dominates block {@code b}.
     */
    static boolean dominates(AbstractBlockBase<?> __a, AbstractBlockBase<?> __b)
    {
        return isDominatedBy(__b, __a);
    }

    /**
     * Calculates the common dominator of two blocks.
     *
     * Note that this algorithm makes use of special properties regarding the numbering of blocks.
     *
     * @see #getBlocks()
     */
    static AbstractBlockBase<?> commonDominator(AbstractBlockBase<?> __a, AbstractBlockBase<?> __b)
    {
        if (__a == null)
        {
            return __b;
        }
        else if (__b == null)
        {
            return __a;
        }
        else if (__a == __b)
        {
            return __a;
        }
        else
        {
            int __aDomDepth = __a.getDominatorDepth();
            int __bDomDepth = __b.getDominatorDepth();
            AbstractBlockBase<?> __aTemp;
            AbstractBlockBase<?> __bTemp;
            if (__aDomDepth > __bDomDepth)
            {
                __aTemp = __a;
                __bTemp = __b;
            }
            else
            {
                __aTemp = __b;
                __bTemp = __a;
            }
            return commonDominatorHelper(__aTemp, __bTemp);
        }
    }

    static AbstractBlockBase<?> commonDominatorHelper(AbstractBlockBase<?> __a, AbstractBlockBase<?> __b)
    {
        int __domNumberA = __a.getDominatorNumber();
        AbstractBlockBase<?> __result = __b;
        while (__domNumberA < __result.getDominatorNumber())
        {
            __result = __result.getDominator();
        }
        while (__domNumberA > __result.getMaxChildDominatorNumber())
        {
            __result = __result.getDominator();
        }
        return __result;
    }

    /**
     * @see AbstractControlFlowGraph#commonDominator(AbstractBlockBase, AbstractBlockBase)
     */
    @SuppressWarnings("unchecked")
    static <T extends AbstractBlockBase<T>> T commonDominatorTyped(T __a, T __b)
    {
        return (T) commonDominator(__a, __b);
    }
}
