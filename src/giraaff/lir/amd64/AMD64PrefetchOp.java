package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64PrefetchOp
public final class AMD64PrefetchOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64PrefetchOp> TYPE = LIRInstructionClass.create(AMD64PrefetchOp.class);

    // @field
    private final int ___instr; // AllocatePrefetchInstr
    @LIRInstruction.Alive({LIRInstruction.OperandFlag.COMPOSITE})
    // @field
    protected AMD64AddressValue ___address;

    // @cons AMD64PrefetchOp
    public AMD64PrefetchOp(AMD64AddressValue __address, int __instr)
    {
        super(TYPE);
        this.___address = __address;
        this.___instr = __instr;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        switch (this.___instr)
        {
            case 0:
            {
                __masm.prefetchnta(this.___address.toAddress());
                break;
            }
            case 1:
            {
                __masm.prefetcht0(this.___address.toAddress());
                break;
            }
            case 2:
            {
                __masm.prefetcht2(this.___address.toAddress());
                break;
            }
            case 3:
            {
                __masm.prefetchw(this.___address.toAddress());
                break;
            }
            default:
                throw GraalError.shouldNotReachHere("unspported prefetch op " + this.___instr);
        }
    }
}
