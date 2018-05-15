package graalvm.compiler.hotspot.amd64;

import static jdk.vm.ci.amd64.AMD64.rsp;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;

@Opcode
final class AMD64HotSpotCRuntimeCallPrologueOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallPrologueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallPrologueOp.class);

    private final int threadLastJavaSpOffset;
    private final Register thread;

    AMD64HotSpotCRuntimeCallPrologueOp(int threadLastJavaSpOffset, Register thread) {
        super(TYPE);
        this.threadLastJavaSpOffset = threadLastJavaSpOffset;
        this.thread = thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        // save last Java frame
        masm.movq(new AMD64Address(thread, threadLastJavaSpOffset), rsp);
    }
}
