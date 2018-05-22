package graalvm.compiler.lir;

import jdk.vm.ci.code.site.InfopointReason;

import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * Emits an infopoint (only mark the position).
 */
@Opcode("INFOPOINT")
public final class FullInfopointOp extends LIRInstruction
{
    public static final LIRInstructionClass<FullInfopointOp> TYPE = LIRInstructionClass.create(FullInfopointOp.class);

    @State protected LIRFrameState state;

    private final InfopointReason reason;

    public FullInfopointOp(LIRFrameState state, InfopointReason reason)
    {
        super(TYPE);
        this.state = state;
        this.reason = reason;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb)
    {
        crb.recordInfopoint(crb.asm.position(), state, reason);
        crb.asm.ensureUniquePC();
    }
}
