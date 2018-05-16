package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.core.common.GraalOptions.GeneratePIC;
import static jdk.vm.ci.code.ValueUtil.asRegister;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

public final class AMD64HotSpotLoadConfigValueOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotLoadConfigValueOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadConfigValueOp.class);

    @Def({OperandFlag.REG}) protected AllocatableValue result;
    private final int markId;

    public AMD64HotSpotLoadConfigValueOp(int markId, AllocatableValue result)
    {
        super(TYPE);
        this.result = result;
        this.markId = markId;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        if (GeneratePIC.getValue(crb.getOptions()))
        {
            AMD64Kind kind = (AMD64Kind) result.getPlatformKind();
            Register reg = asRegister(result);
            AMD64Address placeholder = masm.getPlaceholder(-1);
            switch (kind)
            {
                case BYTE:
                    masm.movsbl(reg, placeholder);
                    break;
                case WORD:
                    masm.movswl(reg, placeholder);
                    break;
                case DWORD:
                    masm.movl(reg, placeholder);
                    break;
                case QWORD:
                    masm.movq(reg, placeholder);
                    break;
                default:
                    throw GraalError.unimplemented();
            }
        }
        else
        {
            throw GraalError.unimplemented();
        }
        crb.recordMark(markId);
    }
}
