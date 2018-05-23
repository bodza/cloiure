package giraaff.lir;

import java.util.ArrayList;
import java.util.List;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.BlockMap;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.gen.LIRGenerator;
import giraaff.options.OptionValues;

/**
 * This class implements the overall container for the LIR graph and directs its construction,
 * optimization, and finalization.
 */
public final class LIR extends LIRGenerator.VariableProvider
{
    private final AbstractControlFlowGraph<?> cfg;

    /**
     * The linear-scan ordered list of blocks.
     */
    private final AbstractBlockBase<?>[] linearScanOrder;

    /**
     * The order in which the code is emitted.
     */
    private final AbstractBlockBase<?>[] codeEmittingOrder;

    /**
     * Map from {@linkplain AbstractBlockBase block} to {@linkplain LIRInstruction}s. Note that we
     * are using {@link ArrayList} instead of {@link List} to avoid interface dispatch.
     */
    private final BlockMap<ArrayList<LIRInstruction>> lirInstructions;

    private boolean hasArgInCallerFrame;

    private final OptionValues options;

    /**
     * Creates a new LIR instance for the specified compilation.
     */
    public LIR(AbstractControlFlowGraph<?> cfg, AbstractBlockBase<?>[] linearScanOrder, AbstractBlockBase<?>[] codeEmittingOrder, OptionValues options)
    {
        this.cfg = cfg;
        this.codeEmittingOrder = codeEmittingOrder;
        this.linearScanOrder = linearScanOrder;
        this.lirInstructions = new BlockMap<>(cfg);
        this.options = options;
    }

    public AbstractControlFlowGraph<?> getControlFlowGraph()
    {
        return cfg;
    }

    public OptionValues getOptions()
    {
        return options;
    }

    /**
     * Determines if any instruction in the LIR has debug info associated with it.
     */
    public boolean hasDebugInfo()
    {
        for (AbstractBlockBase<?> b : linearScanOrder())
        {
            for (LIRInstruction op : getLIRforBlock(b))
            {
                if (op.hasState())
                {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<LIRInstruction> getLIRforBlock(AbstractBlockBase<?> block)
    {
        return lirInstructions.get(block);
    }

    public void setLIRforBlock(AbstractBlockBase<?> block, ArrayList<LIRInstruction> list)
    {
        lirInstructions.put(block, list);
    }

    /**
     * Gets the linear scan ordering of blocks as an array.
     *
     * @return the blocks in linear scan order
     */
    public AbstractBlockBase<?>[] linearScanOrder()
    {
        return linearScanOrder;
    }

    public AbstractBlockBase<?>[] codeEmittingOrder()
    {
        return codeEmittingOrder;
    }

    public void setHasArgInCallerFrame()
    {
        hasArgInCallerFrame = true;
    }

    /**
     * Determines if any of the parameters to the method are passed via the stack where the
     * parameters are located in the caller's frame.
     */
    public boolean hasArgInCallerFrame()
    {
        return hasArgInCallerFrame;
    }

    /**
     * Gets the next non-{@code null} block in a list.
     *
     * @param blocks list of blocks
     * @param blockIndex index of the current block
     * @return the next block in the list that is none {@code null} or {@code null} if there is no
     *         such block
     */
    public static AbstractBlockBase<?> getNextBlock(AbstractBlockBase<?>[] blocks, int blockIndex)
    {
        for (int nextIndex = blockIndex + 1; nextIndex > 0 && nextIndex < blocks.length; nextIndex++)
        {
            AbstractBlockBase<?> nextBlock = blocks[nextIndex];
            if (nextBlock != null)
            {
                return nextBlock;
            }
        }
        return null;
    }

    /**
     * Gets the exception edge (if any) originating at a given operation.
     */
    public static LabelRef getExceptionEdge(LIRInstruction op)
    {
        final LabelRef[] exceptionEdge = { null };
        op.forEachState(state ->
        {
            if (state.exceptionEdge != null)
            {
                exceptionEdge[0] = state.exceptionEdge;
            }
        });
        return exceptionEdge[0];
    }

    public void resetLabels()
    {
        for (AbstractBlockBase<?> block : codeEmittingOrder())
        {
            if (block == null)
            {
                continue;
            }
            for (LIRInstruction inst : lirInstructions.get(block))
            {
                if (inst instanceof LabelOp)
                {
                    ((LabelOp) inst).getLabel().reset();
                }
            }
        }
    }
}
