package giraaff.lir.alloc.trace;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.Loop;
import giraaff.debug.GraalError;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Variable;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;

/**
 * Constructs {@link GlobalLivenessInfo global liveness information}.
 */
public final class GlobalLivenessAnalysisPhase extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        Analyser ssiBuilder = new Analyser(lirGenRes.getLIR());
        ssiBuilder.build();
        ssiBuilder.finish();
        GlobalLivenessInfo livenessInfo = ssiBuilder.getLivenessInfo();
        context.contextAdd(livenessInfo);
    }

    private final class Analyser
    {
        /**
         * Bit map specifying which operands are live upon entry to this block. These are values
         * used in this block or any of its successors where such value are not defined in this
         * block. The bit index of an operand is its {@linkplain #operandNumber operand number}.
         */
        private final BitSet[] liveIns;

        /**
         * Bit map specifying which operands are live upon exit from this block. These are values
         * used in a successor block that are either defined in this block or were live upon entry
         * to this block. The bit index of an operand is its {@linkplain #operandNumber operand
         * number}.
         */
        private final BitSet[] liveOuts;

        private final AbstractBlockBase<?>[] blocks;

        private final Value[] operands;

        private final LIR lir;

        private final GlobalLivenessInfo.Builder livenessInfoBuilder;

        Analyser(LIR lir)
        {
            int numBlocks = lir.getControlFlowGraph().getBlocks().length;
            this.liveIns = new BitSet[numBlocks];
            this.liveOuts = new BitSet[numBlocks];
            this.blocks = lir.getControlFlowGraph().getBlocks();
            this.lir = lir;
            this.operands = new Value[lir.numVariables()];
            this.livenessInfoBuilder = new GlobalLivenessInfo.Builder(lir);
        }

        private BitSet getLiveIn(final AbstractBlockBase<?> block)
        {
            return liveIns[block.getId()];
        }

        private BitSet getLiveOut(final AbstractBlockBase<?> block)
        {
            return liveOuts[block.getId()];
        }

        private void setLiveIn(final AbstractBlockBase<?> block, final BitSet liveIn)
        {
            liveIns[block.getId()] = liveIn;
        }

        private void setLiveOut(final AbstractBlockBase<?> block, final BitSet liveOut)
        {
            liveOuts[block.getId()] = liveOut;
        }

        private void buildIntern()
        {
            computeLiveness();
        }

        /**
         * Gets the size of the {@link #liveIns} and {@link #liveOuts} sets for a basic block.
         */
        private int liveSetSize()
        {
            return lir.numVariables();
        }

        private int operandNumber(Value operand)
        {
            if (LIRValueUtil.isVariable(operand))
            {
                return LIRValueUtil.asVariable(operand).index;
            }
            throw GraalError.shouldNotReachHere("Can only handle Variables: " + operand);
        }

        /**
         * Computes live sets for each block.
         */
        private void computeLiveness()
        {
            // iterate all blocks
            for (int i = blocks.length - 1; i >= 0; i--)
            {
                final AbstractBlockBase<?> block = blocks[i];
                final BitSet liveIn = mergeLiveSets(block);
                setLiveOut(block, (BitSet) liveIn.clone());

                InstructionValueConsumer useConsumer = new InstructionValueConsumer()
                {
                    @Override
                    public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
                    {
                        processUse(liveIn, operand);
                    }
                };
                InstructionValueConsumer defConsumer = new InstructionValueConsumer()
                {
                    @Override
                    public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
                    {
                        processDef(liveIn, op, operand);
                    }
                };

                // iterate all instructions of the block
                ArrayList<LIRInstruction> instructions = getLIR().getLIRforBlock(block);
                for (int j = instructions.size() - 1; j >= 0; j--)
                {
                    final LIRInstruction op = instructions.get(j);

                    op.visitEachOutput(defConsumer);
                    op.visitEachTemp(defConsumer);
                    op.visitEachState(useConsumer);
                    op.visitEachAlive(useConsumer);
                    op.visitEachInput(useConsumer);
                } // end of instruction iteration

                setLiveIn(block, liveIn);
                if (block.isLoopHeader())
                {
                    handleLoopHeader(block.getLoop(), liveIn);
                }
            } // end of block iteration
        }

        /**
         * All variables live at the beginning of a loop are live throughout the loop.
         */
        private void handleLoopHeader(Loop<?> loop, BitSet live)
        {
            for (AbstractBlockBase<?> block : loop.getBlocks())
            {
                getLiveIn(block).or(live);
                getLiveOut(block).or(live);
            }
        }

        private BitSet mergeLiveSets(final AbstractBlockBase<?> block)
        {
            final BitSet liveOut = new BitSet(liveSetSize());
            for (AbstractBlockBase<?> successor : block.getSuccessors())
            {
                BitSet succLiveIn = getLiveIn(successor);
                if (succLiveIn != null)
                {
                    liveOut.or(succLiveIn);
                }
            }
            return liveOut;
        }

        private void processUse(final BitSet liveGen, Value operand)
        {
            if (LIRValueUtil.isVariable(operand))
            {
                int operandNum = operandNumber(operand);
                liveGen.set(operandNum);
            }
        }

        private void processDef(final BitSet liveGen, LIRInstruction op, Value operand)
        {
            if (LIRValueUtil.isVariable(operand))
            {
                recordVariable(op, LIRValueUtil.asVariable(operand));
                int operandNum = operandNumber(operand);
                if (operands[operandNum] == null)
                {
                    operands[operandNum] = operand;
                }
                liveGen.clear(operandNum);
            }
        }

        private LIR getLIR()
        {
            return lir;
        }

        public void build()
        {
            buildIntern();
            // check that the liveIn set of the first block is empty
            AbstractBlockBase<?> startBlock = getLIR().getControlFlowGraph().getStartBlock();
            if (getLiveIn(startBlock).cardinality() != 0)
            {
                // bailout if this occurs in product mode.
                throw new GraalError("liveIn set of first block must be empty: " + getLiveIn(startBlock));
            }
        }

        public void finish()
        {
            // iterate all blocks in reverse order
            for (AbstractBlockBase<?> block : (AbstractBlockBase<?>[]) lir.getControlFlowGraph().getBlocks())
            {
                buildIncoming(block);
                buildOutgoing(block);
            }
        }

        public GlobalLivenessInfo getLivenessInfo()
        {
            return livenessInfoBuilder.createLivenessInfo();
        }

        private void buildIncoming(AbstractBlockBase<?> block)
        {
            if (!GlobalLivenessInfo.storesIncoming(block))
            {
                return;
            }

            final int[] liveInArray;
            if (block.getPredecessorCount() == 0)
            {
                // start block
                liveInArray = livenessInfoBuilder.emptySet;
            }
            else
            {
                /*
                 * Collect live out of predecessors since there might be values not used in this
                 * block which might cause out/in mismatch. Per construction the live sets of all
                 * predecessors are equal.
                 */
                BitSet predLiveOut = getLiveOut(block.getPredecessors()[0]);
                liveInArray = predLiveOut.isEmpty() ? livenessInfoBuilder.emptySet : bitSetToIntArray(predLiveOut);
            }

            livenessInfoBuilder.setIncoming(block, liveInArray);
            // reuse the same array for outgoing variables in predecessors
            for (AbstractBlockBase<?> pred : block.getPredecessors())
            {
                livenessInfoBuilder.setOutgoing(pred, liveInArray);
            }
        }

        private void buildOutgoing(AbstractBlockBase<?> block)
        {
            BitSet liveOut = getLiveOut(block);
            if (!GlobalLivenessInfo.storesOutgoing(block))
            {
                return;
            }
            int[] liveOutArray = liveOut.isEmpty() ? livenessInfoBuilder.emptySet : bitSetToIntArray(liveOut);

            livenessInfoBuilder.setOutgoing(block, liveOutArray);
            // reuse the same array for incoming variables in successors
            for (AbstractBlockBase<?> succ : block.getSuccessors())
            {
                livenessInfoBuilder.setIncoming(succ, liveOutArray);
            }
        }

        private int[] bitSetToIntArray(BitSet live)
        {
            int[] vars = new int[live.cardinality()];
            int cnt = 0;
            for (int i = live.nextSetBit(0); i >= 0; i = live.nextSetBit(i + 1), cnt++)
            {
                vars[cnt] = i;
            }
            return vars;
        }

        private void recordVariable(LIRInstruction op, Variable var)
        {
            livenessInfoBuilder.addVariable(op, var);
        }
    }
}
