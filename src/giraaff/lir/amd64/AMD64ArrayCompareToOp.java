package giraaff.lir.amd64;

import java.lang.reflect.Array;
import java.util.EnumSet;

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
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.util.UnsafeAccess;

///
// Emits code which compares two arrays lexicographically. If the CPU supports any vector
// instructions specialized code is emitted to leverage these instructions.
///
@LIROpcode
// @class AMD64ArrayCompareToOp
public final class AMD64ArrayCompareToOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ArrayCompareToOp> TYPE = LIRInstructionClass.create(AMD64ArrayCompareToOp.class);

    // @field
    private final JavaKind ___kind1;
    // @field
    private final JavaKind ___kind2;
    // @field
    private final int ___array1BaseOffset;
    // @field
    private final int ___array2BaseOffset;

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___resultValue;
    @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___array1Value;
    @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___array2Value;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___length1Value;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___length2Value;
    @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___length1ValueTemp;
    @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___length2ValueTemp;
    @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___temp1;
    @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___temp2;

    @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
    // @field
    protected Value ___vectorTemp1;

    // @cons AMD64ArrayCompareToOp
    public AMD64ArrayCompareToOp(LIRGeneratorTool __tool, JavaKind __kind1, JavaKind __kind2, Value __result, Value __array1, Value __array2, Value __length1, Value __length2)
    {
        super(TYPE);
        this.___kind1 = __kind1;
        this.___kind2 = __kind2;

        // Both offsets should be the same but better be safe than sorry.
        Class<?> __array1Class = Array.newInstance(__kind1.toJavaClass(), 0).getClass();
        Class<?> __array2Class = Array.newInstance(__kind2.toJavaClass(), 0).getClass();
        this.___array1BaseOffset = UnsafeAccess.UNSAFE.arrayBaseOffset(__array1Class);
        this.___array2BaseOffset = UnsafeAccess.UNSAFE.arrayBaseOffset(__array2Class);

        this.___resultValue = __result;
        this.___array1Value = __array1;
        this.___array2Value = __array2;
        // The length values are inputs but are also killed like temporaries so need both LIRInstruction.Use
        // and LIRInstruction.Temp annotations, which will only work with fixed registers.
        this.___length1Value = __length1;
        this.___length2Value = __length2;
        this.___length1ValueTemp = __length1;
        this.___length2ValueTemp = __length2;

        // Allocate some temporaries.
        this.___temp1 = __tool.newVariable(LIRKind.unknownReference(__tool.target().arch.getWordKind()));
        this.___temp2 = __tool.newVariable(LIRKind.unknownReference(__tool.target().arch.getWordKind()));

        // We only need the vector temporaries if we generate SSE code.
        if (supportsSSE42(__tool.target()))
        {
            this.___vectorTemp1 = __tool.newVariable(LIRKind.value(AMD64Kind.DOUBLE));
        }
        else
        {
            this.___vectorTemp1 = Value.ILLEGAL;
        }
    }

    private static boolean supportsSSE42(TargetDescription __target)
    {
        AMD64 __arch = (AMD64) __target.arch;
        return __arch.getFeatures().contains(CPUFeature.SSE4_2);
    }

    private static boolean supportsAVX2(TargetDescription __target)
    {
        AMD64 __arch = (AMD64) __target.arch;
        return __arch.getFeatures().contains(CPUFeature.AVX2);
    }

    private static boolean supportsAVX512VLBW(TargetDescription __target)
    {
        AMD64 __arch = (AMD64) __target.arch;
        EnumSet<CPUFeature> __features = __arch.getFeatures();
        return __features.contains(CPUFeature.AVX512BW) && __features.contains(CPUFeature.AVX512VL);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        Register __result = ValueUtil.asRegister(this.___resultValue);
        Register __str1 = ValueUtil.asRegister(this.___temp1);
        Register __str2 = ValueUtil.asRegister(this.___temp2);

        // Load array base addresses.
        __masm.leaq(__str1, new AMD64Address(ValueUtil.asRegister(this.___array1Value), this.___array1BaseOffset));
        __masm.leaq(__str2, new AMD64Address(ValueUtil.asRegister(this.___array2Value), this.___array2BaseOffset));
        Register __cnt1 = ValueUtil.asRegister(this.___length1Value);
        Register __cnt2 = ValueUtil.asRegister(this.___length2Value);

        Label __LENGTH_DIFF_LABEL = new Label();
        Label __POP_LABEL = new Label();
        Label __DONE_LABEL = new Label();
        Label __WHILE_HEAD_LABEL = new Label();
        Label __COMPARE_WIDE_VECTORS_LOOP_FAILED = new Label(); // used only _LP64 && AVX3
        int stride, stride2;
        int __adr_stride = -1;
        int __adr_stride1 = -1;
        int __adr_stride2 = -1;
        int __stride2x2 = 0x40;
        AMD64Address.Scale __scale = null;
        AMD64Address.Scale __scale1 = null;
        AMD64Address.Scale __scale2 = null;

        if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
        {
            __stride2x2 = 0x20;
        }

        if (this.___kind1 != this.___kind2)
        {
            __masm.shrl(__cnt2, 1);
        }
        // Compute the minimum of the string lengths and the difference of the string lengths (stack),
        // then do the conditional move stuff.
        __masm.movl(__result, __cnt1);
        __masm.subl(__cnt1, __cnt2);
        __masm.push(__cnt1);
        __masm.cmovl(AMD64Assembler.ConditionFlag.LessEqual, __cnt2, __result); // cnt2 = min(cnt1, cnt2)

        // Is the minimum length zero?
        __masm.testl(__cnt2, __cnt2);
        __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __LENGTH_DIFF_LABEL);
        if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
        {
            // load first bytes
            __masm.movzbl(__result, new AMD64Address(__str1, 0)); // result = str1[0]
            __masm.movzbl(__cnt1, new AMD64Address(__str2, 0)); // cnt1 = str2[0]
        }
        else if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Char)
        {
            // load first characters
            __masm.movzwl(__result, new AMD64Address(__str1, 0));
            __masm.movzwl(__cnt1, new AMD64Address(__str2, 0));
        }
        else
        {
            __masm.movzbl(__result, new AMD64Address(__str1, 0));
            __masm.movzwl(__cnt1, new AMD64Address(__str2, 0));
        }
        __masm.subl(__result, __cnt1);
        __masm.jcc(AMD64Assembler.ConditionFlag.NotZero, __POP_LABEL);

        if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Char)
        {
            // divide length by 2 to get number of chars
            __masm.shrl(__cnt2, 1);
        }
        __masm.cmpl(__cnt2, 1);
        __masm.jcc(AMD64Assembler.ConditionFlag.Equal, __LENGTH_DIFF_LABEL);

        // check if the strings start at the same location and setup scale and stride
        if (this.___kind1 == this.___kind2)
        {
            __masm.cmpptr(__str1, __str2);
            __masm.jcc(AMD64Assembler.ConditionFlag.Equal, __LENGTH_DIFF_LABEL);
            if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
            {
                __scale = AMD64Address.Scale.Times1;
                stride = 16;
            }
            else
            {
                __scale = AMD64Address.Scale.Times2;
                stride = 8;
            }
        }
        else
        {
            __scale1 = AMD64Address.Scale.Times1;
            __scale2 = AMD64Address.Scale.Times2;
            // scale not used
            stride = 8;
        }

        if (supportsAVX2(__crb.___target) && supportsSSE42(__crb.___target))
        {
            Register __vec1 = ValueUtil.asRegister(this.___vectorTemp1, AMD64Kind.DOUBLE);

            Label __COMPARE_WIDE_VECTORS = new Label();
            Label __VECTOR_NOT_EQUAL = new Label();
            Label __COMPARE_WIDE_TAIL = new Label();
            Label __COMPARE_SMALL_STR = new Label();
            Label __COMPARE_WIDE_VECTORS_LOOP = new Label();
            Label __COMPARE_16_CHARS = new Label();
            Label __COMPARE_INDEX_CHAR = new Label();
            Label __COMPARE_WIDE_VECTORS_LOOP_AVX2 = new Label();
            Label __COMPARE_TAIL_LONG = new Label();
            Label __COMPARE_WIDE_VECTORS_LOOP_AVX3 = new Label(); // used only _LP64 && AVX3

            int __pcmpmask = 0x19;
            if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
            {
                __pcmpmask &= ~0x01;
            }

            // Setup to compare 16-chars (32-bytes) vectors, start from first character again because it has aligned address.
            if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
            {
                stride2 = 32;
            }
            else
            {
                stride2 = 16;
            }
            if (this.___kind1 == this.___kind2)
            {
                __adr_stride = stride << __scale.___log2;
            }
            else
            {
                __adr_stride1 = 8; // stride << scale1;
                __adr_stride2 = 16; // stride << scale2;
            }

            // rax and rdx are used by pcmpestri as elements counters
            __masm.movl(__result, __cnt2);
            __masm.andl(__cnt2, ~(stride2 - 1)); // cnt2 holds the vector count
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __COMPARE_TAIL_LONG);

            // fast path : compare first 2 8-char vectors.
            __masm.bind(__COMPARE_16_CHARS);
            if (this.___kind1 == this.___kind2)
            {
                __masm.movdqu(__vec1, new AMD64Address(__str1, 0));
            }
            else
            {
                __masm.pmovzxbw(__vec1, new AMD64Address(__str1, 0));
            }
            __masm.pcmpestri(__vec1, new AMD64Address(__str2, 0), __pcmpmask);
            __masm.jccb(AMD64Assembler.ConditionFlag.Below, __COMPARE_INDEX_CHAR);

            if (this.___kind1 == this.___kind2)
            {
                __masm.movdqu(__vec1, new AMD64Address(__str1, __adr_stride));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __adr_stride), __pcmpmask);
            }
            else
            {
                __masm.pmovzxbw(__vec1, new AMD64Address(__str1, __adr_stride1));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __adr_stride2), __pcmpmask);
            }
            __masm.jccb(AMD64Assembler.ConditionFlag.AboveEqual, __COMPARE_WIDE_VECTORS);
            __masm.addl(__cnt1, stride);

            // compare the characters at index in cnt1
            __masm.bind(__COMPARE_INDEX_CHAR); // cnt1 has the offset of the mismatching character
            loadNextElements(__masm, __result, __cnt2, __str1, __str2, __scale, __scale1, __scale2, __cnt1);
            __masm.subl(__result, __cnt2);
            __masm.jmp(__POP_LABEL);

            // setup the registers to start vector comparison loop
            __masm.bind(__COMPARE_WIDE_VECTORS);
            if (this.___kind1 == this.___kind2)
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale));
            }
            else
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale1));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale2));
            }
            __masm.subl(__result, stride2);
            __masm.subl(__cnt2, stride2);
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __COMPARE_WIDE_TAIL);
            __masm.negq(__result);

            // in a loop, compare 16-chars (32-bytes) at once using (vpxor+vptest)
            __masm.bind(__COMPARE_WIDE_VECTORS_LOOP);

            if (supportsAVX512VLBW(__crb.___target)) // trying 64 bytes fast loop
            {
                __masm.cmpl(__cnt2, __stride2x2);
                __masm.jccb(AMD64Assembler.ConditionFlag.Below, __COMPARE_WIDE_VECTORS_LOOP_AVX2);
                __masm.testl(__cnt2, __stride2x2 - 1); // cnt2 holds the vector count
                // means we cannot subtract by 0x40
                __masm.jccb(AMD64Assembler.ConditionFlag.NotZero, __COMPARE_WIDE_VECTORS_LOOP_AVX2);

                __masm.bind(__COMPARE_WIDE_VECTORS_LOOP_AVX3); // the hottest loop
                if (this.___kind1 == this.___kind2)
                {
                    __masm.evmovdquq(__vec1, new AMD64Address(__str1, __result, __scale), AMD64Assembler.AvxVectorLen.AVX_512bit);
                    // k7 == 11..11, if operands equal, otherwise k7 has some 0
                    __masm.evpcmpeqb(AMD64.k7, __vec1, new AMD64Address(__str2, __result, __scale), AMD64Assembler.AvxVectorLen.AVX_512bit);
                }
                else
                {
                    __masm.vpmovzxbw(__vec1, new AMD64Address(__str1, __result, __scale1), AMD64Assembler.AvxVectorLen.AVX_512bit);
                    // k7 == 11..11, if operands equal, otherwise k7 has some 0
                    __masm.evpcmpeqb(AMD64.k7, __vec1, new AMD64Address(__str2, __result, __scale2), AMD64Assembler.AvxVectorLen.AVX_512bit);
                }
                __masm.kortestql(AMD64.k7, AMD64.k7);
                __masm.jcc(AMD64Assembler.ConditionFlag.AboveEqual, __COMPARE_WIDE_VECTORS_LOOP_FAILED); // miscompare
                __masm.addq(__result, __stride2x2); // update since we already compared at this addr
                __masm.subl(__cnt2, __stride2x2); // and sub the size too
                __masm.jccb(AMD64Assembler.ConditionFlag.NotZero, __COMPARE_WIDE_VECTORS_LOOP_AVX3);

                __masm.vpxor(__vec1, __vec1, __vec1);
                __masm.jmpb(__COMPARE_WIDE_TAIL);
            }

            __masm.bind(__COMPARE_WIDE_VECTORS_LOOP_AVX2);
            if (this.___kind1 == this.___kind2)
            {
                __masm.vmovdqu(__vec1, new AMD64Address(__str1, __result, __scale));
                __masm.vpxor(__vec1, __vec1, new AMD64Address(__str2, __result, __scale));
            }
            else
            {
                __masm.vpmovzxbw(__vec1, new AMD64Address(__str1, __result, __scale1), AMD64Assembler.AvxVectorLen.AVX_256bit);
                __masm.vpxor(__vec1, __vec1, new AMD64Address(__str2, __result, __scale2));
            }
            __masm.vptest(__vec1, __vec1);
            __masm.jcc(AMD64Assembler.ConditionFlag.NotZero, __VECTOR_NOT_EQUAL);
            __masm.addq(__result, stride2);
            __masm.subl(__cnt2, stride2);
            __masm.jcc(AMD64Assembler.ConditionFlag.NotZero, __COMPARE_WIDE_VECTORS_LOOP);
            // clean upper bits of YMM registers
            __masm.vpxor(__vec1, __vec1, __vec1);

            // compare wide vectors tail
            __masm.bind(__COMPARE_WIDE_TAIL);
            __masm.testq(__result, __result);
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __LENGTH_DIFF_LABEL);

            __masm.movl(__result, stride2);
            __masm.movl(__cnt2, __result);
            __masm.negq(__result);
            __masm.jmp(__COMPARE_WIDE_VECTORS_LOOP_AVX2);

            // Identifies the mismatching (higher or lower) 16-bytes in the 32-byte vectors.
            __masm.bind(__VECTOR_NOT_EQUAL);
            // clean upper bits of YMM registers
            __masm.vpxor(__vec1, __vec1, __vec1);
            if (this.___kind1 == this.___kind2)
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale));
            }
            else
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale1));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale2));
            }
            __masm.jmp(__COMPARE_16_CHARS);

            // compare tail chars, length between 1 to 15 chars
            __masm.bind(__COMPARE_TAIL_LONG);
            __masm.movl(__cnt2, __result);
            __masm.cmpl(__cnt2, stride);
            __masm.jcc(AMD64Assembler.ConditionFlag.Less, __COMPARE_SMALL_STR);

            if (this.___kind1 == this.___kind2)
            {
                __masm.movdqu(__vec1, new AMD64Address(__str1, 0));
            }
            else
            {
                __masm.pmovzxbw(__vec1, new AMD64Address(__str1, 0));
            }
            __masm.pcmpestri(__vec1, new AMD64Address(__str2, 0), __pcmpmask);
            __masm.jcc(AMD64Assembler.ConditionFlag.Below, __COMPARE_INDEX_CHAR);
            __masm.subq(__cnt2, stride);
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __LENGTH_DIFF_LABEL);
            if (this.___kind1 == this.___kind2)
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale));
            }
            else
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale1));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale2));
            }
            __masm.negq(__cnt2);
            __masm.jmpb(__WHILE_HEAD_LABEL);

            __masm.bind(__COMPARE_SMALL_STR);
        }
        else if (supportsSSE42(__crb.___target))
        {
            Register __vec1 = ValueUtil.asRegister(this.___vectorTemp1, AMD64Kind.DOUBLE);

            Label __COMPARE_WIDE_VECTORS = new Label();
            Label __VECTOR_NOT_EQUAL = new Label();
            Label __COMPARE_TAIL = new Label();

            int __pcmpmask = 0x19;
            // setup to compare 8-char (16-byte) vectors,
            // start from first character again because it has aligned address
            __masm.movl(__result, __cnt2);
            __masm.andl(__cnt2, ~(stride - 1)); // cnt2 holds the vector count
            if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
            {
                __pcmpmask &= ~0x01;
            }
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __COMPARE_TAIL);
            if (this.___kind1 == this.___kind2)
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale));
            }
            else
            {
                __masm.leaq(__str1, new AMD64Address(__str1, __result, __scale1));
                __masm.leaq(__str2, new AMD64Address(__str2, __result, __scale2));
            }
            __masm.negq(__result);

            // pcmpestri
            // inputs:
            // vec1- substring
            // rax - negative string length (elements count)
            // mem - scanned string
            // rdx - string length (elements count)
            // pcmpmask - cmp mode: 11000 (string compare with negated result)
            // + 00 (unsigned bytes) or + 01 (unsigned shorts)
            // outputs:
            // rcx - first mismatched element index

            __masm.bind(__COMPARE_WIDE_VECTORS);
            if (this.___kind1 == this.___kind2)
            {
                __masm.movdqu(__vec1, new AMD64Address(__str1, __result, __scale));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __result, __scale), __pcmpmask);
            }
            else
            {
                __masm.pmovzxbw(__vec1, new AMD64Address(__str1, __result, __scale1));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __result, __scale2), __pcmpmask);
            }

            // after pcmpestri cnt1(rcx) contains mismatched element index
            __masm.jccb(AMD64Assembler.ConditionFlag.Below, __VECTOR_NOT_EQUAL); // CF==1
            __masm.addq(__result, stride);
            __masm.subq(__cnt2, stride);
            __masm.jccb(AMD64Assembler.ConditionFlag.NotZero, __COMPARE_WIDE_VECTORS);

            // compare wide vectors tail
            __masm.testq(__result, __result);
            __masm.jcc(AMD64Assembler.ConditionFlag.Zero, __LENGTH_DIFF_LABEL);

            __masm.movl(__cnt2, stride);
            __masm.movl(__result, stride);
            __masm.negq(__result);
            if (this.___kind1 == this.___kind2)
            {
                __masm.movdqu(__vec1, new AMD64Address(__str1, __result, __scale));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __result, __scale), __pcmpmask);
            }
            else
            {
                __masm.pmovzxbw(__vec1, new AMD64Address(__str1, __result, __scale1));
                __masm.pcmpestri(__vec1, new AMD64Address(__str2, __result, __scale2), __pcmpmask);
            }
            __masm.jccb(AMD64Assembler.ConditionFlag.AboveEqual, __LENGTH_DIFF_LABEL);

            // mismatched characters in the vectors
            __masm.bind(__VECTOR_NOT_EQUAL);
            __masm.addq(__cnt1, __result);
            loadNextElements(__masm, __result, __cnt2, __str1, __str2, __scale, __scale1, __scale2, __cnt1);
            __masm.subl(__result, __cnt2);
            __masm.jmpb(__POP_LABEL);

            __masm.bind(__COMPARE_TAIL); // limit is zero
            __masm.movl(__cnt2, __result);
            // fallthrough to tail compare
        }

        // shift str2 and str1 to the end of the arrays, negate min
        if (this.___kind1 == this.___kind2)
        {
            __masm.leaq(__str1, new AMD64Address(__str1, __cnt2, __scale));
            __masm.leaq(__str2, new AMD64Address(__str2, __cnt2, __scale));
        }
        else
        {
            __masm.leaq(__str1, new AMD64Address(__str1, __cnt2, __scale1));
            __masm.leaq(__str2, new AMD64Address(__str2, __cnt2, __scale2));
        }
        __masm.decrementl(__cnt2); // first character was compared already
        __masm.negq(__cnt2);

        // compare the rest of the elements
        __masm.bind(__WHILE_HEAD_LABEL);
        loadNextElements(__masm, __result, __cnt1, __str1, __str2, __scale, __scale1, __scale2, __cnt2);
        __masm.subl(__result, __cnt1);
        __masm.jccb(AMD64Assembler.ConditionFlag.NotZero, __POP_LABEL);
        __masm.incrementq(__cnt2, 1);
        __masm.jccb(AMD64Assembler.ConditionFlag.NotZero, __WHILE_HEAD_LABEL);

        // strings are equal up to min length: return the length difference
        __masm.bind(__LENGTH_DIFF_LABEL);
        __masm.pop(__result);
        if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Char)
        {
            // divide diff by 2 to get number of chars
            __masm.sarl(__result, 1);
        }
        __masm.jmpb(__DONE_LABEL);

        if (supportsAVX512VLBW(__crb.___target))
        {
            __masm.bind(__COMPARE_WIDE_VECTORS_LOOP_FAILED);

            __masm.kmovql(__cnt1, AMD64.k7);
            __masm.notq(__cnt1);
            __masm.bsfq(__cnt2, __cnt1);
            if (this.___kind1 != JavaKind.Byte && this.___kind2 != JavaKind.Byte)
            {
                // divide diff by 2 to get number of chars
                __masm.sarl(__cnt2, 1);
            }
            __masm.addq(__result, __cnt2);
            if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
            {
                __masm.movzbl(__cnt1, new AMD64Address(__str2, __result, AMD64Address.Scale.Times1));
                __masm.movzbl(__result, new AMD64Address(__str1, __result, AMD64Address.Scale.Times1));
            }
            else if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Char)
            {
                __masm.movzwl(__cnt1, new AMD64Address(__str2, __result, __scale));
                __masm.movzwl(__result, new AMD64Address(__str1, __result, __scale));
            }
            else
            {
                __masm.movzwl(__cnt1, new AMD64Address(__str2, __result, __scale2));
                __masm.movzbl(__result, new AMD64Address(__str1, __result, __scale1));
            }
            __masm.subl(__result, __cnt1);
            __masm.jmpb(__POP_LABEL);
        }

        // discard the stored length difference
        __masm.bind(__POP_LABEL);
        __masm.pop(__cnt1);

        // that's it
        __masm.bind(__DONE_LABEL);
        if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Byte)
        {
            __masm.negl(__result);
        }
    }

    private void loadNextElements(AMD64MacroAssembler __masm, Register __elem1, Register __elem2, Register __str1, Register __str2, AMD64Address.Scale __scale, AMD64Address.Scale __scale1, AMD64Address.Scale __scale2, Register __index)
    {
        if (this.___kind1 == JavaKind.Byte && this.___kind2 == JavaKind.Byte)
        {
            __masm.movzbl(__elem1, new AMD64Address(__str1, __index, __scale, 0));
            __masm.movzbl(__elem2, new AMD64Address(__str2, __index, __scale, 0));
        }
        else if (this.___kind1 == JavaKind.Char && this.___kind2 == JavaKind.Char)
        {
            __masm.movzwl(__elem1, new AMD64Address(__str1, __index, __scale, 0));
            __masm.movzwl(__elem2, new AMD64Address(__str2, __index, __scale, 0));
        }
        else
        {
            __masm.movzbl(__elem1, new AMD64Address(__str1, __index, __scale1, 0));
            __masm.movzwl(__elem2, new AMD64Address(__str2, __index, __scale2, 0));
        }
    }
}
