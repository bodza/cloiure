package giraaff.hotspot.amd64;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Performs a hard-coded tail call to the specified target, which normally should be an
// {@link InstalledCode} instance.
///
@LIROpcode
// @class AMD64TailcallOp
public final class AMD64TailcallOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64TailcallOp> TYPE = LIRInstructionClass.create(AMD64TailcallOp.class);

    @LIRInstruction.Use
    // @field
    protected Value ___target;
    @LIRInstruction.Alive
    // @field
    protected Value[] ___parameters;

    // @cons AMD64TailcallOp
    public AMD64TailcallOp(Value[] __parameters, Value __target)
    {
        super(TYPE);
        this.___target = __target;
        this.___parameters = __parameters;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // destroy the current frame (now the return address is the top of stack)
        __masm.leave();

        // jump to the target method
        __masm.jmp(ValueUtil.asRegister(this.___target));
        __masm.ensureUniquePC();
    }
}
