package graalvm.compiler.lir.alloc;

import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;

import java.util.EnumSet;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

/**
 * Verifies that all virtual operands have been replaced by concrete values.
 */
public class AllocationStageVerifier extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        verifyLIR(lirGenRes.getLIR());
    }

    protected void verifyLIR(LIR lir)
    {
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            verifyBlock(lir, block);
        }
    }

    protected void verifyBlock(LIR lir, AbstractBlockBase<?> block)
    {
        for (LIRInstruction inst : lir.getLIRforBlock(block))
        {
            verifyInstruction(inst);
        }
    }

    protected void verifyInstruction(LIRInstruction inst)
    {
        inst.visitEachInput(this::verifyOperands);
        inst.visitEachOutput(this::verifyOperands);
        inst.visitEachAlive(this::verifyOperands);
        inst.visitEachTemp(this::verifyOperands);
    }

    /**
     * @param instruction
     * @param value
     * @param mode
     * @param flags
     */
    protected void verifyOperands(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
    {
        assert !isVirtualStackSlot(value) && !isVariable(value) : "Virtual values not allowed after allocation stage: " + value;
    }
}
