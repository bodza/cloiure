package graalvm.compiler.hotspot.lir;

import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import java.util.ArrayList;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.hotspot.HotSpotLIRGenerationResult;
import graalvm.compiler.hotspot.stubs.Stub;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.SaveRegistersOp;
import graalvm.compiler.lir.gen.DiagnosticLIRGeneratorTool;
import graalvm.compiler.lir.gen.DiagnosticLIRGeneratorTool.ZapRegistersAfterInstruction;
import graalvm.compiler.lir.gen.DiagnosticLIRGeneratorTool.ZapStackArgumentSpaceBeforeInstruction;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Inserts a {@link DiagnosticLIRGeneratorTool#createZapRegisters ZapRegistersOp} after
 * {@link ZapRegistersAfterInstruction} for stubs and
 * {@link DiagnosticLIRGeneratorTool#zapArgumentSpace ZapArgumentSpaceOp} after
 * {@link ZapStackArgumentSpaceBeforeInstruction} for all compiles.
 */
public final class HotSpotZapRegistersPhase extends PostAllocationOptimizationPhase {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context) {
        Stub stub = ((HotSpotLIRGenerationResult) lirGenRes).getStub();
        boolean zapRegisters = stub != null && !stub.preservesRegisters();
        boolean zapStack = false;
        for (AllocatableValue arg : lirGenRes.getCallingConvention().getArguments()) {
            if (isStackSlot(arg)) {
                zapStack = true;
                break;
            }
        }
        if (zapRegisters || zapStack) {
            LIR lir = lirGenRes.getLIR();
            processLIR(context.diagnosticLirGenTool, (HotSpotLIRGenerationResult) lirGenRes, lir, zapRegisters, zapStack);
        }
    }

    private static void processLIR(DiagnosticLIRGeneratorTool diagnosticLirGenTool, HotSpotLIRGenerationResult res, LIR lir, boolean zapRegisters, boolean zapStack) {
        LIRInsertionBuffer buffer = new LIRInsertionBuffer();
        for (AbstractBlockBase<?> block : lir.codeEmittingOrder()) {
            if (block != null) {
                processBlock(diagnosticLirGenTool, res, lir, buffer, block, zapRegisters, zapStack);
            }
        }
    }

    @SuppressWarnings("try")
    private static void processBlock(DiagnosticLIRGeneratorTool diagnosticLirGenTool, HotSpotLIRGenerationResult res, LIR lir, LIRInsertionBuffer buffer, AbstractBlockBase<?> block,
                    boolean zapRegisters, boolean zapStack) {
        DebugContext debug = lir.getDebug();
        try (Indent indent = debug.logAndIndent("Process block %s", block)) {
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            buffer.init(instructions);
            for (int index = 0; index < instructions.size(); index++) {
                LIRInstruction inst = instructions.get(index);
                if (zapStack && inst instanceof ZapStackArgumentSpaceBeforeInstruction) {
                    LIRInstruction zap = diagnosticLirGenTool.zapArgumentSpace();
                    if (zap != null) {
                        buffer.append(index, zap);
                    }
                }
                if (zapRegisters && inst instanceof ZapRegistersAfterInstruction) {
                    LIRFrameState state = getLIRState(inst);
                    if (state != null) {
                        SaveRegistersOp zap = diagnosticLirGenTool.createZapRegisters();
                        SaveRegistersOp old = res.getCalleeSaveInfo().put(state, zap);
                        assert old == null : "Already another SaveRegisterOp registered! " + old;
                        buffer.append(index + 1, (LIRInstruction) zap);
                        debug.log("Insert ZapRegister after %s", inst);
                    }
                }
            }
            buffer.finish();
        }
    }

    /**
     * Returns the {@link LIRFrameState} of an instruction.
     */
    private static LIRFrameState getLIRState(LIRInstruction inst) {
        final LIRFrameState[] lirState = {null};
        inst.forEachState(state -> {
            assert lirState[0] == null : "Multiple states: " + inst;
            lirState[0] = state;
        });
        assert lirState[0] != null : "No state: " + inst;
        return lirState[0];
    }

}
