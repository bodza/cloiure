package giraaff.lir;

import giraaff.asm.Label;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.StandardOp.BranchOp;
import giraaff.lir.StandardOp.JumpOp;

/**
 * LIR instructions such as {@link JumpOp} and {@link BranchOp} need to reference their target
 * {@link AbstractBlockBase}. However, direct references are not possible since the control flow
 * graph (and therefore successors lists) can be changed by optimizations - and fixing the
 * instructions is error prone. Therefore, we represent an edge to block B from block A via the
 * tuple {@code (A,
 * successor-index-of-B)}. That is, indirectly by storing the index into the successor list of A.
 * Note therefore that the successor list cannot be re-ordered.
 */
// @class LabelRef
public final class LabelRef
{
    // @field
    private final LIR lir;
    // @field
    private final AbstractBlockBase<?> block;
    // @field
    private final int suxIndex;

    /**
     * Returns a new reference to a successor of the given block.
     *
     * @param block The base block that contains the successor list.
     * @param suxIndex The index of the successor.
     * @return The newly created label reference.
     */
    public static LabelRef forSuccessor(final LIR __lir, final AbstractBlockBase<?> __block, final int __suxIndex)
    {
        return new LabelRef(__lir, __block, __suxIndex);
    }

    /**
     * Returns a new reference to a successor of the given block.
     *
     * @param block The base block that contains the successor list.
     * @param suxIndex The index of the successor.
     */
    // @cons
    private LabelRef(final LIR __lir, final AbstractBlockBase<?> __block, final int __suxIndex)
    {
        super();
        this.lir = __lir;
        this.block = __block;
        this.suxIndex = __suxIndex;
    }

    public AbstractBlockBase<?> getSourceBlock()
    {
        return block;
    }

    public AbstractBlockBase<?> getTargetBlock()
    {
        return block.getSuccessors()[suxIndex];
    }

    public Label label()
    {
        return ((StandardOp.LabelOp) lir.getLIRforBlock(getTargetBlock()).get(0)).getLabel();
    }
}
