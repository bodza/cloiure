package graalvm.compiler.hotspot;

import java.util.ArrayList;

import graalvm.compiler.asm.Assembler;
import graalvm.compiler.asm.Assembler.InstructionCounter;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.ConstantValue;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.StandardOp.BlockEndOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.gen.DiagnosticLIRGeneratorTool;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

public class HotSpotInstructionProfiling extends PostAllocationOptimizationPhase {
    public static final String COUNTER_GROUP = "INSTRUCTION_COUNTER";
    private final String[] instructionsToProfile;

    public HotSpotInstructionProfiling(String instructionsToProfile) {
        this.instructionsToProfile = instructionsToProfile.split(",");
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context) {
        new Analyzer(target, lirGenRes.getCompilationUnitName(), lirGenRes.getLIR(), context.diagnosticLirGenTool).run();
    }

    private class Analyzer {
        private final TargetDescription target;
        private final LIR lir;
        private final DiagnosticLIRGeneratorTool diagnosticLirGenTool;
        private final LIRInsertionBuffer buffer;
        private final String compilationUnitName;

        Analyzer(TargetDescription target, String compilationUnitName, LIR lir, DiagnosticLIRGeneratorTool diagnosticLirGenTool) {
            this.target = target;
            this.lir = lir;
            this.compilationUnitName = compilationUnitName;
            this.diagnosticLirGenTool = diagnosticLirGenTool;
            this.buffer = new LIRInsertionBuffer();
        }

        public void run() {
            for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks()) {
                doBlock(block);
            }
        }

        public void doBlock(AbstractBlockBase<?> block) {
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            assert instructions.size() >= 2 : "Malformed block: " + block + ", " + instructions;
            assert instructions.get(instructions.size() - 1) instanceof BlockEndOp : "Not a BlockEndOp: " + instructions.get(instructions.size() - 1);
            assert !(instructions.get(instructions.size() - 2) instanceof BlockEndOp) : "Is a BlockEndOp: " + instructions.get(instructions.size() - 2);
            assert instructions.get(0) instanceof LabelOp : "Not a LabelOp: " + instructions.get(0);
            assert !(instructions.get(1) instanceof LabelOp) : "Is a LabelOp: " + instructions.get(1);
            String[] names = new String[instructionsToProfile.length];
            String[] groups = new String[instructionsToProfile.length];
            Value[] increments = new Value[instructionsToProfile.length];
            for (int i = 0; i < instructionsToProfile.length; i++) {
                names[i] = compilationUnitName;
                groups[i] = COUNTER_GROUP + " " + instructionsToProfile[i];
                // Default is zero; this value is patched to the real instruction count after
                // assembly in method HotSpotInstructionProfiling.countInstructions
                increments[i] = new ConstantValue(LIRKind.fromJavaKind(target.arch, JavaKind.Int), JavaConstant.INT_0);
            }
            HotSpotCounterOp op = (HotSpotCounterOp) diagnosticLirGenTool.createMultiBenchmarkCounter(names, groups, increments);
            LIRInstruction inst = new InstructionCounterOp(op, instructionsToProfile);
            assert inst != null;
            buffer.init(instructions);
            buffer.append(1, inst);
            buffer.finish();
        }
    }

    /**
     * After assembly the {@link HotSpotBackend#profileInstructions(LIR, CompilationResultBuilder)}
     * calls this method for patching the instruction counts into the counter increment code.
     */
    public static void countInstructions(LIR lir, Assembler asm) {
        InstructionCounterOp lastOp = null;
        InstructionCounter counter = asm.getInstructionCounter();
        for (AbstractBlockBase<?> block : lir.codeEmittingOrder()) {
            if (block == null) {
                continue;
            }
            for (LIRInstruction inst : lir.getLIRforBlock(block)) {
                if (inst instanceof InstructionCounterOp) {
                    InstructionCounterOp currentOp = (InstructionCounterOp) inst;

                    if (lastOp != null) {
                        int beginPc = lastOp.countOffsetEnd;
                        int endPc = currentOp.countOffsetBegin;
                        int[] instructionCounts = counter.countInstructions(lastOp.instructionsToProfile, beginPc, endPc);
                        lastOp.delegate.patchCounterIncrement(asm, instructionCounts);
                    }
                    lastOp = ((InstructionCounterOp) inst);
                }
            }
        }
        if (lastOp != null) {
            assert lastOp.countOffsetBegin < asm.position();
            int beginPc = lastOp.countOffsetBegin;
            int endPc = asm.position();
            int[] instructionCounts = counter.countInstructions(lastOp.instructionsToProfile, beginPc, endPc);
            lastOp.delegate.patchCounterIncrement(asm, instructionCounts);
        }
    }

    public static class InstructionCounterOp extends LIRInstruction {
        public static final LIRInstructionClass<InstructionCounterOp> TYPE = LIRInstructionClass.create(InstructionCounterOp.class);
        private final HotSpotCounterOp delegate;
        private final String[] instructionsToProfile;
        private int countOffsetBegin;
        private int countOffsetEnd;

        public InstructionCounterOp(HotSpotCounterOp delegate, String[] instructionsToProfile) {
            super(TYPE);
            this.delegate = delegate;
            this.instructionsToProfile = instructionsToProfile;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb) {
            countOffsetBegin = crb.asm.position();
            this.delegate.emitCode(crb);
            countOffsetEnd = crb.asm.position();
        }

        public String[] getInstructionsToProfile() {
            return instructionsToProfile;
        }
    }
}
