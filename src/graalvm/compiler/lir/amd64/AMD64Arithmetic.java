package graalvm.compiler.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.asm.Label;
import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64Assembler.ConditionFlag;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

public enum AMD64Arithmetic
{
    FREM,
    DREM;

    public static class FPDivRemOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<FPDivRemOp> TYPE = LIRInstructionClass.create(FPDivRemOp.class);

        @Opcode private final AMD64Arithmetic opcode;
        @Def protected AllocatableValue result;
        @Use protected AllocatableValue x;
        @Use protected AllocatableValue y;
        @Temp protected AllocatableValue raxTemp;

        public FPDivRemOp(AMD64Arithmetic opcode, AllocatableValue result, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.result = result;
            this.raxTemp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.DWORD));
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Address tmp = new AMD64Address(AMD64.rsp);
            masm.subq(AMD64.rsp, 8);
            if (opcode == FREM)
            {
                masm.movflt(tmp, ValueUtil.asRegister(y));
                masm.flds(tmp);
                masm.movflt(tmp, ValueUtil.asRegister(x));
                masm.flds(tmp);
            }
            else
            {
                masm.movdbl(tmp, ValueUtil.asRegister(y));
                masm.fldd(tmp);
                masm.movdbl(tmp, ValueUtil.asRegister(x));
                masm.fldd(tmp);
            }

            Label label = new Label();
            masm.bind(label);
            masm.fprem();
            masm.fwait();
            masm.fnstswAX();
            masm.testl(AMD64.rax, 0x400);
            masm.jcc(ConditionFlag.NotZero, label);
            masm.fxch(1);
            masm.fpop();

            if (opcode == FREM)
            {
                masm.fstps(tmp);
                masm.movflt(ValueUtil.asRegister(result), tmp);
            }
            else
            {
                masm.fstpd(tmp);
                masm.movdbl(ValueUtil.asRegister(result), tmp);
            }
            masm.addq(AMD64.rsp, 8);
        }
    }
}
