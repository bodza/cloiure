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
    // @def
    public static final LIRInstructionClass<AMD64HotSpotCRuntimeCallPrologueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotCRuntimeCallPrologueOp.class);

    // @field
    private final int ___threadLastJavaSpOffset;
    // @field
    private final Register ___thread;

    // @cons
    AMD64HotSpotCRuntimeCallPrologueOp(int __threadLastJavaSpOffset, Register __thread)
    {
        super(TYPE);
        this.___threadLastJavaSpOffset = __threadLastJavaSpOffset;
        this.___thread = __thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // save last Java frame
        __masm.movq(new AMD64Address(this.___thread, this.___threadLastJavaSpOffset), AMD64.rsp);
    }
}
