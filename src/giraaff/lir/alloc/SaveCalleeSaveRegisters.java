package giraaff.lir.alloc;

import java.util.ArrayList;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.PlatformKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
import giraaff.lir.Variable;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.PreAllocationOptimizationPhase;
import giraaff.lir.util.RegisterMap;

// @class SaveCalleeSaveRegisters
public final class SaveCalleeSaveRegisters extends PreAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PreAllocationOptimizationContext context)
    {
        RegisterArray calleeSaveRegisters = lirGenRes.getRegisterConfig().getCalleeSaveRegisters();
        if (calleeSaveRegisters == null || calleeSaveRegisters.size() == 0)
        {
            return;
        }
        LIR lir = lirGenRes.getLIR();
        RegisterMap<Variable> savedRegisters = saveAtEntry(lir, context.lirGen, lirGenRes, calleeSaveRegisters, target.arch);

        for (AbstractBlockBase<?> block : lir.codeEmittingOrder())
        {
            if (block == null)
            {
                continue;
            }
            if (block.getSuccessorCount() == 0)
            {
                restoreAtExit(lir, context.lirGen.getSpillMoveFactory(), lirGenRes, savedRegisters, block);
            }
        }
    }

    private static RegisterMap<Variable> saveAtEntry(LIR lir, LIRGeneratorTool lirGen, LIRGenerationResult lirGenRes, RegisterArray calleeSaveRegisters, Architecture arch)
    {
        AbstractBlockBase<?> startBlock = lir.getControlFlowGraph().getStartBlock();
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(startBlock);
        int insertionIndex = 1;
        LIRInsertionBuffer buffer = new LIRInsertionBuffer();
        buffer.init(instructions);
        StandardOp.LabelOp entry = (StandardOp.LabelOp) instructions.get(insertionIndex - 1);
        RegisterValue[] savedRegisterValues = new RegisterValue[calleeSaveRegisters.size()];
        int savedRegisterValueIndex = 0;
        RegisterMap<Variable> saveMap = new RegisterMap<>(arch);
        for (Register register : calleeSaveRegisters)
        {
            PlatformKind registerPlatformKind = arch.getLargestStorableKind(register.getRegisterCategory());
            LIRKind lirKind = LIRKind.value(registerPlatformKind);
            RegisterValue registerValue = register.asValue(lirKind);
            Variable saveVariable = lirGen.newVariable(lirKind);
            LIRInstruction save = lirGen.getSpillMoveFactory().createMove(saveVariable, registerValue);
            buffer.append(insertionIndex, save);
            save.setComment(lirGenRes, "SaveCalleeSavedRegisters: saveAtEntry");
            saveMap.put(register, saveVariable);
            savedRegisterValues[savedRegisterValueIndex++] = registerValue;
        }
        entry.addIncomingValues(savedRegisterValues);
        buffer.finish();
        return saveMap;
    }

    private static void restoreAtExit(LIR lir, LIRGeneratorTool.MoveFactory moveFactory, LIRGenerationResult lirGenRes, RegisterMap<Variable> calleeSaveRegisters, AbstractBlockBase<?> block)
    {
        ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
        int insertionIndex = instructions.size() - 1;
        LIRInsertionBuffer buffer = new LIRInsertionBuffer();
        buffer.init(instructions);
        calleeSaveRegisters.forEach((Register register, Variable saved) ->
        {
            LIRInstruction restore = moveFactory.createMove(register.asValue(saved.getValueKind()), saved);
            buffer.append(insertionIndex, restore);
            restore.setComment(lirGenRes, "SaveCalleeSavedRegisters: restoreAtExit");
        });
        buffer.finish();
    }
}
