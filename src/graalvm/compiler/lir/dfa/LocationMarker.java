package graalvm.compiler.lir.dfa;

import java.util.ArrayList;
import java.util.EnumSet;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.lir.InstructionStateProcedure;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.ValueConsumer;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.util.ValueSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

public abstract class LocationMarker<S extends ValueSet<S>>
{
    private final LIR lir;
    private final BlockMap<S> liveInMap;
    private final BlockMap<S> liveOutMap;

    protected final FrameMap frameMap;

    protected LocationMarker(LIR lir, FrameMap frameMap)
    {
        this.lir = lir;
        this.frameMap = frameMap;
        liveInMap = new BlockMap<>(lir.getControlFlowGraph());
        liveOutMap = new BlockMap<>(lir.getControlFlowGraph());
    }

    protected abstract S newLiveValueSet();

    protected abstract boolean shouldProcessValue(Value operand);

    protected abstract void processState(LIRInstruction op, LIRFrameState info, S values);

    void build()
    {
        AbstractBlockBase<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        UniqueWorkList worklist = new UniqueWorkList(blocks.length);
        for (int i = blocks.length - 1; i >= 0; i--)
        {
            worklist.add(blocks[i]);
        }
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            liveInMap.put(block, newLiveValueSet());
        }
        while (!worklist.isEmpty())
        {
            AbstractBlockBase<?> block = worklist.poll();
            processBlock(block, worklist);
        }
    }

    /**
     * Merge outSet with in-set of successors.
     */
    private boolean updateOutBlock(AbstractBlockBase<?> block)
    {
        S union = newLiveValueSet();
        for (AbstractBlockBase<?> succ : block.getSuccessors())
        {
            union.putAll(liveInMap.get(succ));
        }
        S outSet = liveOutMap.get(block);
        // check if changed
        if (outSet == null || !union.equals(outSet))
        {
            liveOutMap.put(block, union);
            return true;
        }
        return false;
    }

    private void processBlock(AbstractBlockBase<?> block, UniqueWorkList worklist)
    {
        if (updateOutBlock(block))
        {
            currentSet = liveOutMap.get(block).copy();
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            for (int i = instructions.size() - 1; i >= 0; i--)
            {
                LIRInstruction inst = instructions.get(i);
                processInstructionBottomUp(inst);
            }
            liveInMap.put(block, currentSet);
            currentSet = null;
            for (AbstractBlockBase<?> b : block.getPredecessors())
            {
                worklist.add(b);
            }
        }
    }

    private static final EnumSet<OperandFlag> REGISTER_FLAG_SET = EnumSet.of(OperandFlag.REG);

    private S currentSet;

    /**
     * Process all values of an instruction bottom-up, i.e. definitions before usages. Values that
     * start or end at the current operation are not included.
     */
    private void processInstructionBottomUp(LIRInstruction op)
    {
        // kills
        op.visitEachTemp(defConsumer);
        op.visitEachOutput(defConsumer);
        if (frameMap != null && op.destroysCallerSavedRegisters())
        {
            for (Register reg : frameMap.getRegisterConfig().getCallerSaveRegisters())
            {
                PlatformKind kind = frameMap.getTarget().arch.getLargestStorableKind(reg.getRegisterCategory());
                defConsumer.visitValue(reg.asValue(LIRKind.value(kind)), OperandMode.TEMP, REGISTER_FLAG_SET);
            }
        }

        // gen - values that are considered alive for this state
        op.visitEachAlive(useConsumer);
        op.visitEachState(useConsumer);
        // mark locations
        op.forEachState(stateConsumer);
        // gen
        op.visitEachInput(useConsumer);
    }

    InstructionStateProcedure stateConsumer = new InstructionStateProcedure()
    {
        @Override
        public void doState(LIRInstruction inst, LIRFrameState info)
        {
            processState(inst, info, currentSet);
        }
    };

    ValueConsumer useConsumer = new ValueConsumer()
    {
        @Override
        public void visitValue(Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            if (shouldProcessValue(operand))
            {
                // no need to insert values and derived reference
                currentSet.put(operand);
            }
        }
    };

    ValueConsumer defConsumer = new ValueConsumer()
    {
        @Override
        public void visitValue(Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            if (shouldProcessValue(operand))
            {
                currentSet.remove(operand);
            }
        }
    };
}
