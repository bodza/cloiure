package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64Assembler.SSEOp;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.LIRGeneratorTool;

/**
 * Emits code which compares two arrays of the same length. If the CPU supports any vector
 * instructions specialized code is emitted to leverage these instructions.
 */
@Opcode
// @class AMD64ArrayEqualsOp
public final class AMD64ArrayEqualsOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ArrayEqualsOp> TYPE = LIRInstructionClass.create(AMD64ArrayEqualsOp.class);

    // @field
    private final JavaKind kind;
    // @field
    private final int arrayBaseOffset;
    // @field
    private final int arrayIndexScale;

    @Def({OperandFlag.REG})
    // @field
    protected Value resultValue;
    @Alive({OperandFlag.REG})
    // @field
    protected Value array1Value;
    @Alive({OperandFlag.REG})
    // @field
    protected Value array2Value;
    @Alive({OperandFlag.REG})
    // @field
    protected Value lengthValue;
    @Temp({OperandFlag.REG})
    // @field
    protected Value temp1;
    @Temp({OperandFlag.REG})
    // @field
    protected Value temp2;
    @Temp({OperandFlag.REG})
    // @field
    protected Value temp3;
    @Temp({OperandFlag.REG})
    // @field
    protected Value temp4;

    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected Value temp5;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected Value tempXMM;

    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected Value vectorTemp1;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected Value vectorTemp2;

    // @cons
    public AMD64ArrayEqualsOp(LIRGeneratorTool __tool, JavaKind __kind, Value __result, Value __array1, Value __array2, Value __length)
    {
        super(TYPE);
        this.kind = __kind;

        this.arrayBaseOffset = __tool.getProviders().getArrayOffsetProvider().arrayBaseOffset(__kind);
        this.arrayIndexScale = __tool.getProviders().getArrayOffsetProvider().arrayScalingFactor(__kind);

        this.resultValue = __result;
        this.array1Value = __array1;
        this.array2Value = __array2;
        this.lengthValue = __length;

        // Allocate some temporaries.
        this.temp1 = __tool.newVariable(LIRKind.unknownReference(__tool.target().arch.getWordKind()));
        this.temp2 = __tool.newVariable(LIRKind.unknownReference(__tool.target().arch.getWordKind()));
        this.temp3 = __tool.newVariable(LIRKind.value(__tool.target().arch.getWordKind()));
        this.temp4 = __tool.newVariable(LIRKind.value(__tool.target().arch.getWordKind()));

        this.temp5 = __kind.isNumericFloat() ? __tool.newVariable(LIRKind.value(__tool.target().arch.getWordKind())) : Value.ILLEGAL;
        if (__kind == JavaKind.Float)
        {
            this.tempXMM = __tool.newVariable(LIRKind.value(AMD64Kind.SINGLE));
        }
        else if (__kind == JavaKind.Double)
        {
            this.tempXMM = __tool.newVariable(LIRKind.value(AMD64Kind.DOUBLE));
        }
        else
        {
            this.tempXMM = Value.ILLEGAL;
        }

        // We only need the vector temporaries if we generate SSE code.
        if (supportsSSE41(__tool.target()))
        {
            this.vectorTemp1 = __tool.newVariable(LIRKind.value(AMD64Kind.DOUBLE));
            this.vectorTemp2 = __tool.newVariable(LIRKind.value(AMD64Kind.DOUBLE));
        }
        else
        {
            this.vectorTemp1 = Value.ILLEGAL;
            this.vectorTemp2 = Value.ILLEGAL;
        }
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        Register __result = ValueUtil.asRegister(resultValue);
        Register __array1 = ValueUtil.asRegister(temp1);
        Register __array2 = ValueUtil.asRegister(temp2);
        Register __length = ValueUtil.asRegister(temp3);

        Label __trueLabel = new Label();
        Label __falseLabel = new Label();
        Label __done = new Label();

        // Load array base addresses.
        __masm.leaq(__array1, new AMD64Address(ValueUtil.asRegister(array1Value), arrayBaseOffset));
        __masm.leaq(__array2, new AMD64Address(ValueUtil.asRegister(array2Value), arrayBaseOffset));

        // Get array length in bytes.
        __masm.movl(__length, ValueUtil.asRegister(lengthValue));

        if (arrayIndexScale > 1)
        {
            __masm.shll(__length, NumUtil.log2Ceil(arrayIndexScale)); // scale length
        }

        __masm.movl(__result, __length); // copy

        if (supportsAVX2(__crb.target))
        {
            emitAVXCompare(__crb, __masm, __result, __array1, __array2, __length, __trueLabel, __falseLabel);
        }
        else if (supportsSSE41(__crb.target))
        {
            // this code is used for AVX as well because our backend correctly ensures that
            // VEX-prefixed instructions are emitted if AVX is supported
            emitSSE41Compare(__crb, __masm, __result, __array1, __array2, __length, __trueLabel, __falseLabel);
        }

        emit8ByteCompare(__crb, __masm, __result, __array1, __array2, __length, __trueLabel, __falseLabel);
        emitTailCompares(__masm, __result, __array1, __array2, __length, __trueLabel, __falseLabel);

        // return true
        __masm.bind(__trueLabel);
        __masm.movl(__result, 1);
        __masm.jmpb(__done);

        // return false
        __masm.bind(__falseLabel);
        __masm.xorl(__result, __result);

        // that's it
        __masm.bind(__done);
    }

    /**
     * Returns if the underlying AMD64 architecture supports SSE 4.1 instructions.
     *
     * @param target target description of the underlying architecture
     * @return true if the underlying architecture supports SSE 4.1
     */
    private static boolean supportsSSE41(TargetDescription __target)
    {
        AMD64 __arch = (AMD64) __target.arch;
        return __arch.getFeatures().contains(CPUFeature.SSE4_1);
    }

    /**
     * Vector size used in {@link #emitSSE41Compare}.
     */
    // @def
    private static final int SSE4_1_VECTOR_SIZE = 16;

    /**
     * Emits code that uses SSE4.1 128-bit (16-byte) vector compares.
     */
    private void emitSSE41Compare(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, Register __array1, Register __array2, Register __length, Label __trueLabel, Label __falseLabel)
    {
        Register __vector1 = ValueUtil.asRegister(vectorTemp1, AMD64Kind.DOUBLE);
        Register __vector2 = ValueUtil.asRegister(vectorTemp2, AMD64Kind.DOUBLE);

        Label __loop = new Label();
        Label __compareTail = new Label();

        boolean __requiresNaNCheck = kind.isNumericFloat();
        Label __loopCheck = new Label();
        Label __nanCheck = new Label();

        // compare 16-byte vectors
        __masm.andl(__result, SSE4_1_VECTOR_SIZE - 1); // tail count (in bytes)
        __masm.andl(__length, ~(SSE4_1_VECTOR_SIZE - 1)); // vector count (in bytes)
        __masm.jcc(ConditionFlag.Zero, __compareTail);

        __masm.leaq(__array1, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.leaq(__array2, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.negq(__length);

        // align the main loop
        __masm.align(__crb.target.wordSize * 2);
        __masm.bind(__loop);
        __masm.movdqu(__vector1, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.movdqu(__vector2, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.pxor(__vector1, __vector2);
        __masm.ptest(__vector1, __vector1);
        __masm.jcc(ConditionFlag.NotZero, __requiresNaNCheck ? __nanCheck : __falseLabel);

        __masm.bind(__loopCheck);
        __masm.addq(__length, SSE4_1_VECTOR_SIZE);
        __masm.jcc(ConditionFlag.NotZero, __loop);

        __masm.testl(__result, __result);
        __masm.jcc(ConditionFlag.Zero, __trueLabel);

        if (__requiresNaNCheck)
        {
            Label __unalignedCheck = new Label();
            __masm.jmpb(__unalignedCheck);
            __masm.bind(__nanCheck);
            emitFloatCompareWithinRange(__crb, __masm, __array1, __array2, __length, 0, __falseLabel, SSE4_1_VECTOR_SIZE);
            __masm.jmpb(__loopCheck);
            __masm.bind(__unalignedCheck);
        }

        // Compare the remaining bytes with an unaligned memory load aligned to the end of the array.
        __masm.movdqu(__vector1, new AMD64Address(__array1, __result, Scale.Times1, -SSE4_1_VECTOR_SIZE));
        __masm.movdqu(__vector2, new AMD64Address(__array2, __result, Scale.Times1, -SSE4_1_VECTOR_SIZE));
        __masm.pxor(__vector1, __vector2);
        __masm.ptest(__vector1, __vector1);
        if (__requiresNaNCheck)
        {
            __masm.jcc(ConditionFlag.Zero, __trueLabel);
            emitFloatCompareWithinRange(__crb, __masm, __array1, __array2, __result, -SSE4_1_VECTOR_SIZE, __falseLabel, SSE4_1_VECTOR_SIZE);
        }
        else
        {
            __masm.jcc(ConditionFlag.NotZero, __falseLabel);
        }
        __masm.jmp(__trueLabel);

        __masm.bind(__compareTail);
        __masm.movl(__length, __result);
    }

    /**
     * Returns if the underlying AMD64 architecture supports AVX instructions.
     *
     * @param target target description of the underlying architecture
     * @return true if the underlying architecture supports AVX
     */
    private static boolean supportsAVX2(TargetDescription __target)
    {
        AMD64 __arch = (AMD64) __target.arch;
        return __arch.getFeatures().contains(CPUFeature.AVX2);
    }

    /**
     * Vector size used in {@link #emitAVXCompare}.
     */
    // @def
    private static final int AVX_VECTOR_SIZE = 32;

    private void emitAVXCompare(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, Register __array1, Register __array2, Register __length, Label __trueLabel, Label __falseLabel)
    {
        Register __vector1 = ValueUtil.asRegister(vectorTemp1, AMD64Kind.DOUBLE);
        Register __vector2 = ValueUtil.asRegister(vectorTemp2, AMD64Kind.DOUBLE);

        Label __loop = new Label();
        Label __compareTail = new Label();

        boolean __requiresNaNCheck = kind.isNumericFloat();
        Label __loopCheck = new Label();
        Label __nanCheck = new Label();

        // compare 16-byte vectors
        __masm.andl(__result, AVX_VECTOR_SIZE - 1); // tail count (in bytes)
        __masm.andl(__length, ~(AVX_VECTOR_SIZE - 1)); // vector count (in bytes)
        __masm.jcc(ConditionFlag.Zero, __compareTail);

        __masm.leaq(__array1, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.leaq(__array2, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.negq(__length);

        // align the main loop
        __masm.align(__crb.target.wordSize * 2);
        __masm.bind(__loop);
        __masm.vmovdqu(__vector1, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.vmovdqu(__vector2, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.vpxor(__vector1, __vector1, __vector2);
        __masm.vptest(__vector1, __vector1);
        __masm.jcc(ConditionFlag.NotZero, __requiresNaNCheck ? __nanCheck : __falseLabel);

        __masm.bind(__loopCheck);
        __masm.addq(__length, AVX_VECTOR_SIZE);
        __masm.jcc(ConditionFlag.NotZero, __loop);

        __masm.testl(__result, __result);
        __masm.jcc(ConditionFlag.Zero, __trueLabel);

        if (__requiresNaNCheck)
        {
            Label __unalignedCheck = new Label();
            __masm.jmpb(__unalignedCheck);
            __masm.bind(__nanCheck);
            emitFloatCompareWithinRange(__crb, __masm, __array1, __array2, __length, 0, __falseLabel, AVX_VECTOR_SIZE);
            __masm.jmpb(__loopCheck);
            __masm.bind(__unalignedCheck);
        }

        // Compare the remaining bytes with an unaligned memory load aligned to the end of the array.
        __masm.vmovdqu(__vector1, new AMD64Address(__array1, __result, Scale.Times1, -AVX_VECTOR_SIZE));
        __masm.vmovdqu(__vector2, new AMD64Address(__array2, __result, Scale.Times1, -AVX_VECTOR_SIZE));
        __masm.vpxor(__vector1, __vector1, __vector2);
        __masm.vptest(__vector1, __vector1);
        if (__requiresNaNCheck)
        {
            __masm.jcc(ConditionFlag.Zero, __trueLabel);
            emitFloatCompareWithinRange(__crb, __masm, __array1, __array2, __result, -AVX_VECTOR_SIZE, __falseLabel, AVX_VECTOR_SIZE);
        }
        else
        {
            __masm.jcc(ConditionFlag.NotZero, __falseLabel);
        }
        __masm.jmp(__trueLabel);

        __masm.bind(__compareTail);
        __masm.movl(__length, __result);
    }

    /**
     * Vector size used in {@link #emit8ByteCompare}.
     */
    // @def
    private static final int VECTOR_SIZE = 8;

    /**
     * Emits code that uses 8-byte vector compares.
     */
    private void emit8ByteCompare(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, Register __array1, Register __array2, Register __length, Label __trueLabel, Label __falseLabel)
    {
        Label __loop = new Label();
        Label __compareTail = new Label();

        boolean __requiresNaNCheck = kind.isNumericFloat();
        Label __loopCheck = new Label();
        Label __nanCheck = new Label();

        Register __temp = ValueUtil.asRegister(temp4);

        __masm.andl(__result, VECTOR_SIZE - 1); // tail count (in bytes)
        __masm.andl(__length, ~(VECTOR_SIZE - 1)); // vector count (in bytes)
        __masm.jcc(ConditionFlag.Zero, __compareTail);

        __masm.leaq(__array1, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.leaq(__array2, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.negq(__length);

        // align the main loop
        __masm.align(__crb.target.wordSize * 2);
        __masm.bind(__loop);
        __masm.movq(__temp, new AMD64Address(__array1, __length, Scale.Times1, 0));
        __masm.cmpq(__temp, new AMD64Address(__array2, __length, Scale.Times1, 0));
        __masm.jcc(ConditionFlag.NotEqual, __requiresNaNCheck ? __nanCheck : __falseLabel);

        __masm.bind(__loopCheck);
        __masm.addq(__length, VECTOR_SIZE);
        __masm.jccb(ConditionFlag.NotZero, __loop);

        __masm.testl(__result, __result);
        __masm.jcc(ConditionFlag.Zero, __trueLabel);

        if (__requiresNaNCheck)
        {
            // NaN check is slow path and hence placed outside of the main loop.
            Label __unalignedCheck = new Label();
            __masm.jmpb(__unalignedCheck);
            __masm.bind(__nanCheck);
            // At most two iterations, unroll in the emitted code.
            for (int __offset = 0; __offset < VECTOR_SIZE; __offset += kind.getByteCount())
            {
                emitFloatCompare(__masm, __array1, __array2, __length, __offset, __falseLabel, kind.getByteCount() == VECTOR_SIZE);
            }
            __masm.jmpb(__loopCheck);
            __masm.bind(__unalignedCheck);
        }

        // Compare the remaining bytes with an unaligned memory load aligned to the end of the array.
        __masm.movq(__temp, new AMD64Address(__array1, __result, Scale.Times1, -VECTOR_SIZE));
        __masm.cmpq(__temp, new AMD64Address(__array2, __result, Scale.Times1, -VECTOR_SIZE));
        if (__requiresNaNCheck)
        {
            __masm.jcc(ConditionFlag.Equal, __trueLabel);
            // At most two iterations, unroll in the emitted code.
            for (int __offset = 0; __offset < VECTOR_SIZE; __offset += kind.getByteCount())
            {
                emitFloatCompare(__masm, __array1, __array2, __result, -VECTOR_SIZE + __offset, __falseLabel, kind.getByteCount() == VECTOR_SIZE);
            }
        }
        else
        {
            __masm.jccb(ConditionFlag.NotEqual, __falseLabel);
        }
        __masm.jmpb(__trueLabel);

        __masm.bind(__compareTail);
        __masm.movl(__length, __result);
    }

    /**
     * Emits code to compare the remaining 1 to 4 bytes.
     */
    private void emitTailCompares(AMD64MacroAssembler __masm, Register __result, Register __array1, Register __array2, Register __length, Label __trueLabel, Label __falseLabel)
    {
        Label __compare2Bytes = new Label();
        Label __compare1Byte = new Label();

        Register __temp = ValueUtil.asRegister(temp4);

        if (kind.getByteCount() <= 4)
        {
            // Compare trailing 4 bytes, if any.
            __masm.testl(__result, 4);
            __masm.jccb(ConditionFlag.Zero, __compare2Bytes);
            __masm.movl(__temp, new AMD64Address(__array1, 0));
            __masm.cmpl(__temp, new AMD64Address(__array2, 0));
            if (kind == JavaKind.Float)
            {
                __masm.jccb(ConditionFlag.Equal, __trueLabel);
                emitFloatCompare(__masm, __array1, __array2, Register.None, 0, __falseLabel, true);
                __masm.jmpb(__trueLabel);
            }
            else
            {
                __masm.jccb(ConditionFlag.NotEqual, __falseLabel);
            }
            if (kind.getByteCount() <= 2)
            {
                // Move array pointers forward.
                __masm.leaq(__array1, new AMD64Address(__array1, 4));
                __masm.leaq(__array2, new AMD64Address(__array2, 4));

                // Compare trailing 2 bytes, if any.
                __masm.bind(__compare2Bytes);
                __masm.testl(__result, 2);
                __masm.jccb(ConditionFlag.Zero, __compare1Byte);
                __masm.movzwl(__temp, new AMD64Address(__array1, 0));
                __masm.movzwl(__length, new AMD64Address(__array2, 0));
                __masm.cmpl(__temp, __length);
                __masm.jccb(ConditionFlag.NotEqual, __falseLabel);

                // The one-byte tail compare is only required for boolean and byte arrays.
                if (kind.getByteCount() <= 1)
                {
                    // Move array pointers forward before we compare the last trailing byte.
                    __masm.leaq(__array1, new AMD64Address(__array1, 2));
                    __masm.leaq(__array2, new AMD64Address(__array2, 2));

                    // Compare trailing byte, if any.
                    __masm.bind(__compare1Byte);
                    __masm.testl(__result, 1);
                    __masm.jccb(ConditionFlag.Zero, __trueLabel);
                    __masm.movzbl(__temp, new AMD64Address(__array1, 0));
                    __masm.movzbl(__length, new AMD64Address(__array2, 0));
                    __masm.cmpl(__temp, __length);
                    __masm.jccb(ConditionFlag.NotEqual, __falseLabel);
                }
                else
                {
                    __masm.bind(__compare1Byte);
                }
            }
            else
            {
                __masm.bind(__compare2Bytes);
            }
        }
    }

    /**
     * Emits code to fall through if {@code src} is NaN, otherwise jump to {@code branchOrdered}.
     */
    private void emitNaNCheck(AMD64MacroAssembler __masm, AMD64Address __src, Label __branchIfNonNaN)
    {
        Register __tempXMMReg = ValueUtil.asRegister(tempXMM);
        if (kind == JavaKind.Float)
        {
            __masm.movflt(__tempXMMReg, __src);
        }
        else
        {
            __masm.movdbl(__tempXMMReg, __src);
        }
        SSEOp.UCOMIS.emit(__masm, kind == JavaKind.Float ? OperandSize.PS : OperandSize.PD, __tempXMMReg, __tempXMMReg);
        __masm.jcc(ConditionFlag.NoParity, __branchIfNonNaN);
    }

    /**
     * Emits code to compare if two floats are bitwise equal or both NaN.
     */
    private void emitFloatCompare(AMD64MacroAssembler __masm, Register __base1, Register __base2, Register __index, int __offset, Label __falseLabel, boolean __skipBitwiseCompare)
    {
        AMD64Address __address1 = new AMD64Address(__base1, __index, Scale.Times1, __offset);
        AMD64Address __address2 = new AMD64Address(__base2, __index, Scale.Times1, __offset);

        Label __bitwiseEqual = new Label();

        if (!__skipBitwiseCompare)
        {
            // bitwise compare
            Register __temp = ValueUtil.asRegister(temp4);

            if (kind == JavaKind.Float)
            {
                __masm.movl(__temp, __address1);
                __masm.cmpl(__temp, __address2);
            }
            else
            {
                __masm.movq(__temp, __address1);
                __masm.cmpq(__temp, __address2);
            }
            __masm.jccb(ConditionFlag.Equal, __bitwiseEqual);
        }

        emitNaNCheck(__masm, __address1, __falseLabel);
        emitNaNCheck(__masm, __address2, __falseLabel);

        __masm.bind(__bitwiseEqual);
    }

    /**
     * Emits code to compare float equality within a range.
     */
    private void emitFloatCompareWithinRange(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __base1, Register __base2, Register __index, int __offset, Label __falseLabel, int __range)
    {
        Label __loop = new Label();
        Register __i = ValueUtil.asRegister(temp5);

        __masm.movq(__i, __range);
        __masm.negq(__i);
        // align the main loop
        __masm.align(__crb.target.wordSize * 2);
        __masm.bind(__loop);
        emitFloatCompare(__masm, __base1, __base2, __index, __offset, __falseLabel, kind.getByteCount() == __range);
        __masm.addq(__index, kind.getByteCount());
        __masm.addq(__i, kind.getByteCount());
        __masm.jccb(ConditionFlag.NotZero, __loop);
        // floats within the range are equal: revert change to the register index
        __masm.subq(__index, __range);
    }
}
