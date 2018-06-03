package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64PrefetchOp
public final class AMD64PrefetchOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64PrefetchOp> TYPE = LIRInstructionClass.create(AMD64PrefetchOp.class);

    // @field
    private final int instr; // AllocatePrefetchInstr
    @Alive({OperandFlag.COMPOSITE})
    // @field
    protected AMD64AddressValue address;

    // @cons
    public AMD64PrefetchOp(AMD64AddressValue __address, int __instr)
    {
        super(TYPE);
        this.address = __address;
        this.instr = __instr;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        switch (instr)
        {
            case 0:
                __masm.prefetchnta(address.toAddress());
                break;
            case 1:
                __masm.prefetcht0(address.toAddress());
                break;
            case 2:
                __masm.prefetcht2(address.toAddress());
                break;
            case 3:
                __masm.prefetchw(address.toAddress());
                break;
            default:
                throw GraalError.shouldNotReachHere("unspported prefetch op " + instr);
        }
    }
}
