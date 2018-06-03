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
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PreAllocationOptimizationContext __context)
    {
        RegisterArray __calleeSaveRegisters = __lirGenRes.getRegisterConfig().getCalleeSaveRegisters();
        if (__calleeSaveRegisters == null || __calleeSaveRegisters.size() == 0)
        {
            return;
        }
        LIR __lir = __lirGenRes.getLIR();
        RegisterMap<Variable> __savedRegisters = saveAtEntry(__lir, __context.lirGen, __lirGenRes, __calleeSaveRegisters, __target.arch);

        for (AbstractBlockBase<?> __block : __lir.codeEmittingOrder())
        {
            if (__block == null)
            {
                continue;
            }
            if (__block.getSuccessorCount() == 0)
            {
                restoreAtExit(__lir, __context.lirGen.getSpillMoveFactory(), __lirGenRes, __savedRegisters, __block);
            }
        }
    }

    private static RegisterMap<Variable> saveAtEntry(LIR __lir, LIRGeneratorTool __lirGen, LIRGenerationResult __lirGenRes, RegisterArray __calleeSaveRegisters, Architecture __arch)
    {
        AbstractBlockBase<?> __startBlock = __lir.getControlFlowGraph().getStartBlock();
        ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__startBlock);
        int __insertionIndex = 1;
        LIRInsertionBuffer __buffer = new LIRInsertionBuffer();
        __buffer.init(__instructions);
        StandardOp.LabelOp __entry = (StandardOp.LabelOp) __instructions.get(__insertionIndex - 1);
        RegisterValue[] __savedRegisterValues = new RegisterValue[__calleeSaveRegisters.size()];
        int __savedRegisterValueIndex = 0;
        RegisterMap<Variable> __saveMap = new RegisterMap<>(__arch);
        for (Register __register : __calleeSaveRegisters)
        {
            PlatformKind __registerPlatformKind = __arch.getLargestStorableKind(__register.getRegisterCategory());
            LIRKind __lirKind = LIRKind.value(__registerPlatformKind);
            RegisterValue __registerValue = __register.asValue(__lirKind);
            Variable __saveVariable = __lirGen.newVariable(__lirKind);
            LIRInstruction __save = __lirGen.getSpillMoveFactory().createMove(__saveVariable, __registerValue);
            __buffer.append(__insertionIndex, __save);
            __saveMap.put(__register, __saveVariable);
            __savedRegisterValues[__savedRegisterValueIndex++] = __registerValue;
        }
        __entry.addIncomingValues(__savedRegisterValues);
        __buffer.finish();
        return __saveMap;
    }

    private static void restoreAtExit(LIR __lir, LIRGeneratorTool.MoveFactory __moveFactory, LIRGenerationResult __lirGenRes, RegisterMap<Variable> __calleeSaveRegisters, AbstractBlockBase<?> __block)
    {
        ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
        int __insertionIndex = __instructions.size() - 1;
        LIRInsertionBuffer __buffer = new LIRInsertionBuffer();
        __buffer.init(__instructions);
        __calleeSaveRegisters.forEach((Register __register, Variable __saved) ->
        {
            LIRInstruction __restore = __moveFactory.createMove(__register.asValue(__saved.getValueKind()), __saved);
            __buffer.append(__insertionIndex, __restore);
        });
        __buffer.finish();
    }
}
