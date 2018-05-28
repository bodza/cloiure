package giraaff.lir.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.LIRGeneratorTool;

@Opcode
public final class AMD64StringIndexOfOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64StringIndexOfOp> TYPE = LIRInstructionClass.create(AMD64StringIndexOfOp.class);

    @Def({OperandFlag.REG}) protected Value resultValue;
    @Alive({OperandFlag.REG}) protected Value charPtr1Value;
    @Alive({OperandFlag.REG}) protected Value charPtr2Value;
    @Use({OperandFlag.REG}) protected RegisterValue cnt1Value;
    @Temp({OperandFlag.REG}) protected RegisterValue cnt1ValueT;
    @Use({OperandFlag.REG}) protected RegisterValue cnt2Value;
    @Temp({OperandFlag.REG}) protected RegisterValue cnt2ValueT;
    @Temp({OperandFlag.REG}) protected Value temp1;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL}) protected Value vectorTemp1;

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
        Register charPtr1 = ValueUtil.asRegister(charPtr1Value);
        Register charPtr2 = ValueUtil.asRegister(charPtr2Value);
        Register cnt1 = ValueUtil.asRegister(cnt1Value);
        Register cnt2 = ValueUtil.asRegister(cnt2Value);
        Register result = ValueUtil.asRegister(resultValue);
        Register vec = ValueUtil.asRegister(vectorTemp1);
        Register tmp = ValueUtil.asRegister(temp1);
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
