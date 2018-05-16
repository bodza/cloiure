package graalvm.compiler.lir.ssa;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isJavaConstant;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.InstructionValueConsumer;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

import jdk.vm.ci.meta.Value;

final class SSAVerifier
{
    private static class Entry
    {
        private final LIRInstruction inst;
        private final AbstractBlockBase<?> block;

        Entry(LIRInstruction inst, AbstractBlockBase<?> block)
        {
            this.inst = inst;
            this.block = block;
        }
    }

    private final LIR lir;
    private final BitSet visited;
    private final HashMap<Value, Entry> defined;
    private AbstractBlockBase<?> currentBlock;

    SSAVerifier(LIR lir)
    {
        this.lir = lir;
        this.visited = new BitSet(lir.getControlFlowGraph().getBlocks().length);
        this.defined = new HashMap<>();
    }

    @SuppressWarnings("try")
    public boolean verify()
    {
        DebugContext debug = lir.getDebug();
        try (DebugContext.Scope s = debug.scope("SSAVerifier", lir))
        {
            for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
            {
                doBlock(block);
            }
        }
        catch (Throwable e)
        {
            throw debug.handle(e);
        }
        return true;
    }

    @SuppressWarnings("try")
    private void doBlock(AbstractBlockBase<?> b)
    {
        if (visited.get(b.getId()))
        {
            return;
        }
        for (AbstractBlockBase<?> pred : b.getPredecessors())
        {
            if (!b.isLoopHeader() || !pred.isLoopEnd())
            {
                doBlock(pred);
            }
        }
        try (Indent indent = lir.getDebug().logAndIndent(DebugContext.INFO_LEVEL, "handle block %s", b))
        {
            assert verifyBlock(b);
        }
    }

    private boolean verifyBlock(AbstractBlockBase<?> block)
    {
        currentBlock = block;
        assert !visited.get(block.getId()) : "Block already visited: " + block;
        visited.set(block.getId());
        for (LIRInstruction op : lir.getLIRforBlock(block))
        {
            op.visitEachAlive(this::useConsumer);
            op.visitEachState(this::useConsumer);
            op.visitEachInput(this::useConsumer);

            op.visitEachTemp(this::defConsumer);
            op.visitEachOutput(this::defConsumer);
        }
        currentBlock = null;
        return true;
    }

    /**
     * @see InstructionValueConsumer
     * @param mode
     * @param flags
     */
    private void useConsumer(LIRInstruction inst, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
    {
        if (shouldProcess(value))
        {
            assert defined.containsKey(value) || flags.contains(OperandFlag.UNINITIALIZED) : String.format("Value %s used at instruction %s in block %s but never defined", value, inst, currentBlock);
        }
    }

    /**
     * @see InstructionValueConsumer
     * @param mode
     * @param flags
     */
    private void defConsumer(LIRInstruction inst, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
    {
        if (shouldProcess(value))
        {
            assert !defined.containsKey(value) : String.format("Value %s redefined at %s but never defined (previous definition %s in block %s)", value, inst, defined.get(value).inst, defined.get(value).block);
            defined.put(value, new Entry(inst, currentBlock));
        }
    }

    private static boolean shouldProcess(Value value)
    {
        return !value.equals(Value.ILLEGAL) && !isJavaConstant(value) && !isRegister(value) && !isStackSlotValue(value);
    }
}
