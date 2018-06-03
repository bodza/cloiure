package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode
// @class AMD64HotSpotCRuntimeCallEpilogueOp
final class AMD64HotSpotCRuntimeCallEpilogueOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallEpilogueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallEpilogueOp.class);

    // @field
    private final int threadLastJavaSpOffset;
    // @field
    private final int threadLastJavaFpOffset;
    // @field
    private final int threadLastJavaPcOffset;
    // @field
    private final Register thread;

    // @cons
    AMD64HotSpotCRuntimeCallEpilogueOp(int __threadLastJavaSpOffset, int __threadLastJavaFpOffset, int __threadLastJavaPcOffset, Register __thread)
    {
        super(TYPE);
        this.threadLastJavaSpOffset = __threadLastJavaSpOffset;
        this.threadLastJavaFpOffset = __threadLastJavaFpOffset;
        this.threadLastJavaPcOffset = __threadLastJavaPcOffset;
        this.thread = __thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // reset last Java frame
        __masm.movslq(new AMD64Address(thread, threadLastJavaSpOffset), 0);
        __masm.movslq(new AMD64Address(thread, threadLastJavaFpOffset), 0);
        __masm.movslq(new AMD64Address(thread, threadLastJavaPcOffset), 0);
    }
}
