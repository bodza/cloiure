package giraaff.hotspot.amd64;

import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64HotSpotLoadConfigValueOp
public final class AMD64HotSpotLoadConfigValueOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotLoadConfigValueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadConfigValueOp.class);

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___result;
    // @field
    private final int ___markId;

    // @cons AMD64HotSpotLoadConfigValueOp
    public AMD64HotSpotLoadConfigValueOp(int __markId, AllocatableValue __result)
    {
        super(TYPE);
        this.___result = __result;
        this.___markId = __markId;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        throw GraalError.unimplemented();
    }
}
