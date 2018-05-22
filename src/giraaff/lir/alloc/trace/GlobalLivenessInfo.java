package giraaff.lir.alloc.trace;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.Variable;

/**
 * Stores live in/live out variables and locations for each basic block.
 *
 * <em>Live variable information</em> is stored as an integer array containing <em>variable
 * indices</em>. The information is only stored once per <em>control-flow split</em> or
 * <em>control-merge</em>. In other words the live sets at the end of the source block and the
 * beginning of the target block of an edge are the same.
 */
public final class GlobalLivenessInfo
{
    public static final class Builder
    {
        private GlobalLivenessInfo info;
        public final int[] emptySet;

        public Builder(LIR lir)
        {
            this(lir.numVariables(), lir.getControlFlowGraph().getBlocks().length);
        }

        public Builder(int numVariables, int numBlocks)
        {
            info = new GlobalLivenessInfo(numVariables, numBlocks);
            emptySet = new int[0];
        }

        public GlobalLivenessInfo createLivenessInfo()
        {
            GlobalLivenessInfo livenessInfo = info;
            info = null;
            return livenessInfo;
        }

        public void setIncoming(AbstractBlockBase<?> block, int[] varsIn)
        {
            info.blockToVarIn[block.getId()] = varsIn;
        }

        public void setOutgoing(AbstractBlockBase<?> block, int[] varsOut)
        {
            info.blockToVarOut[block.getId()] = varsOut;
        }

        @SuppressWarnings("unused")
        public void addVariable(LIRInstruction op, Variable var)
        {
            info.variables[var.index] = var;
        }
    }

    private final Variable[] variables;
    private final int[][] blockToVarIn;
    private final int[][] blockToVarOut;
    private final Value[][] blockToLocIn;
    private final Value[][] blockToLocOut;

    private GlobalLivenessInfo(int numVariables, int numBlocks)
    {
        variables = new Variable[numVariables];
        blockToVarIn = new int[numBlocks][];
        blockToVarOut = new int[numBlocks][];
        blockToLocIn = new Value[numBlocks][];
        blockToLocOut = new Value[numBlocks][];
    }

    public Variable getVariable(int varNum)
    {
        return variables[varNum];
    }

    public int[] getBlockOut(AbstractBlockBase<?> block)
    {
        return blockToVarOut[block.getId()];
    }

    public int[] getBlockIn(AbstractBlockBase<?> block)
    {
        return blockToVarIn[block.getId()];
    }

    public void setInLocations(AbstractBlockBase<?> block, Value[] values)
    {
        blockToLocIn[block.getId()] = values;
    }

    public void setOutLocations(AbstractBlockBase<?> block, Value[] values)
    {
        blockToLocOut[block.getId()] = values;
    }

    public Value[] getInLocation(AbstractBlockBase<?> block)
    {
        return blockToLocIn[block.getId()];
    }

    public Value[] getOutLocation(AbstractBlockBase<?> block)
    {
        return blockToLocOut[block.getId()];
    }

    public static boolean storesIncoming(AbstractBlockBase<?> block)
    {
        return block.getPredecessorCount() != 1;
    }

    public static boolean storesOutgoing(AbstractBlockBase<?> block)
    {
        /*
         * The second condition handles non-critical empty blocks, introduced, e.g., by two
         * consecutive loop-exits.
         */
        return block.getSuccessorCount() != 1 || block.getSuccessors()[0].getPredecessorCount() == 1;
    }
}
