package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode
// @class AMD64HotSpotCRuntimeCallPrologueOp
final class AMD64HotSpotCRuntimeCallPrologueOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallPrologueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallPrologueOp.class);

    private final int threadLastJavaSpOffset;
    private final Register thread;

    // @cons
    AMD64HotSpotCRuntimeCallPrologueOp(int threadLastJavaSpOffset, Register thread)
    {
        super(TYPE);
        this.threadLastJavaSpOffset = threadLastJavaSpOffset;
        this.thread = thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // save last Java frame
        masm.movq(new AMD64Address(thread, threadLastJavaSpOffset), AMD64.rsp);
    }
}
