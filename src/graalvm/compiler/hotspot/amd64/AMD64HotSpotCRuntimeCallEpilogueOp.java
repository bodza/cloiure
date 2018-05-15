package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;

@Opcode("CRUNTIME_CALL_EPILOGUE")
final class AMD64HotSpotCRuntimeCallEpilogueOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallEpilogueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallEpilogueOp.class);

    private final int threadLastJavaSpOffset;
    private final int threadLastJavaFpOffset;
    private final int threadLastJavaPcOffset;
    private final Register thread;

    AMD64HotSpotCRuntimeCallEpilogueOp(int threadLastJavaSpOffset, int threadLastJavaFpOffset, int threadLastJavaPcOffset, Register thread) {
        super(TYPE);
        this.threadLastJavaSpOffset = threadLastJavaSpOffset;
        this.threadLastJavaFpOffset = threadLastJavaFpOffset;
        this.threadLastJavaPcOffset = threadLastJavaPcOffset;
        this.thread = thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // reset last Java frame:
        masm.movslq(new AMD64Address(thread, threadLastJavaSpOffset), 0);
        masm.movslq(new AMD64Address(thread, threadLastJavaFpOffset), 0);
        masm.movslq(new AMD64Address(thread, threadLastJavaPcOffset), 0);
    }
}
