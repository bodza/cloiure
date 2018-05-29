package giraaff.hotspot.amd64;

import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64HotSpotLoadConfigValueOp
public final class AMD64HotSpotLoadConfigValueOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotLoadConfigValueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadConfigValueOp.class);

    @Def({OperandFlag.REG}) protected AllocatableValue result;
    private final int markId;

    // @cons
    public AMD64HotSpotLoadConfigValueOp(int markId, AllocatableValue result)
    {
        super(TYPE);
        this.result = result;
        this.markId = markId;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        throw GraalError.unimplemented();
    }
}
