package giraaff.lir;

import java.util.ArrayList;
import java.util.List;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.BlockMap;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.gen.LIRGenerator;

///
// This class implements the overall container for the LIR graph and directs its construction,
// optimization, and finalization.
///
// @class LIR
public final class LIR extends LIRGenerator.VariableProvider
{
    // @field
    private final AbstractControlFlowGraph<?> ___cfg;

    ///
    // The linear-scan ordered list of blocks.
    ///
    // @field
    private final AbstractBlockBase<?>[] ___linearScanOrder;

    ///
    // The order in which the code is emitted.
    ///
    // @field
    private final AbstractBlockBase<?>[] ___codeEmittingOrder;

    ///
    // Map from {@linkplain AbstractBlockBase block} to {@linkplain LIRInstruction}s. Note that we
    // are using {@link ArrayList} instead of {@link List} to avoid interface dispatch.
    ///
    // @field
    private final BlockMap<ArrayList<LIRInstruction>> ___lirInstructions;

    // @field
    private boolean ___hasArgInCallerFrame;

    ///
    // Creates a new LIR instance for the specified compilation.
    ///
    // @cons
    public LIR(AbstractControlFlowGraph<?> __cfg, AbstractBlockBase<?>[] __linearScanOrder, AbstractBlockBase<?>[] __codeEmittingOrder)
    {
        super();
        this.___cfg = __cfg;
        this.___codeEmittingOrder = __codeEmittingOrder;
        this.___linearScanOrder = __linearScanOrder;
        this.___lirInstructions = new BlockMap<>(__cfg);
    }

    public AbstractControlFlowGraph<?> getControlFlowGraph()
    {
        return this.___cfg;
    }

    public ArrayList<LIRInstruction> getLIRforBlock(AbstractBlockBase<?> __block)
    {
        return this.___lirInstructions.get(__block);
    }

    public void setLIRforBlock(AbstractBlockBase<?> __block, ArrayList<LIRInstruction> __list)
    {
        this.___lirInstructions.put(__block, __list);
    }

    ///
    // Gets the linear scan ordering of blocks as an array.
    //
    // @return the blocks in linear scan order
    ///
    public AbstractBlockBase<?>[] linearScanOrder()
    {
        return this.___linearScanOrder;
    }

    public AbstractBlockBase<?>[] codeEmittingOrder()
    {
        return this.___codeEmittingOrder;
    }

    public void setHasArgInCallerFrame()
    {
        this.___hasArgInCallerFrame = true;
    }

    ///
    // Determines if any of the parameters to the method are passed via the stack where the
    // parameters are located in the caller's frame.
    ///
    public boolean hasArgInCallerFrame()
    {
        return this.___hasArgInCallerFrame;
    }

    ///
    // Gets the next non-{@code null} block in a list.
    //
    // @param blocks list of blocks
    // @param blockIndex index of the current block
    // @return the next block in the list that is none {@code null} or {@code null} if there is no
    //         such block
    ///
    public static AbstractBlockBase<?> getNextBlock(AbstractBlockBase<?>[] __blocks, int __blockIndex)
    {
        for (int __nextIndex = __blockIndex + 1; __nextIndex > 0 && __nextIndex < __blocks.length; __nextIndex++)
        {
            AbstractBlockBase<?> __nextBlock = __blocks[__nextIndex];
            if (__nextBlock != null)
            {
                return __nextBlock;
            }
        }
        return null;
    }

    public void resetLabels()
    {
        for (AbstractBlockBase<?> __block : codeEmittingOrder())
        {
            if (__block == null)
            {
                continue;
            }
            for (LIRInstruction __inst : this.___lirInstructions.get(__block))
            {
                if (__inst instanceof LabelOp)
                {
                    ((LabelOp) __inst).getLabel().reset();
                }
            }
        }
    }
}
