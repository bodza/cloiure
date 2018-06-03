package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

// @enum AMD64Arithmetic
public enum AMD64Arithmetic
{
    FREM,
    DREM;

    // @class AMD64Arithmetic.FPDivRemOp
    public static final class FPDivRemOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<FPDivRemOp> TYPE = LIRInstructionClass.create(FPDivRemOp.class);

        @Opcode
        // @field
        private final AMD64Arithmetic ___opcode;
        @Def
        // @field
        protected AllocatableValue ___result;
        @Use
        // @field
        protected AllocatableValue ___x;
        @Use
        // @field
        protected AllocatableValue ___y;
        @Temp
        // @field
        protected AllocatableValue ___raxTemp;

        // @cons
        public FPDivRemOp(AMD64Arithmetic __opcode, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___result = __result;
            this.___raxTemp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.DWORD));
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Address __tmp = new AMD64Address(AMD64.rsp);
            __masm.subq(AMD64.rsp, 8);
            if (this.___opcode == FREM)
            {
                __masm.movflt(__tmp, ValueUtil.asRegister(this.___y));
                __masm.flds(__tmp);
                __masm.movflt(__tmp, ValueUtil.asRegister(this.___x));
                __masm.flds(__tmp);
            }
            else
            {
                __masm.movdbl(__tmp, ValueUtil.asRegister(this.___y));
                __masm.fldd(__tmp);
                __masm.movdbl(__tmp, ValueUtil.asRegister(this.___x));
                __masm.fldd(__tmp);
            }

            Label __label = new Label();
            __masm.bind(__label);
            __masm.fprem();
            __masm.fwait();
            __masm.fnstswAX();
            __masm.testl(AMD64.rax, 0x400);
            __masm.jcc(ConditionFlag.NotZero, __label);
            __masm.fxch(1);
            __masm.fpop();

            if (this.___opcode == FREM)
            {
                __masm.fstps(__tmp);
                __masm.movflt(ValueUtil.asRegister(this.___result), __tmp);
            }
            else
            {
                __masm.fstpd(__tmp);
                __masm.movdbl(ValueUtil.asRegister(this.___result), __tmp);
            }
            __masm.addq(AMD64.rsp, 8);
        }
    }
}
