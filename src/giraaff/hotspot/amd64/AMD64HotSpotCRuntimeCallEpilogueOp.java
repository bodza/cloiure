package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

@LIROpcode
// @class AMD64HotSpotCRuntimeCallEpilogueOp
final class AMD64HotSpotCRuntimeCallEpilogueOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallEpilogueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallEpilogueOp.class);

    // @field
    private final int ___threadLastJavaSpOffset;
    // @field
    private final int ___threadLastJavaFpOffset;
    // @field
    private final int ___threadLastJavaPcOffset;
    // @field
    private final Register ___thread;

    // @cons AMD64HotSpotCRuntimeCallEpilogueOp
    AMD64HotSpotCRuntimeCallEpilogueOp(int __threadLastJavaSpOffset, int __threadLastJavaFpOffset, int __threadLastJavaPcOffset, Register __thread)
    {
        super(TYPE);
        this.___threadLastJavaSpOffset = __threadLastJavaSpOffset;
        this.___threadLastJavaFpOffset = __threadLastJavaFpOffset;
        this.___threadLastJavaPcOffset = __threadLastJavaPcOffset;
        this.___thread = __thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // reset last Java frame
        __masm.movslq(new AMD64Address(this.___thread, this.___threadLastJavaSpOffset), 0);
        __masm.movslq(new AMD64Address(this.___thread, this.___threadLastJavaFpOffset), 0);
        __masm.movslq(new AMD64Address(this.___thread, this.___threadLastJavaPcOffset), 0);
    }
}
