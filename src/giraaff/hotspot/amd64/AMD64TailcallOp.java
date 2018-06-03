package giraaff.hotspot.amd64;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Performs a hard-coded tail call to the specified target, which normally should be an
 * {@link InstalledCode} instance.
 */
@Opcode
// @class AMD64TailcallOp
public final class AMD64TailcallOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64TailcallOp> TYPE = LIRInstructionClass.create(AMD64TailcallOp.class);

    @Use
    // @field
    protected Value target;
    @Alive
    // @field
    protected Value[] parameters;

    // @cons
    public AMD64TailcallOp(Value[] __parameters, Value __target)
    {
        super(TYPE);
        this.target = __target;
        this.parameters = __parameters;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // destroy the current frame (now the return address is the top of stack)
        __masm.leave();

        // jump to the target method
        __masm.jmp(ValueUtil.asRegister(target));
        __masm.ensureUniquePC();
    }
}
