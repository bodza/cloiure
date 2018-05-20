package graalvm.compiler.lir.amd64;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.ILLEGAL;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.gen.LIRGeneratorTool;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.Value;

/**
  */
@Opcode("AMD64_STRING_INDEX_OF")
public final class AMD64StringIndexOfOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64StringIndexOfOp> TYPE = LIRInstructionClass.create(AMD64StringIndexOfOp.class);

    @Def({REG}) protected Value resultValue;
    @Alive({REG}) protected Value charPtr1Value;
    @Alive({REG}) protected Value charPtr2Value;
    @Use({REG}) protected RegisterValue cnt1Value;
    @Temp({REG}) protected RegisterValue cnt1ValueT;
    @Use({REG}) protected RegisterValue cnt2Value;
    @Temp({REG}) protected RegisterValue cnt2ValueT;
    @Temp({REG}) protected Value temp1;
    @Temp({REG, ILLEGAL}) protected Value vectorTemp1;

    private final int intCnt2;

    private final int vmPageSize;

    public AMD64StringIndexOfOp(LIRGeneratorTool tool, Value result, Value charPtr1, Value charPtr2, RegisterValue cnt1, RegisterValue cnt2, RegisterValue temp1, RegisterValue vectorTemp1, int intCnt2, int vmPageSize)
    {
        super(TYPE);
        resultValue = result;
        charPtr1Value = charPtr1;
        charPtr2Value = charPtr2;
        /*
         * The count values are inputs but are also killed like temporaries so need both Use and
         * Temp annotations, which will only work with fixed registers.
         */
        cnt1Value = cnt1;
        cnt1ValueT = cnt1;
        cnt2Value = cnt2;
        cnt2ValueT = cnt2;

        this.temp1 = temp1;
        this.vectorTemp1 = vectorTemp1;
        this.intCnt2 = intCnt2;
        this.vmPageSize = vmPageSize;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        Register charPtr1 = asRegister(charPtr1Value);
        Register charPtr2 = asRegister(charPtr2Value);
        Register cnt1 = asRegister(cnt1Value);
        Register cnt2 = asRegister(cnt2Value);
        Register result = asRegister(resultValue);
        Register vec = asRegister(vectorTemp1);
        Register tmp = asRegister(temp1);
        if (intCnt2 >= 8)
        {
            masm.stringIndexofC8(charPtr1, charPtr2, cnt1, cnt2, intCnt2, result, vec, tmp);
        }
        else
        {
            masm.stringIndexOf(charPtr1, charPtr2, cnt1, cnt2, intCnt2, result, vec, tmp, vmPageSize);
        }
    }
}
