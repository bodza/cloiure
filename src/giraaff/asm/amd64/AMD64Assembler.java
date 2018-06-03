package giraaff.asm.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.PlatformKind;

import giraaff.asm.Assembler;
import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.asm.amd64.AMD64AsmOptions;
import giraaff.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.core.common.NumUtil;
import giraaff.util.GraalError;

///
// This class implements an assembler that can encode most X86 instructions.
///
// @class AMD64Assembler
public class AMD64Assembler extends Assembler
{
    // @def
    private static final int MinEncodingNeedsRex = 8;

    ///
    // The x86 condition codes used for conditional jumps/moves.
    ///
    // @enum AMD64Assembler.ConditionFlag
    public enum ConditionFlag
    {
        Zero(0x4, "|zero|"),
        NotZero(0x5, "|nzero|"),
        Equal(0x4, "="),
        NotEqual(0x5, "!="),
        Less(0xc, "<"),
        LessEqual(0xe, "<="),
        Greater(0xf, ">"),
        GreaterEqual(0xd, ">="),
        Below(0x2, "|<|"),
        BelowEqual(0x6, "|<=|"),
        Above(0x7, "|>|"),
        AboveEqual(0x3, "|>=|"),
        Overflow(0x0, "|of|"),
        NoOverflow(0x1, "|nof|"),
        CarrySet(0x2, "|carry|"),
        CarryClear(0x3, "|ncarry|"),
        Negative(0x8, "|neg|"),
        Positive(0x9, "|pos|"),
        Parity(0xa, "|par|"),
        NoParity(0xb, "|npar|");

        // @field
        private final int ___value;
        // @field
        private final String ___operator;

        ConditionFlag(int __value, String __operator)
        {
            this.___value = __value;
            this.___operator = __operator;
        }

        public ConditionFlag negate()
        {
            switch (this)
            {
                case Zero:
                    return NotZero;
                case NotZero:
                    return Zero;
                case Equal:
                    return NotEqual;
                case NotEqual:
                    return Equal;
                case Less:
                    return GreaterEqual;
                case LessEqual:
                    return Greater;
                case Greater:
                    return LessEqual;
                case GreaterEqual:
                    return Less;
                case Below:
                    return AboveEqual;
                case BelowEqual:
                    return Above;
                case Above:
                    return BelowEqual;
                case AboveEqual:
                    return Below;
                case Overflow:
                    return NoOverflow;
                case NoOverflow:
                    return Overflow;
                case CarrySet:
                    return CarryClear;
                case CarryClear:
                    return CarrySet;
                case Negative:
                    return Positive;
                case Positive:
                    return Negative;
                case Parity:
                    return NoParity;
                case NoParity:
                    return Parity;
            }
            throw new IllegalArgumentException();
        }

        public int getValue()
        {
            return this.___value;
        }
    }

    ///
    // Constants for X86 prefix bytes.
    ///
    // @class AMD64Assembler.Prefix
    private static final class Prefix
    {
        // @defs
        private static final int
            REX        = 0x40,
            REXB       = 0x41,
            REXX       = 0x42,
            REXXB      = 0x43,
            REXR       = 0x44,
            REXRB      = 0x45,
            REXRX      = 0x46,
            REXRXB     = 0x47,
            REXW       = 0x48,
            REXWB      = 0x49,
            REXWX      = 0x4A,
            REXWXB     = 0x4B,
            REXWR      = 0x4C,
            REXWRB     = 0x4D,
            REXWRX     = 0x4E,
            REXWRXB    = 0x4F,
            VEX_3BYTES = 0xC4,
            VEX_2BYTES = 0xC5;
    }

    // @class AMD64Assembler.VexPrefix
    private static final class VexPrefix
    {
        // @defs
        private static final int
            VEX_R = 0x80,
            VEX_W = 0x80;
    }

    // @class AMD64Assembler.VexSimdPrefix
    private static final class VexSimdPrefix
    {
        // @defs
        private static final int
            VEX_SIMD_NONE = 0x0,
            VEX_SIMD_66   = 0x1,
            VEX_SIMD_F3   = 0x2,
            VEX_SIMD_F2   = 0x3;
    }

    // @class AMD64Assembler.VexOpcode
    private static final class VexOpcode
    {
        // @defs
        private static final int
            VEX_OPCODE_NONE  = 0x0,
            VEX_OPCODE_0F    = 0x1,
            VEX_OPCODE_0F_38 = 0x2,
            VEX_OPCODE_0F_3A = 0x3;
    }

    // @class AMD64Assembler.AvxVectorLen
    public static final class AvxVectorLen
    {
        // @defs
        public static final int
            AVX_128bit = 0x0,
            AVX_256bit = 0x1,
            AVX_512bit = 0x2,
            AVX_NoVec  = 0x4;
    }

    // @class AMD64Assembler.EvexTupleType
    public static final class EvexTupleType
    {
        // @defs
        public static final int
            EVEX_FV   =  0,
            EVEX_HV   =  4,
            EVEX_FVM  =  6,
            EVEX_T1S  =  7,
            EVEX_T1F  = 11,
            EVEX_T2   = 13,
            EVEX_T4   = 15,
            EVEX_T8   = 17,
            EVEX_HVM  = 18,
            EVEX_QVM  = 19,
            EVEX_OVM  = 20,
            EVEX_M128 = 21,
            EVEX_DUP  = 22,
            EVEX_ETUP = 23;
    }

    // @class AMD64Assembler.EvexInputSizeInBits
    public static final class EvexInputSizeInBits
    {
        // @defs
        public static final int
            EVEX_8bit  = 0,
            EVEX_16bit = 1,
            EVEX_32bit = 2,
            EVEX_64bit = 3,
            EVEX_NObit = 4;
    }

    // @field
    private AMD64InstructionAttr ___curAttributes;

    AMD64InstructionAttr getCurAttributes()
    {
        return this.___curAttributes;
    }

    void setCurAttributes(AMD64InstructionAttr __attributes)
    {
        this.___curAttributes = __attributes;
    }

    ///
    // The x86 operand sizes.
    ///
    // @enum AMD64Assembler.OperandSize
    public enum OperandSize
    {
        BYTE(1, AMD64Kind.BYTE)
        {
            @Override
            protected void emitImmediate(AMD64Assembler __asm, int __imm)
            {
                __asm.emitByte(__imm);
            }

            @Override
            protected int immediateSize()
            {
                return 1;
            }
        },

        WORD(2, AMD64Kind.WORD, 0x66)
        {
            @Override
            protected void emitImmediate(AMD64Assembler __asm, int __imm)
            {
                __asm.emitShort(__imm);
            }

            @Override
            protected int immediateSize()
            {
                return 2;
            }
        },

        DWORD(4, AMD64Kind.DWORD)
        {
            @Override
            protected void emitImmediate(AMD64Assembler __asm, int __imm)
            {
                __asm.emitInt(__imm);
            }

            @Override
            protected int immediateSize()
            {
                return 4;
            }
        },

        QWORD(8, AMD64Kind.QWORD)
        {
            @Override
            protected void emitImmediate(AMD64Assembler __asm, int __imm)
            {
                __asm.emitInt(__imm);
            }

            @Override
            protected int immediateSize()
            {
                return 4;
            }
        },

        SS(4, AMD64Kind.SINGLE, 0xF3, true),

        SD(8, AMD64Kind.DOUBLE, 0xF2, true),

        PS(16, AMD64Kind.V128_SINGLE, true),

        PD(16, AMD64Kind.V128_DOUBLE, 0x66, true);

        // @field
        private final int ___sizePrefix;
        // @field
        private final int ___bytes;
        // @field
        private final boolean ___xmm;
        // @field
        private final AMD64Kind ___kind;

        OperandSize(int __bytes, AMD64Kind __kind)
        {
            this(__bytes, __kind, 0);
        }

        OperandSize(int __bytes, AMD64Kind __kind, int __sizePrefix)
        {
            this(__bytes, __kind, __sizePrefix, false);
        }

        OperandSize(int __bytes, AMD64Kind __kind, boolean __xmm)
        {
            this(__bytes, __kind, 0, __xmm);
        }

        OperandSize(int __bytes, AMD64Kind __kind, int __sizePrefix, boolean __xmm)
        {
            this.___sizePrefix = __sizePrefix;
            this.___bytes = __bytes;
            this.___kind = __kind;
            this.___xmm = __xmm;
        }

        public int getBytes()
        {
            return this.___bytes;
        }

        public boolean isXmmType()
        {
            return this.___xmm;
        }

        public AMD64Kind getKind()
        {
            return this.___kind;
        }

        public static OperandSize get(PlatformKind __kind)
        {
            for (OperandSize __operandSize : OperandSize.values())
            {
                if (__operandSize.___kind.equals(__kind))
                {
                    return __operandSize;
                }
            }
            throw GraalError.shouldNotReachHere("unexpected kind: " + __kind);
        }

        ///
        // Emit an immediate of this size. Note that immediate {@link #QWORD} operands are encoded
        // as sign-extended 32-bit values.
        ///
        protected void emitImmediate(AMD64Assembler __asm, int __imm)
        {
            throw new UnsupportedOperationException();
        }

        protected int immediateSize()
        {
            throw new UnsupportedOperationException();
        }
    }

    ///
    // Operand size and register type constraints.
    ///
    // @enum AMD64Assembler.OpAssertion
    private enum OpAssertion
    {
        ByteAssertion(AMD64.CPU, AMD64.CPU, OperandSize.BYTE),
        ByteOrLargerAssertion(AMD64.CPU, AMD64.CPU, OperandSize.BYTE, OperandSize.WORD, OperandSize.DWORD, OperandSize.QWORD),
        WordOrLargerAssertion(AMD64.CPU, AMD64.CPU, OperandSize.WORD, OperandSize.DWORD, OperandSize.QWORD),
        DwordOrLargerAssertion(AMD64.CPU, AMD64.CPU, OperandSize.DWORD, OperandSize.QWORD),
        WordOrDwordAssertion(AMD64.CPU, AMD64.CPU, OperandSize.WORD, OperandSize.QWORD),
        QwordAssertion(AMD64.CPU, AMD64.CPU, OperandSize.QWORD),
        FloatAssertion(AMD64.XMM, AMD64.XMM, OperandSize.SS, OperandSize.SD, OperandSize.PS, OperandSize.PD),
        PackedFloatAssertion(AMD64.XMM, AMD64.XMM, OperandSize.PS, OperandSize.PD),
        SingleAssertion(AMD64.XMM, AMD64.XMM, OperandSize.SS),
        DoubleAssertion(AMD64.XMM, AMD64.XMM, OperandSize.SD),
        PackedDoubleAssertion(AMD64.XMM, AMD64.XMM, OperandSize.PD),
        IntToFloatAssertion(AMD64.XMM, AMD64.CPU, OperandSize.DWORD, OperandSize.QWORD),
        FloatToIntAssertion(AMD64.CPU, AMD64.XMM, OperandSize.DWORD, OperandSize.QWORD);

        // @field
        private final RegisterCategory ___resultCategory;
        // @field
        private final RegisterCategory ___inputCategory;
        // @field
        private final OperandSize[] ___allowedSizes;

        OpAssertion(RegisterCategory __resultCategory, RegisterCategory __inputCategory, OperandSize... __allowedSizes)
        {
            this.___resultCategory = __resultCategory;
            this.___inputCategory = __inputCategory;
            this.___allowedSizes = __allowedSizes;
        }

        protected boolean checkOperands(AMD64Op __op, OperandSize __size, Register __resultReg, Register __inputReg)
        {
            for (OperandSize __s : this.___allowedSizes)
            {
                if (__size == __s)
                {
                    return true;
                }
            }

            return false;
        }
    }

    ///
    // Constructs an assembler for the AMD64 architecture.
    ///
    // @cons
    public AMD64Assembler(TargetDescription __target)
    {
        super(__target);
    }

    public boolean supports(CPUFeature __feature)
    {
        return ((AMD64) this.___target.arch).getFeatures().contains(__feature);
    }

    private static int encode(Register __r)
    {
        return __r.encoding & 0x7;
    }

    ///
    // Get RXB bits for register-register instruction. In that encoding, ModRM.rm contains a
    // register index. The R bit extends the ModRM.reg field and the B bit extends the ModRM.rm
    // field. The X bit must be 0.
    ///
    protected static int getRXB(Register __reg, Register __rm)
    {
        int __rxb = (__reg == null ? 0 : __reg.encoding & 0x08) >> 1;
        __rxb |= (__rm == null ? 0 : __rm.encoding & 0x08) >> 3;
        return __rxb;
    }

    ///
    // Get RXB bits for register-memory instruction. The R bit extends the ModRM.reg field. There
    // are two cases for the memory operand:
    // ModRM.rm contains the base register: In that case, B extends the ModRM.rm field and X = 0.
    //
    // There is an SIB byte: In that case, X extends SIB.index and B extends SIB.base.
    ///
    protected static int getRXB(Register __reg, AMD64Address __rm)
    {
        int __rxb = (__reg == null ? 0 : __reg.encoding & 0x08) >> 1;
        if (!__rm.getIndex().equals(Register.None))
        {
            __rxb |= (__rm.getIndex().encoding & 0x08) >> 2;
        }
        if (!__rm.getBase().equals(Register.None))
        {
            __rxb |= (__rm.getBase().encoding & 0x08) >> 3;
        }
        return __rxb;
    }

    ///
    // Emit the ModR/M byte for one register operand and an opcode extension in the R field.
    //
    // Format: [ 11 reg r/m ]
    ///
    protected void emitModRM(int __reg, Register __rm)
    {
        emitByte(0xC0 | (__reg << 3) | (__rm.encoding & 0x07));
    }

    ///
    // Emit the ModR/M byte for two register operands.
    //
    // Format: [ 11 reg r/m ]
    ///
    protected void emitModRM(Register __reg, Register __rm)
    {
        emitModRM(__reg.encoding & 0x07, __rm);
    }

    protected void emitOperandHelper(Register __reg, AMD64Address __addr, int __additionalInstructionSize)
    {
        emitOperandHelper(encode(__reg), __addr, false, __additionalInstructionSize);
    }

    ///
    // Emits the ModR/M byte and optionally the SIB byte for one register and one memory operand.
    //
    // @param force4Byte use 4 byte encoding for displacements that would normally fit in a byte
    ///
    protected void emitOperandHelper(Register __reg, AMD64Address __addr, boolean __force4Byte, int __additionalInstructionSize)
    {
        emitOperandHelper(encode(__reg), __addr, __force4Byte, __additionalInstructionSize);
    }

    protected void emitOperandHelper(int __reg, AMD64Address __addr, int __additionalInstructionSize)
    {
        emitOperandHelper(__reg, __addr, false, __additionalInstructionSize);
    }

    ///
    // Emits the ModR/M byte and optionally the SIB byte for one memory operand and an opcode
    // extension in the R field.
    //
    // @param force4Byte use 4 byte encoding for displacements that would normally fit in a byte
    // @param additionalInstructionSize the number of bytes that will be emitted after the operand,
    //            so that the start position of the next instruction can be computed even though
    //            this instruction has not been completely emitted yet.
    ///
    protected void emitOperandHelper(int __reg, AMD64Address __addr, boolean __force4Byte, int __additionalInstructionSize)
    {
        int __regenc = __reg << 3;

        Register __base = __addr.getBase();
        Register __index = __addr.getIndex();

        AMD64Address.Scale __scale = __addr.getScale();
        int __disp = __addr.getDisplacement();

        if (__base.equals(AMD64.rip)) // also matches addresses returned by getPlaceholder()
        {
            // [00 000 101] disp32
            emitByte(0x05 | __regenc);
            emitInt(__disp);
        }
        else if (__base.isValid())
        {
            int __baseenc = __base.isValid() ? encode(__base) : 0;
            if (__index.isValid())
            {
                int __indexenc = encode(__index) << 3;
                // [base + indexscale + disp]
                if (__disp == 0 && !__base.equals(AMD64.rbp) && !__base.equals(AMD64.r13))
                {
                    // [base + indexscale]
                    // [00 reg 100][ss index base]
                    emitByte(0x04 | __regenc);
                    emitByte(__scale.___log2 << 6 | __indexenc | __baseenc);
                }
                else if (NumUtil.isByte(__disp) && !__force4Byte)
                {
                    // [base + indexscale + imm8]
                    // [01 reg 100][ss index base] imm8
                    emitByte(0x44 | __regenc);
                    emitByte(__scale.___log2 << 6 | __indexenc | __baseenc);
                    emitByte(__disp & 0xFF);
                }
                else
                {
                    // [base + indexscale + disp32]
                    // [10 reg 100][ss index base] disp32
                    emitByte(0x84 | __regenc);
                    emitByte(__scale.___log2 << 6 | __indexenc | __baseenc);
                    emitInt(__disp);
                }
            }
            else if (__base.equals(AMD64.rsp) || __base.equals(AMD64.r12))
            {
                // [rsp + disp]
                if (__disp == 0)
                {
                    // [rsp]
                    // [00 reg 100][00 100 100]
                    emitByte(0x04 | __regenc);
                    emitByte(0x24);
                }
                else if (NumUtil.isByte(__disp) && !__force4Byte)
                {
                    // [rsp + imm8]
                    // [01 reg 100][00 100 100] disp8
                    emitByte(0x44 | __regenc);
                    emitByte(0x24);
                    emitByte(__disp & 0xFF);
                }
                else
                {
                    // [rsp + imm32]
                    // [10 reg 100][00 100 100] disp32
                    emitByte(0x84 | __regenc);
                    emitByte(0x24);
                    emitInt(__disp);
                }
            }
            else
            {
                // [base + disp]
                if (__disp == 0 && !__base.equals(AMD64.rbp) && !__base.equals(AMD64.r13))
                {
                    // [base]
                    // [00 reg base]
                    emitByte(0x00 | __regenc | __baseenc);
                }
                else if (NumUtil.isByte(__disp) && !__force4Byte)
                {
                    // [base + disp8]
                    // [01 reg base] disp8
                    emitByte(0x40 | __regenc | __baseenc);
                    emitByte(__disp & 0xFF);
                }
                else
                {
                    // [base + disp32]
                    // [10 reg base] disp32
                    emitByte(0x80 | __regenc | __baseenc);
                    emitInt(__disp);
                }
            }
        }
        else
        {
            if (__index.isValid())
            {
                int __indexenc = encode(__index) << 3;
                // [indexscale + disp]
                // [00 reg 100][ss index 101] disp32
                emitByte(0x04 | __regenc);
                emitByte(__scale.___log2 << 6 | __indexenc | 0x05);
                emitInt(__disp);
            }
            else
            {
                // [disp] ABSOLUTE
                // [00 reg 100][00 100 101] disp32
                emitByte(0x04 | __regenc);
                emitByte(0x25);
                emitInt(__disp);
            }
        }
        setCurAttributes(null);
    }

    ///
    // Base class for AMD64 opcodes.
    ///
    // @class AMD64Assembler.AMD64Op
    public static class AMD64Op
    {
        // @def
        protected static final int P_0F = 0x0F;
        // @def
        protected static final int P_0F38 = 0x380F;
        // @def
        protected static final int P_0F3A = 0x3A0F;

        // @field
        private final String ___opcode;

        // @field
        protected final int ___prefix1;
        // @field
        protected final int ___prefix2;
        // @field
        protected final int ___op;

        // @field
        private final boolean ___dstIsByte;
        // @field
        private final boolean ___srcIsByte;

        // @field
        private final OpAssertion ___assertion;
        // @field
        private final CPUFeature ___feature;

        // @cons
        protected AMD64Op(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            this(__opcode, __prefix1, __prefix2, __op, __assertion == OpAssertion.ByteAssertion, __assertion == OpAssertion.ByteAssertion, __assertion, __feature);
        }

        // @cons
        protected AMD64Op(String __opcode, int __prefix1, int __prefix2, int __op, boolean __dstIsByte, boolean __srcIsByte, OpAssertion __assertion, CPUFeature __feature)
        {
            super();
            this.___opcode = __opcode;
            this.___prefix1 = __prefix1;
            this.___prefix2 = __prefix2;
            this.___op = __op;

            this.___dstIsByte = __dstIsByte;
            this.___srcIsByte = __srcIsByte;

            this.___assertion = __assertion;
            this.___feature = __feature;
        }

        protected final void emitOpcode(AMD64Assembler __asm, OperandSize __size, int __rxb, int __dstEnc, int __srcEnc)
        {
            if (this.___prefix1 != 0)
            {
                __asm.emitByte(this.___prefix1);
            }
            if (__size.___sizePrefix != 0)
            {
                __asm.emitByte(__size.___sizePrefix);
            }
            int __rexPrefix = 0x40 | __rxb;
            if (__size == OperandSize.QWORD)
            {
                __rexPrefix |= 0x08;
            }
            if (__rexPrefix != 0x40 || (this.___dstIsByte && __dstEnc >= 4) || (this.___srcIsByte && __srcEnc >= 4))
            {
                __asm.emitByte(__rexPrefix);
            }
            if (this.___prefix2 > 0xFF)
            {
                __asm.emitShort(this.___prefix2);
            }
            else if (this.___prefix2 > 0)
            {
                __asm.emitByte(this.___prefix2);
            }
            __asm.emitByte(this.___op);
        }
    }

    ///
    // Base class for AMD64 opcodes with immediate operands.
    ///
    // @class AMD64Assembler.AMD64ImmOp
    public static class AMD64ImmOp extends AMD64Op
    {
        // @field
        private final boolean ___immIsByte;

        // @cons
        protected AMD64ImmOp(String __opcode, boolean __immIsByte, int __prefix, int __op, OpAssertion __assertion)
        {
            super(__opcode, 0, __prefix, __op, __assertion, null);
            this.___immIsByte = __immIsByte;
        }

        protected final void emitImmediate(AMD64Assembler __asm, OperandSize __size, int __imm)
        {
            if (this.___immIsByte)
            {
                __asm.emitByte(__imm);
            }
            else
            {
                __size.emitImmediate(__asm, __imm);
            }
        }

        protected final int immediateSize(OperandSize __size)
        {
            if (this.___immIsByte)
            {
                return 1;
            }
            else
            {
                return __size.___bytes;
            }
        }
    }

    ///
    // Opcode with operand order of either RM or MR for 2 address forms.
    ///
    // @class AMD64Assembler.AMD64RROp
    public abstract static class AMD64RROp extends AMD64Op
    {
        // @cons
        protected AMD64RROp(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __assertion, __feature);
        }

        // @cons
        protected AMD64RROp(String __opcode, int __prefix1, int __prefix2, int __op, boolean __dstIsByte, boolean __srcIsByte, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __dstIsByte, __srcIsByte, __assertion, __feature);
        }

        public abstract void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __src);
    }

    ///
    // Opcode with operand order of either RM or MR for 3 address forms.
    ///
    // @class AMD64Assembler.AMD64RRROp
    public abstract static class AMD64RRROp extends AMD64Op
    {
        // @cons
        protected AMD64RRROp(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __assertion, __feature);
        }

        // @cons
        protected AMD64RRROp(String __opcode, int __prefix1, int __prefix2, int __op, boolean __dstIsByte, boolean __srcIsByte, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __dstIsByte, __srcIsByte, __assertion, __feature);
        }

        public abstract void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __nds, Register __src);
    }

    ///
    // Opcode with operand order of RM.
    ///
    // @class AMD64Assembler.AMD64RMOp
    public static class AMD64RMOp extends AMD64RROp
    {
        // @defs
        public static final AMD64RMOp
            IMUL   = new AMD64RMOp("IMUL",         P_0F, 0xAF, OpAssertion.ByteOrLargerAssertion),
            BSF    = new AMD64RMOp("BSF",          P_0F, 0xBC),
            BSR    = new AMD64RMOp("BSR",          P_0F, 0xBD),
            POPCNT = new AMD64RMOp("POPCNT", 0xF3, P_0F, 0xB8, CPUFeature.POPCNT),
            TZCNT  = new AMD64RMOp("TZCNT",  0xF3, P_0F, 0xBC, CPUFeature.BMI1),
            LZCNT  = new AMD64RMOp("LZCNT",  0xF3, P_0F, 0xBD, CPUFeature.LZCNT),
            MOVZXB = new AMD64RMOp("MOVZXB",       P_0F, 0xB6, false, true, OpAssertion.WordOrLargerAssertion),
            MOVZX  = new AMD64RMOp("MOVZX",        P_0F, 0xB7, OpAssertion.DwordOrLargerAssertion),
            MOVSXB = new AMD64RMOp("MOVSXB",       P_0F, 0xBE, false, true, OpAssertion.WordOrLargerAssertion),
            MOVSX  = new AMD64RMOp("MOVSX",        P_0F, 0xBF, OpAssertion.DwordOrLargerAssertion),
            MOVSXD = new AMD64RMOp("MOVSXD",             0x63, OpAssertion.QwordAssertion),
            MOVB   = new AMD64RMOp("MOVB",               0x8A, OpAssertion.ByteAssertion),
            MOV    = new AMD64RMOp("MOV",                0x8B);

        // MOVD/MOVQ and MOVSS/MOVSD are the same opcode, just with different operand size prefix.
        // @defs
        public static final AMD64RMOp
            MOVD   = new AMD64RMOp("MOVD",   0x66, P_0F, 0x6E, OpAssertion.IntToFloatAssertion, CPUFeature.SSE2),
            MOVQ   = new AMD64RMOp("MOVQ",   0x66, P_0F, 0x6E, OpAssertion.IntToFloatAssertion, CPUFeature.SSE2),
            MOVSS  = new AMD64RMOp("MOVSS",        P_0F, 0x10, OpAssertion.FloatAssertion, CPUFeature.SSE),
            MOVSD  = new AMD64RMOp("MOVSD",        P_0F, 0x10, OpAssertion.FloatAssertion, CPUFeature.SSE);

        // TEST is documented as MR operation, but it's symmetric, and using it as RM operation is more convenient.
        // @defs
        public static final AMD64RMOp
            TESTB  = new AMD64RMOp("TEST",               0x84, OpAssertion.ByteAssertion),
            TEST   = new AMD64RMOp("TEST",               0x85);

        // @cons
        protected AMD64RMOp(String __opcode, int __op)
        {
            this(__opcode, 0, __op);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __op, __assertion);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix, int __op)
        {
            this(__opcode, 0, __prefix, __op, null);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __prefix, __op, __assertion, null);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            this(__opcode, 0, __prefix, __op, __assertion, __feature);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix, int __op, boolean __dstIsByte, boolean __srcIsByte, OpAssertion __assertion)
        {
            super(__opcode, 0, __prefix, __op, __dstIsByte, __srcIsByte, __assertion, null);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix1, int __prefix2, int __op, CPUFeature __feature)
        {
            this(__opcode, __prefix1, __prefix2, __op, OpAssertion.WordOrLargerAssertion, __feature);
        }

        // @cons
        protected AMD64RMOp(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __assertion, __feature);
        }

        @Override
        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __src)
        {
            boolean __isSimd = false;
            boolean __noNds = false;

            switch (this.___op)
            {
                case 0x2A:
                case 0x2C:
                case 0x2E:
                case 0x5A:
                case 0x6E:
                    __isSimd = true;
                    __noNds = true;
                    break;
                case 0x10:
                case 0x51:
                case 0x54:
                case 0x55:
                case 0x56:
                case 0x57:
                case 0x58:
                case 0x59:
                case 0x5C:
                case 0x5D:
                case 0x5E:
                case 0x5F:
                    __isSimd = true;
                    break;
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __opc = VexOpcode.VEX_OPCODE_NONE;
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                int __encode;
                if (__noNds)
                {
                    __encode = __asm.simdPrefixAndEncode(__dst, Register.None, __src, __pre, __opc, __attributes);
                }
                else
                {
                    __encode = __asm.simdPrefixAndEncode(__dst, __dst, __src, __pre, __opc, __attributes);
                }
                __asm.emitByte(this.___op);
                __asm.emitByte(0xC0 | __encode);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__dst, __src), __dst.encoding, __src.encoding);
                __asm.emitModRM(__dst, __src);
            }
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, AMD64Address __src)
        {
            boolean __isSimd = false;
            boolean __noNds = false;

            switch (this.___op)
            {
                case 0x10:
                case 0x2A:
                case 0x2C:
                case 0x2E:
                case 0x6E:
                    __isSimd = true;
                    __noNds = true;
                    break;
                case 0x51:
                case 0x54:
                case 0x55:
                case 0x56:
                case 0x57:
                case 0x58:
                case 0x59:
                case 0x5C:
                case 0x5D:
                case 0x5E:
                case 0x5F:
                    __isSimd = true;
                    break;
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                if (__noNds)
                {
                    __asm.simdPrefix(__dst, Register.None, __src, __pre, __opc, __attributes);
                }
                else
                {
                    __asm.simdPrefix(__dst, __dst, __src, __pre, __opc, __attributes);
                }
                __asm.emitByte(this.___op);
                __asm.emitOperandHelper(__dst, __src, 0);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__dst, __src), __dst.encoding, 0);
                __asm.emitOperandHelper(__dst, __src, 0);
            }
        }
    }

    ///
    // Opcode with operand order of RM.
    ///
    // @class AMD64Assembler.AMD64RRMOp
    public static class AMD64RRMOp extends AMD64RRROp
    {
        // @cons
        protected AMD64RRMOp(String __opcode, int __op)
        {
            this(__opcode, 0, __op);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __op, __assertion);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix, int __op)
        {
            this(__opcode, 0, __prefix, __op, null);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __prefix, __op, __assertion, null);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            this(__opcode, 0, __prefix, __op, __assertion, __feature);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix, int __op, boolean __dstIsByte, boolean __srcIsByte, OpAssertion __assertion)
        {
            super(__opcode, 0, __prefix, __op, __dstIsByte, __srcIsByte, __assertion, null);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix1, int __prefix2, int __op, CPUFeature __feature)
        {
            this(__opcode, __prefix1, __prefix2, __op, OpAssertion.WordOrLargerAssertion, __feature);
        }

        // @cons
        protected AMD64RRMOp(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __assertion, __feature);
        }

        @Override
        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __nds, Register __src)
        {
            int __pre;
            int __opc;
            boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
            int __curPrefix = __size.___sizePrefix | this.___prefix1;
            switch (__curPrefix)
            {
                case 0x66:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_66;
                    break;
                }
                case 0xF2:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_F2;
                    break;
                }
                case 0xF3:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_F3;
                    break;
                }
                default:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_NONE;
                    break;
                }
            }
            switch (this.___prefix2)
            {
                case P_0F:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F;
                    break;
                }
                case P_0F38:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F_38;
                    break;
                }
                case P_0F3A:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F_3A;
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere("invalid VEX instruction prefix");
            }
            int __encode;
            __encode = __asm.simdPrefixAndEncode(__dst, __nds, __src, __pre, __opc, __attributes);
            __asm.emitByte(this.___op);
            __asm.emitByte(0xC0 | __encode);
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __nds, AMD64Address __src)
        {
            int __pre;
            int __opc;
            boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
            int __curPrefix = __size.___sizePrefix | this.___prefix1;
            switch (__curPrefix)
            {
                case 0x66:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_66;
                    break;
                }
                case 0xF2:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_F2;
                    break;
                }
                case 0xF3:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_F3;
                    break;
                }
                default:
                {
                    __pre = VexSimdPrefix.VEX_SIMD_NONE;
                    break;
                }
            }
            switch (this.___prefix2)
            {
                case P_0F:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F;
                    break;
                }
                case P_0F38:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F_38;
                    break;
                }
                case P_0F3A:
                {
                    __opc = VexOpcode.VEX_OPCODE_0F_3A;
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere("invalid VEX instruction prefix");
            }
            __asm.simdPrefix(__dst, __nds, __src, __pre, __opc, __attributes);
            __asm.emitByte(this.___op);
            __asm.emitOperandHelper(__dst, __src, 0);
        }
    }

    ///
    // Opcode with operand order of MR.
    ///
    // @class AMD64Assembler.AMD64MROp
    public static final class AMD64MROp extends AMD64RROp
    {
        public static final AMD64MROp MOVB   = new AMD64MROp("MOVB",             0x88, OpAssertion.ByteAssertion);
        public static final AMD64MROp MOV    = new AMD64MROp("MOV",              0x89);

        // MOVD and MOVQ are the same opcode, just with different operand size prefix.
        // Note that as MR opcodes, they have reverse operand order, so the IntToFloatingAssertion must be used.
        public static final AMD64MROp MOVD   = new AMD64MROp("MOVD", 0x66, P_0F, 0x7E, OpAssertion.IntToFloatAssertion, CPUFeature.SSE2);
        public static final AMD64MROp MOVQ   = new AMD64MROp("MOVQ", 0x66, P_0F, 0x7E, OpAssertion.IntToFloatAssertion, CPUFeature.SSE2);

        // MOVSS and MOVSD are the same opcode, just with different operand size prefix.
        public static final AMD64MROp MOVSS  = new AMD64MROp("MOVSS",      P_0F, 0x11, OpAssertion.FloatAssertion, CPUFeature.SSE);
        public static final AMD64MROp MOVSD  = new AMD64MROp("MOVSD",      P_0F, 0x11, OpAssertion.FloatAssertion, CPUFeature.SSE);

        // @cons
        protected AMD64MROp(String __opcode, int __op)
        {
            this(__opcode, 0, __op);
        }

        // @cons
        protected AMD64MROp(String __opcode, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __op, __assertion);
        }

        // @cons
        protected AMD64MROp(String __opcode, int __prefix, int __op)
        {
            this(__opcode, __prefix, __op, OpAssertion.WordOrLargerAssertion);
        }

        // @cons
        protected AMD64MROp(String __opcode, int __prefix, int __op, OpAssertion __assertion)
        {
            this(__opcode, __prefix, __op, __assertion, null);
        }

        // @cons
        protected AMD64MROp(String __opcode, int __prefix, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            this(__opcode, 0, __prefix, __op, __assertion, __feature);
        }

        // @cons
        protected AMD64MROp(String __opcode, int __prefix1, int __prefix2, int __op, OpAssertion __assertion, CPUFeature __feature)
        {
            super(__opcode, __prefix1, __prefix2, __op, __assertion, __feature);
        }

        @Override
        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __src)
        {
            boolean __isSimd = false;
            boolean __noNds = false;

            switch (this.___op)
            {
                case 0x7E:
                    __isSimd = true;
                    __noNds = true;
                    break;
                case 0x11:
                    __isSimd = true;
                    break;
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                int __encode;
                if (__noNds)
                {
                    __encode = __asm.simdPrefixAndEncode(__src, Register.None, __dst, __pre, __opc, __attributes);
                }
                else
                {
                    __encode = __asm.simdPrefixAndEncode(__src, __src, __dst, __pre, __opc, __attributes);
                }
                __asm.emitByte(this.___op);
                __asm.emitByte(0xC0 | __encode);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__src, __dst), __src.encoding, __dst.encoding);
                __asm.emitModRM(__src, __dst);
            }
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, AMD64Address __dst, Register __src)
        {
            boolean __isSimd = false;

            switch (this.___op)
            {
                case 0x7E:
                case 0x11:
                {
                    __isSimd = true;
                    break;
                }
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                boolean __rexVexW = (__size == OperandSize.QWORD) ? true : false;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __rexVexW, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                __asm.simdPrefix(__src, Register.None, __dst, __pre, __opc, __attributes);
                __asm.emitByte(this.___op);
                __asm.emitOperandHelper(__src, __dst, 0);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__src, __dst), __src.encoding, 0);
                __asm.emitOperandHelper(__src, __dst, 0);
            }
        }
    }

    ///
    // Opcodes with operand order of M.
    ///
    // @class AMD64Assembler.AMD64MOp
    public static final class AMD64MOp extends AMD64Op
    {
        // @defs
        public static final AMD64MOp
            NOT  = new AMD64MOp("NOT",  0xF7, 2),
            NEG  = new AMD64MOp("NEG",  0xF7, 3),
            MUL  = new AMD64MOp("MUL",  0xF7, 4),
            IMUL = new AMD64MOp("IMUL", 0xF7, 5),
            DIV  = new AMD64MOp("DIV",  0xF7, 6),
            IDIV = new AMD64MOp("IDIV", 0xF7, 7),
            INC  = new AMD64MOp("INC",  0xFF, 0),
            DEC  = new AMD64MOp("DEC",  0xFF, 1),
            PUSH = new AMD64MOp("PUSH", 0xFF, 6),
            POP  = new AMD64MOp("POP",  0x8F, 0, OpAssertion.WordOrDwordAssertion);

        // @field
        private final int ___ext;

        // @cons
        protected AMD64MOp(String __opcode, int __op, int __ext)
        {
            this(__opcode, 0, __op, __ext);
        }

        // @cons
        protected AMD64MOp(String __opcode, int __prefix, int __op, int __ext)
        {
            this(__opcode, __prefix, __op, __ext, OpAssertion.WordOrLargerAssertion);
        }

        // @cons
        protected AMD64MOp(String __opcode, int __op, int __ext, OpAssertion __assertion)
        {
            this(__opcode, 0, __op, __ext, __assertion);
        }

        // @cons
        protected AMD64MOp(String __opcode, int __prefix, int __op, int __ext, OpAssertion __assertion)
        {
            super(__opcode, 0, __prefix, __op, __assertion, null);
            this.___ext = __ext;
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst)
        {
            emitOpcode(__asm, __size, getRXB(null, __dst), 0, __dst.encoding);
            __asm.emitModRM(this.___ext, __dst);
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, AMD64Address __dst)
        {
            emitOpcode(__asm, __size, getRXB(null, __dst), 0, 0);
            __asm.emitOperandHelper(this.___ext, __dst, 0);
        }
    }

    ///
    // Opcodes with operand order of MI.
    ///
    // @class AMD64Assembler.AMD64MIOp
    public static final class AMD64MIOp extends AMD64ImmOp
    {
        // @defs
        public static final AMD64MIOp
            MOVB = new AMD64MIOp("MOVB", true,  0xC6, 0, OpAssertion.ByteAssertion),
            MOV  = new AMD64MIOp("MOV",  false, 0xC7, 0),
            TEST = new AMD64MIOp("TEST", false, 0xF7, 0);

        // @field
        private final int ___ext;

        // @cons
        protected AMD64MIOp(String __opcode, boolean __immIsByte, int __op, int __ext)
        {
            this(__opcode, __immIsByte, __op, __ext, OpAssertion.WordOrLargerAssertion);
        }

        // @cons
        protected AMD64MIOp(String __opcode, boolean __immIsByte, int __op, int __ext, OpAssertion __assertion)
        {
            this(__opcode, __immIsByte, 0, __op, __ext, __assertion);
        }

        // @cons
        protected AMD64MIOp(String __opcode, boolean __immIsByte, int __prefix, int __op, int __ext, OpAssertion __assertion)
        {
            super(__opcode, __immIsByte, __prefix, __op, __assertion);
            this.___ext = __ext;
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, int __imm)
        {
            emitOpcode(__asm, __size, getRXB(null, __dst), 0, __dst.encoding);
            __asm.emitModRM(this.___ext, __dst);
            emitImmediate(__asm, __size, __imm);
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, AMD64Address __dst, int __imm)
        {
            emitOpcode(__asm, __size, getRXB(null, __dst), 0, 0);
            __asm.emitOperandHelper(this.___ext, __dst, immediateSize(__size));
            emitImmediate(__asm, __size, __imm);
        }
    }

    ///
    // Opcodes with operand order of RMI.
    //
    // We only have one form of round as the operation is always treated with single variant input,
    // making its extension to 3 address forms redundant.
    ///
    // @class AMD64Assembler.AMD64RMIOp
    public static final class AMD64RMIOp extends AMD64ImmOp
    {
        // @defs
        public static final AMD64RMIOp
            IMUL    = new AMD64RMIOp("IMUL",    false,        0x69),
            IMUL_SX = new AMD64RMIOp("IMUL",    true,         0x6B),
            ROUNDSS = new AMD64RMIOp("ROUNDSS", true, P_0F3A, 0x0A, OpAssertion.PackedDoubleAssertion),
            ROUNDSD = new AMD64RMIOp("ROUNDSD", true, P_0F3A, 0x0B, OpAssertion.PackedDoubleAssertion);

        // @cons
        protected AMD64RMIOp(String __opcode, boolean __immIsByte, int __op)
        {
            this(__opcode, __immIsByte, 0, __op, OpAssertion.WordOrLargerAssertion);
        }

        // @cons
        protected AMD64RMIOp(String __opcode, boolean __immIsByte, int __prefix, int __op, OpAssertion __assertion)
        {
            super(__opcode, __immIsByte, __prefix, __op, __assertion);
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, Register __src, int __imm)
        {
            boolean __isSimd = false;
            boolean __noNds = false;

            switch (this.___op)
            {
                case 0x0A:
                case 0x0B:
                    __isSimd = true;
                    __noNds = true;
                    break;
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                int __encode;
                if (__noNds)
                {
                    __encode = __asm.simdPrefixAndEncode(__dst, Register.None, __src, __pre, __opc, __attributes);
                }
                else
                {
                    __encode = __asm.simdPrefixAndEncode(__dst, __dst, __src, __pre, __opc, __attributes);
                }
                __asm.emitByte(this.___op);
                __asm.emitByte(0xC0 | __encode);
                emitImmediate(__asm, __size, __imm);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__dst, __src), __dst.encoding, __src.encoding);
                __asm.emitModRM(__dst, __src);
                emitImmediate(__asm, __size, __imm);
            }
        }

        public final void emit(AMD64Assembler __asm, OperandSize __size, Register __dst, AMD64Address __src, int __imm)
        {
            boolean __isSimd = false;
            boolean __noNds = false;

            switch (this.___op)
            {
                case 0x0A:
                case 0x0B:
                    __isSimd = true;
                    __noNds = true;
                    break;
            }

            int __opc = 0;
            if (__isSimd)
            {
                switch (this.___prefix2)
                {
                    case P_0F:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F;
                        break;
                    }
                    case P_0F38:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_38;
                        break;
                    }
                    case P_0F3A:
                    {
                        __opc = VexOpcode.VEX_OPCODE_0F_3A;
                        break;
                    }
                    default:
                    {
                        __isSimd = false;
                        break;
                    }
                }
            }

            if (__isSimd)
            {
                int __pre;
                AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, __asm.___target);
                int __curPrefix = __size.___sizePrefix | this.___prefix1;
                switch (__curPrefix)
                {
                    case 0x66:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_66;
                        break;
                    }
                    case 0xF2:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F2;
                        break;
                    }
                    case 0xF3:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_F3;
                        break;
                    }
                    default:
                    {
                        __pre = VexSimdPrefix.VEX_SIMD_NONE;
                        break;
                    }
                }
                if (__noNds)
                {
                    __asm.simdPrefix(__dst, Register.None, __src, __pre, __opc, __attributes);
                }
                else
                {
                    __asm.simdPrefix(__dst, __dst, __src, __pre, __opc, __attributes);
                }
                __asm.emitByte(this.___op);
                __asm.emitOperandHelper(__dst, __src, immediateSize(__size));
                emitImmediate(__asm, __size, __imm);
            }
            else
            {
                emitOpcode(__asm, __size, getRXB(__dst, __src), __dst.encoding, 0);
                __asm.emitOperandHelper(__dst, __src, immediateSize(__size));
                emitImmediate(__asm, __size, __imm);
            }
        }
    }

    // @class AMD64Assembler.SSEOp
    public static final class SSEOp extends AMD64RMOp
    {
        // @defs
        public static final SSEOp
            CVTSI2SS  = new SSEOp("CVTSI2SS",  0xF3, P_0F, 0x2A, OpAssertion.IntToFloatAssertion),
            CVTSI2SD  = new SSEOp("CVTSI2SS",  0xF2, P_0F, 0x2A, OpAssertion.IntToFloatAssertion),
            CVTTSS2SI = new SSEOp("CVTTSS2SI", 0xF3, P_0F, 0x2C, OpAssertion.FloatToIntAssertion),
            CVTTSD2SI = new SSEOp("CVTTSD2SI", 0xF2, P_0F, 0x2C, OpAssertion.FloatToIntAssertion),
            UCOMIS    = new SSEOp("UCOMIS",          P_0F, 0x2E, OpAssertion.PackedFloatAssertion),
            SQRT      = new SSEOp("SQRT",            P_0F, 0x51),
            AND       = new SSEOp("AND",             P_0F, 0x54, OpAssertion.PackedFloatAssertion),
            ANDN      = new SSEOp("ANDN",            P_0F, 0x55, OpAssertion.PackedFloatAssertion),
            OR        = new SSEOp("OR",              P_0F, 0x56, OpAssertion.PackedFloatAssertion),
            XOR       = new SSEOp("XOR",             P_0F, 0x57, OpAssertion.PackedFloatAssertion),
            ADD       = new SSEOp("ADD",             P_0F, 0x58),
            MUL       = new SSEOp("MUL",             P_0F, 0x59),
            CVTSS2SD  = new SSEOp("CVTSS2SD",        P_0F, 0x5A, OpAssertion.SingleAssertion),
            CVTSD2SS  = new SSEOp("CVTSD2SS",        P_0F, 0x5A, OpAssertion.DoubleAssertion),
            SUB       = new SSEOp("SUB",             P_0F, 0x5C),
            MIN       = new SSEOp("MIN",             P_0F, 0x5D),
            DIV       = new SSEOp("DIV",             P_0F, 0x5E),
            MAX       = new SSEOp("MAX",             P_0F, 0x5F);

        // @cons
        protected SSEOp(String __opcode, int __prefix, int __op)
        {
            this(__opcode, __prefix, __op, OpAssertion.FloatAssertion);
        }

        // @cons
        protected SSEOp(String __opcode, int __prefix, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __prefix, __op, __assertion);
        }

        // @cons
        protected SSEOp(String __opcode, int __mandatoryPrefix, int __prefix, int __op, OpAssertion __assertion)
        {
            super(__opcode, __mandatoryPrefix, __prefix, __op, __assertion, CPUFeature.SSE2);
        }
    }

    // @class AMD64Assembler.AVXOp
    public static final class AVXOp extends AMD64RRMOp
    {
        // @defs
        public static final AVXOp
            AND  = new AVXOp("AND",  P_0F, 0x54, OpAssertion.PackedFloatAssertion),
            ANDN = new AVXOp("ANDN", P_0F, 0x55, OpAssertion.PackedFloatAssertion),
            OR   = new AVXOp("OR",   P_0F, 0x56, OpAssertion.PackedFloatAssertion),
            XOR  = new AVXOp("XOR",  P_0F, 0x57, OpAssertion.PackedFloatAssertion),
            ADD  = new AVXOp("ADD",  P_0F, 0x58),
            MUL  = new AVXOp("MUL",  P_0F, 0x59),
            SUB  = new AVXOp("SUB",  P_0F, 0x5C),
            MIN  = new AVXOp("MIN",  P_0F, 0x5D),
            DIV  = new AVXOp("DIV",  P_0F, 0x5E),
            MAX  = new AVXOp("MAX",  P_0F, 0x5F);

        // @cons
        protected AVXOp(String __opcode, int __prefix, int __op)
        {
            this(__opcode, __prefix, __op, OpAssertion.FloatAssertion);
        }

        // @cons
        protected AVXOp(String __opcode, int __prefix, int __op, OpAssertion __assertion)
        {
            this(__opcode, 0, __prefix, __op, __assertion);
        }

        // @cons
        protected AVXOp(String __opcode, int __mandatoryPrefix, int __prefix, int __op, OpAssertion __assertion)
        {
            super(__opcode, __mandatoryPrefix, __prefix, __op, __assertion, CPUFeature.AVX);
        }
    }

    ///
    // Arithmetic operation with operand order of RM, MR or MI.
    ///
    // @class AMD64Assembler.AMD64BinaryArithmetic
    public static final class AMD64BinaryArithmetic
    {
        // @defs
        public static final AMD64BinaryArithmetic
            ADD = new AMD64BinaryArithmetic("ADD", 0),
            OR  = new AMD64BinaryArithmetic("OR",  1),
            ADC = new AMD64BinaryArithmetic("ADC", 2),
            SBB = new AMD64BinaryArithmetic("SBB", 3),
            AND = new AMD64BinaryArithmetic("AND", 4),
            SUB = new AMD64BinaryArithmetic("SUB", 5),
            XOR = new AMD64BinaryArithmetic("XOR", 6),
            CMP = new AMD64BinaryArithmetic("CMP", 7);

        // @field
        private final AMD64MIOp ___byteImmOp;
        // @field
        private final AMD64MROp ___byteMrOp;
        // @field
        private final AMD64RMOp ___byteRmOp;

        // @field
        private final AMD64MIOp ___immOp;
        // @field
        private final AMD64MIOp ___immSxOp;
        // @field
        private final AMD64MROp ___mrOp;
        // @field
        private final AMD64RMOp ___rmOp;

        // @cons
        private AMD64BinaryArithmetic(String __opcode, int __code)
        {
            super();
            int __baseOp = __code << 3;

            this.___byteImmOp = new AMD64MIOp(__opcode, true, 0, 0x80, __code, OpAssertion.ByteAssertion);
            this.___byteMrOp = new AMD64MROp(__opcode, 0, __baseOp, OpAssertion.ByteAssertion);
            this.___byteRmOp = new AMD64RMOp(__opcode, 0, __baseOp | 0x02, OpAssertion.ByteAssertion);

            this.___immOp = new AMD64MIOp(__opcode, false, 0, 0x81, __code, OpAssertion.WordOrLargerAssertion);
            this.___immSxOp = new AMD64MIOp(__opcode, true, 0, 0x83, __code, OpAssertion.WordOrLargerAssertion);
            this.___mrOp = new AMD64MROp(__opcode, 0, __baseOp | 0x01, OpAssertion.WordOrLargerAssertion);
            this.___rmOp = new AMD64RMOp(__opcode, 0, __baseOp | 0x03, OpAssertion.WordOrLargerAssertion);
        }

        public AMD64MIOp getMIOpcode(OperandSize __size, boolean __sx)
        {
            if (__size == OperandSize.BYTE)
            {
                return this.___byteImmOp;
            }
            else if (__sx)
            {
                return this.___immSxOp;
            }
            else
            {
                return this.___immOp;
            }
        }

        public AMD64MROp getMROpcode(OperandSize __size)
        {
            if (__size == OperandSize.BYTE)
            {
                return this.___byteMrOp;
            }
            else
            {
                return this.___mrOp;
            }
        }

        public AMD64RMOp getRMOpcode(OperandSize __size)
        {
            if (__size == OperandSize.BYTE)
            {
                return this.___byteRmOp;
            }
            else
            {
                return this.___rmOp;
            }
        }
    }

    ///
    // Shift operation with operand order of M1, MC or MI.
    ///
    // @class AMD64Assembler.AMD64Shift
    public static final class AMD64Shift
    {
        // @defs
        public static final AMD64Shift
            ROL = new AMD64Shift("ROL", 0),
            ROR = new AMD64Shift("ROR", 1),
            RCL = new AMD64Shift("RCL", 2),
            RCR = new AMD64Shift("RCR", 3),
            SHL = new AMD64Shift("SHL", 4),
            SHR = new AMD64Shift("SHR", 5),
            SAR = new AMD64Shift("SAR", 7);

        // @field
        public final AMD64MOp ___m1Op;
        // @field
        public final AMD64MOp ___mcOp;
        // @field
        public final AMD64MIOp ___miOp;

        // @cons
        private AMD64Shift(String __opcode, int __code)
        {
            super();
            this.___m1Op = new AMD64MOp(__opcode, 0, 0xD1, __code, OpAssertion.WordOrLargerAssertion);
            this.___mcOp = new AMD64MOp(__opcode, 0, 0xD3, __code, OpAssertion.WordOrLargerAssertion);
            this.___miOp = new AMD64MIOp(__opcode, true, 0, 0xC1, __code, OpAssertion.WordOrLargerAssertion);
        }
    }

    public final void addl(AMD64Address __dst, int __imm32)
    {
        AMD64BinaryArithmetic.ADD.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void addl(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.ADD.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void addl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.ADD.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void addpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x58);
        emitByte(0xC0 | __encode);
    }

    public final void addpd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x58);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void addsd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x58);
        emitByte(0xC0 | __encode);
    }

    public final void addsd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x58);
        emitOperandHelper(__dst, __src, 0);
    }

    private void addrNop4()
    {
        // 4 bytes: NOP DWORD PTR [EAX+0]
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x40); // emitRm(cbuf, 0x1, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    private void addrNop5()
    {
        // 5 bytes: NOP DWORD PTR [EAX+EAX*0+0] 8-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x44); // emitRm(cbuf, 0x1, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitByte(0); // 8-bits offset (1 byte)
    }

    private void addrNop7()
    {
        // 7 bytes: NOP DWORD PTR [EAX+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x80); // emitRm(cbuf, 0x2, EAXEnc, EAXEnc);
        emitInt(0); // 32-bits offset (4 bytes)
    }

    private void addrNop8()
    {
        // 8 bytes: NOP DWORD PTR [EAX+EAX*0+0] 32-bits offset
        emitByte(0x0F);
        emitByte(0x1F);
        emitByte(0x84); // emitRm(cbuf, 0x2, EAXEnc, 0x4);
        emitByte(0x00); // emitRm(cbuf, 0x0, EAXEnc, EAXEnc);
        emitInt(0); // 32-bits offset (4 bytes)
    }

    public final void andl(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.AND.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void andl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.AND.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void andpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x54);
        emitByte(0xC0 | __encode);
    }

    public final void andpd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x54);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void bsfq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding(), __src.encoding());
        emitByte(0x0F);
        emitByte(0xBC);
        emitByte(0xC0 | __encode);
    }

    public final void bsrl(Register __dst, Register __src)
    {
        int __encode = prefixAndEncode(__dst.encoding(), __src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | __encode);
    }

    public final void bswapl(Register __reg)
    {
        int __encode = prefixAndEncode(__reg.encoding);
        emitByte(0x0F);
        emitByte(0xC8 | __encode);
    }

    public final void cdql()
    {
        emitByte(0x99);
    }

    public final void cmovl(ConditionFlag __cc, Register __dst, Register __src)
    {
        int __encode = prefixAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x0F);
        emitByte(0x40 | __cc.getValue());
        emitByte(0xC0 | __encode);
    }

    public final void cmovl(ConditionFlag __cc, Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x0F);
        emitByte(0x40 | __cc.getValue());
        emitOperandHelper(__dst, __src, 0);
    }

    public final void cmpl(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.CMP.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void cmpl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.CMP.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void cmpl(Register __dst, AMD64Address __src)
    {
        AMD64BinaryArithmetic.CMP.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void cmpl(AMD64Address __dst, int __imm32)
    {
        AMD64BinaryArithmetic.CMP.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    // The 32-bit cmpxchg compares the value at adr with the contents of X86.rax,
    // and stores reg into adr if so; otherwise, the value at adr is loaded into X86.rax.
    // The ZF is set if the compared values were equal, and cleared otherwise.
    public final void cmpxchgl(Register __reg, AMD64Address __adr) // cmpxchg
    {
        prefix(__adr, __reg);
        emitByte(0x0F);
        emitByte(0xB1);
        emitOperandHelper(__reg, __adr, 0);
    }

    public final void cvtsi2sdl(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x2A);
        emitByte(0xC0 | __encode);
    }

    public final void cvttsd2sil(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x2C);
        emitByte(0xC0 | __encode);
    }

    protected final void decl(AMD64Address __dst)
    {
        prefix(__dst);
        emitByte(0xFF);
        emitOperandHelper(1, __dst, 0);
    }

    public final void divsd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x5E);
        emitByte(0xC0 | __encode);
    }

    public final void evmovdquq(Register __dst, AMD64Address __src, int __vectorLen)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(__vectorLen, true, false, false, true, this.___target);
        __attributes.setAddressAttributes(EvexTupleType.EVEX_FVM, EvexInputSizeInBits.EVEX_NObit);
        __attributes.setIsEvexInstruction();
        vexPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6F);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void evpcmpeqb(Register __kdst, Register __nds, AMD64Address __src, int __vectorLen)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(__vectorLen, false, false, true, false, this.___target);
        __attributes.setIsEvexInstruction();
        __attributes.setAddressAttributes(EvexTupleType.EVEX_FVM, EvexInputSizeInBits.EVEX_NObit);
        vexPrefix(__src, __nds, __kdst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x74);
        emitOperandHelper(__kdst, __src, 0);
    }

    public final void hlt()
    {
        emitByte(0xF4);
    }

    public final void imull(Register __dst, Register __src, int __value)
    {
        if (NumUtil.isByte(__value))
        {
            AMD64RMIOp.IMUL_SX.emit(this, OperandSize.DWORD, __dst, __src, __value);
        }
        else
        {
            AMD64RMIOp.IMUL.emit(this, OperandSize.DWORD, __dst, __src, __value);
        }
    }

    protected final void incl(AMD64Address __dst)
    {
        prefix(__dst);
        emitByte(0xFF);
        emitOperandHelper(0, __dst, 0);
    }

    public void jcc(ConditionFlag __cc, int __jumpTarget, boolean __forceDisp32)
    {
        int __shortSize = 2;
        int __longSize = 6;
        long __disp = __jumpTarget - position();
        if (!__forceDisp32 && NumUtil.isByte(__disp - __shortSize))
        {
            // 0111 tttn #8-bit disp
            emitByte(0x70 | __cc.getValue());
            emitByte((int) ((__disp - __shortSize) & 0xFF));
        }
        else
        {
            // 0000 1111 1000 tttn #32-bit disp
            emitByte(0x0F);
            emitByte(0x80 | __cc.getValue());
            emitInt((int) (__disp - __longSize));
        }
    }

    public final void jcc(ConditionFlag __cc, Label __l)
    {
        if (__l.isBound())
        {
            jcc(__cc, __l.position(), false);
        }
        else
        {
            // note: could eliminate cond. jumps to this jump if condition is the same however, seems to be rather unlikely case
            // note: use jccb() if label to be bound is very close to get an 8-bit displacement
            __l.addPatchAt(position());
            emitByte(0x0F);
            emitByte(0x80 | __cc.getValue());
            emitInt(0);
        }
    }

    public final void jccb(ConditionFlag __cc, Label __l)
    {
        if (__l.isBound())
        {
            int __shortSize = 2;
            int __entry = __l.position();
            long __disp = __entry - position();
            // 0111 tttn #8-bit disp
            emitByte(0x70 | __cc.getValue());
            emitByte((int) ((__disp - __shortSize) & 0xFF));
        }
        else
        {
            __l.addPatchAt(position());
            emitByte(0x70 | __cc.getValue());
            emitByte(0);
        }
    }

    public final void jmp(int __jumpTarget, boolean __forceDisp32)
    {
        int __shortSize = 2;
        int __longSize = 5;
        long __disp = __jumpTarget - position();
        if (!__forceDisp32 && NumUtil.isByte(__disp - __shortSize))
        {
            emitByte(0xEB);
            emitByte((int) ((__disp - __shortSize) & 0xFF));
        }
        else
        {
            emitByte(0xE9);
            emitInt((int) (__disp - __longSize));
        }
    }

    @Override
    public final void jmp(Label __l)
    {
        if (__l.isBound())
        {
            jmp(__l.position(), false);
        }
        else
        {
            // By default, forward jumps are always 32-bit displacements, since we can't yet know where the label will be bound.
            // If you're sure that the forward jump will not run beyond 256 bytes, use jmpb to force an 8-bit displacement.
            __l.addPatchAt(position());
            emitByte(0xE9);
            emitInt(0);
        }
    }

    public final void jmp(Register __entry)
    {
        int __encode = prefixAndEncode(__entry.encoding);
        emitByte(0xFF);
        emitByte(0xE0 | __encode);
    }

    public final void jmp(AMD64Address __adr)
    {
        prefix(__adr);
        emitByte(0xFF);
        emitOperandHelper(AMD64.rsp, __adr, 0);
    }

    public final void jmpb(Label __l)
    {
        if (__l.isBound())
        {
            int __shortSize = 2;
            int __entry = __l.position();
            long __offs = __entry - position();
            emitByte(0xEB);
            emitByte((int) ((__offs - __shortSize) & 0xFF));
        }
        else
        {
            __l.addPatchAt(position());
            emitByte(0xEB);
            emitByte(0);
        }
    }

    // This instruction produces ZF or CF flags.
    public final void kortestql(Register __src1, Register __src2)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, true, true, false, this.___target);
        int __encode = vexPrefixAndEncode(__src1, Register.None, __src2, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x98);
        emitByte(0xC0 | __encode);
    }

    public final void kmovql(Register __dst, Register __src)
    {
        if (__src.getRegisterCategory().equals(AMD64.MASK))
        {
            // kmovql(KRegister dst, KRegister src)
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, true, true, false, this.___target);
            int __encode = vexPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x90);
            emitByte(0xC0 | __encode);
        }
        else
        {
            // kmovql(KRegister dst, Register src)
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, true, true, false, this.___target);
            int __encode = vexPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x92);
            emitByte(0xC0 | __encode);
        }
    }

    public final void lead(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x8D);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void leaq(Register __dst, AMD64Address __src)
    {
        prefixq(__src, __dst);
        emitByte(0x8D);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void leave()
    {
        emitByte(0xC9);
    }

    public final void lock()
    {
        emitByte(0xF0);
    }

    public final void movapd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x28);
        emitByte(0xC0 | __encode);
    }

    public final void movaps(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x28);
        emitByte(0xC0 | __encode);
    }

    public final void movb(AMD64Address __dst, int __imm8)
    {
        prefix(__dst);
        emitByte(0xC6);
        emitOperandHelper(0, __dst, 1);
        emitByte(__imm8);
    }

    public final void movb(AMD64Address __dst, Register __src)
    {
        prefix(__dst, __src, true);
        emitByte(0x88);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void movl(Register __dst, int __imm32)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xB8 | __encode);
        emitInt(__imm32);
    }

    public final void movl(Register __dst, Register __src)
    {
        int __encode = prefixAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x8B);
        emitByte(0xC0 | __encode);
    }

    public final void movl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x8B);
        emitOperandHelper(__dst, __src, 0);
    }

    ///
    // @param wide use 4 byte encoding for displacements that would normally fit in a byte
    ///
    public final void movl(Register __dst, AMD64Address __src, boolean __wide)
    {
        prefix(__src, __dst);
        emitByte(0x8B);
        emitOperandHelper(__dst, __src, __wide, 0);
    }

    public final void movl(AMD64Address __dst, int __imm32)
    {
        prefix(__dst);
        emitByte(0xC7);
        emitOperandHelper(0, __dst, 4);
        emitInt(__imm32);
    }

    public final void movl(AMD64Address __dst, Register __src)
    {
        prefix(__dst, __src);
        emitByte(0x89);
        emitOperandHelper(__src, __dst, 0);
    }

    ///
    // New CPUs require use of movsd and movss to avoid partial register stall when loading from memory.
    // But for old Opteron use movlpd instead of movsd. The selection is done in
    // {@link AMD64MacroAssembler#movdbl(Register, AMD64Address)} and
    // {@link AMD64MacroAssembler#movflt(Register, Register)}.
    ///
    public final void movlpd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x12);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movlhps(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __src, __src, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x16);
        emitByte(0xC0 | __encode);
    }

    public final void movq(Register __dst, AMD64Address __src)
    {
        movq(__dst, __src, false);
    }

    public final void movq(Register __dst, AMD64Address __src, boolean __wide)
    {
        if (__dst.getRegisterCategory().equals(AMD64.XMM))
        {
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, __wide, false, false, false, this.___target);
            simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x7E);
            emitOperandHelper(__dst, __src, __wide, 0);
        }
        else
        {
            // gpr version of movq
            prefixq(__src, __dst);
            emitByte(0x8B);
            emitOperandHelper(__dst, __src, __wide, 0);
        }
    }

    public final void movq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x8B);
        emitByte(0xC0 | __encode);
    }

    public final void movq(AMD64Address __dst, Register __src)
    {
        if (__src.getRegisterCategory().equals(AMD64.XMM))
        {
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
            simdPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0xD6);
            emitOperandHelper(__src, __dst, 0);
        }
        else
        {
            // gpr version of movq
            prefixq(__dst, __src);
            emitByte(0x89);
            emitOperandHelper(__src, __dst, 0);
        }
    }

    public final void movsbl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x0F);
        emitByte(0xBE);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movsbl(Register __dst, Register __src)
    {
        int __encode = prefixAndEncode(__dst.encoding, false, __src.encoding, true);
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | __encode);
    }

    public final void movsbq(Register __dst, AMD64Address __src)
    {
        prefixq(__src, __dst);
        emitByte(0x0F);
        emitByte(0xBE);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movsbq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x0F);
        emitByte(0xBE);
        emitByte(0xC0 | __encode);
    }

    public final void movsd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x10);
        emitByte(0xC0 | __encode);
    }

    public final void movsd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x10);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movsd(AMD64Address __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x11);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void movss(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x10);
        emitByte(0xC0 | __encode);
    }

    public final void movss(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x10);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movss(AMD64Address __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x11);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void mulpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x59);
        emitByte(0xC0 | __encode);
    }

    public final void mulpd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x59);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void mulsd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x59);
        emitByte(0xC0 | __encode);
    }

    public final void mulsd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x59);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void mulss(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x59);
        emitByte(0xC0 | __encode);
    }

    public final void movswl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x0F);
        emitByte(0xBF);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movw(AMD64Address __dst, int __imm16)
    {
        emitByte(0x66); // switch to 16-bit mode
        prefix(__dst);
        emitByte(0xC7);
        emitOperandHelper(0, __dst, 2);
        emitShort(__imm16);
    }

    public final void movw(AMD64Address __dst, Register __src)
    {
        emitByte(0x66);
        prefix(__dst, __src);
        emitByte(0x89);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void movzbl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x0F);
        emitByte(0xB6);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movzbl(Register __dst, Register __src)
    {
        AMD64RMOp.MOVZXB.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void movzbq(Register __dst, Register __src)
    {
        AMD64RMOp.MOVZXB.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void movzwl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x0F);
        emitByte(0xB7);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void negl(Register __dst)
    {
        AMD64MOp.NEG.emit(this, OperandSize.DWORD, __dst);
    }

    public final void notl(Register __dst)
    {
        AMD64MOp.NOT.emit(this, OperandSize.DWORD, __dst);
    }

    public final void notq(Register __dst)
    {
        AMD64MOp.NOT.emit(this, OperandSize.QWORD, __dst);
    }

    @Override
    public final void ensureUniquePC()
    {
        nop();
    }

    public final void nop()
    {
        nop(1);
    }

    public void nop(int __count)
    {
        int __i = __count;
        if (AMD64AsmOptions.UseNormalNop)
        {
            // The fancy nops aren't currently recognized by debuggers making it a pain to disassemble code while debugging.
            // If assert are on clearly speed is not an issue so simply use the single byte traditional nop to do alignment.
            for ( ; __i > 0; __i--)
            {
                emitByte(0x90);
            }
            return;
        }

        if (AMD64AsmOptions.UseAddressNop)
        {
            // Using multi-bytes nops "0x0F 0x1F [Address]" for AMD.
            //
            // 1: 0x90
            // 2: 0x66 0x90
            // 3: 0x66 0x66 0x90 (don't use "0x0F 0x1F 0x00" - need patching safe padding)
            // 4: 0x0F 0x1F 0x40 0x00
            // 5: 0x0F 0x1F 0x44 0x00 0x00
            // 6: 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 7: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 8: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 9: 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 10: 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            // 11: 0x66 0x66 0x66 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            //
            // The rest coding is AMD specific - use consecutive Address nops.
            //
            // 12: 0x66 0x0F 0x1F 0x44 0x00 0x00 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 13: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00 0x66 0x0F 0x1F 0x44 0x00 0x00
            // 14: 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 15: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x0F 0x1F 0x80 0x00 0x00 0x00 0x00
            // 16: 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00 0x0F 0x1F 0x84 0x00 0x00 0x00 0x00 0x00
            //
            // Size prefixes (0x66) are added for larger sizes.
            while (__i >= 22)
            {
                __i -= 11;
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                emitByte(0x66); // size prefix
                addrNop8();
            }
            // Generate first nop for size between 21-12.
            switch (__i)
            {
                case 21:
                    __i -= 11;
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 20:
                case 19:
                    __i -= 10;
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 18:
                case 17:
                    __i -= 9;
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 16:
                case 15:
                    __i -= 8;
                    addrNop8();
                    break;
                case 14:
                case 13:
                    __i -= 7;
                    addrNop7();
                    break;
                case 12:
                    __i -= 6;
                    emitByte(0x66); // size prefix
                    addrNop5();
                    break;
            }

            // Generate second nop for size between 11-1.
            switch (__i)
            {
                case 11:
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 10:
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 9:
                    emitByte(0x66); // size prefix
                    addrNop8();
                    break;
                case 8:
                    addrNop8();
                    break;
                case 7:
                    addrNop7();
                    break;
                case 6:
                    emitByte(0x66); // size prefix
                    addrNop5();
                    break;
                case 5:
                    addrNop5();
                    break;
                case 4:
                    addrNop4();
                    break;
                case 3:
                    // Don't use "0x0F 0x1F 0x00" - need patching safe padding.
                    emitByte(0x66); // size prefix
                    emitByte(0x66); // size prefix
                    emitByte(0x90); // nop
                    break;
                case 2:
                    emitByte(0x66); // size prefix
                    emitByte(0x90); // nop
                    break;
                case 1:
                    emitByte(0x90); // nop
                    break;
            }
            return;
        }

        // Using nops with size prefixes "0x66 0x90".
        // From AMD Optimization Guide:
        // 1: 0x90
        // 2: 0x66 0x90
        // 3: 0x66 0x66 0x90
        // 4: 0x66 0x66 0x66 0x90
        // 5: 0x66 0x66 0x90 0x66 0x90
        // 6: 0x66 0x66 0x90 0x66 0x66 0x90
        // 7: 0x66 0x66 0x66 0x90 0x66 0x66 0x90
        // 8: 0x66 0x66 0x66 0x90 0x66 0x66 0x66 0x90
        // 9: 0x66 0x66 0x90 0x66 0x66 0x90 0x66 0x66 0x90
        // 10: 0x66 0x66 0x66 0x90 0x66 0x66 0x90 0x66 0x66 0x90
        while (__i > 12)
        {
            __i -= 4;
            emitByte(0x66); // size prefix
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90); // nop
        }
        // 1 - 12 nops
        if (__i > 8)
        {
            if (__i > 9)
            {
                __i -= 1;
                emitByte(0x66);
            }
            __i -= 3;
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90);
        }
        // 1 - 8 nops
        if (__i > 4)
        {
            if (__i > 6)
            {
                __i -= 1;
                emitByte(0x66);
            }
            __i -= 3;
            emitByte(0x66);
            emitByte(0x66);
            emitByte(0x90);
        }
        switch (__i)
        {
            case 4:
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 3:
                emitByte(0x66);
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 2:
                emitByte(0x66);
                emitByte(0x90);
                break;
            case 1:
                emitByte(0x90);
                break;
        }
    }

    public final void orl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.OR.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void orl(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.OR.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void pop(Register __dst)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0x58 | __encode);
    }

    public void popfq()
    {
        emitByte(0x9D);
    }

    public final void ptest(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_38, __attributes);
        emitByte(0x17);
        emitByte(0xC0 | __encode);
    }

    public final void vptest(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_256bit, false, false, false, false, this.___target);
        int __encode = vexPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_38, __attributes);
        emitByte(0x17);
        emitByte(0xC0 | __encode);
    }

    public final void pcmpestri(Register __dst, AMD64Address __src, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_3A, __attributes);
        emitByte(0x61);
        emitOperandHelper(__dst, __src, 0);
        emitByte(__imm8);
    }

    public final void pcmpestri(Register __dst, Register __src, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_3A, __attributes);
        emitByte(0x61);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void pmovzxbw(Register __dst, AMD64Address __src)
    {
        // XXX legacy_mode should be: _legacy_mode_bw
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, true, false, this.___target);
        __attributes.setAddressAttributes(EvexTupleType.EVEX_HVM, EvexInputSizeInBits.EVEX_NObit);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_38, __attributes);
        emitByte(0x30);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void vpmovzxbw(Register __dst, AMD64Address __src, int __vectorLen)
    {
        // XXX legacy_mode should be: _legacy_mode_bw
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(__vectorLen, false, false, true, false, this.___target);
        __attributes.setAddressAttributes(EvexTupleType.EVEX_HVM, EvexInputSizeInBits.EVEX_NObit);
        vexPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F_38, __attributes);
        emitByte(0x30);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void push(Register __src)
    {
        int __encode = prefixAndEncode(__src.encoding);
        emitByte(0x50 | __encode);
    }

    public void pushfq()
    {
        emitByte(0x9c);
    }

    public final void paddd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xFE);
        emitByte(0xC0 | __encode);
    }

    public final void paddq(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xD4);
        emitByte(0xC0 | __encode);
    }

    public final void pextrw(Register __dst, Register __src, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xC5);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void pinsrw(Register __dst, Register __src, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xC4);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void por(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xEB);
        emitByte(0xC0 | __encode);
    }

    public final void pand(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xDB);
        emitByte(0xC0 | __encode);
    }

    public final void pxor(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xEF);
        emitByte(0xC0 | __encode);
    }

    public final void vpxor(Register __dst, Register __nds, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_256bit, false, false, false, false, this.___target);
        int __encode = vexPrefixAndEncode(__dst, __nds, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xEF);
        emitByte(0xC0 | __encode);
    }

    public final void vpxor(Register __dst, Register __nds, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_256bit, false, false, false, true, this.___target);
        __attributes.setAddressAttributes(EvexTupleType.EVEX_FV, EvexInputSizeInBits.EVEX_32bit);
        vexPrefix(__src, __nds, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xEF);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void pslld(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        // XMM6 is for /6 encoding: 66 0F 72 /6 ib
        int __encode = simdPrefixAndEncode(AMD64.xmm6, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x72);
        emitByte(0xC0 | __encode);
        emitByte(__imm8 & 0xFF);
    }

    public final void psllq(Register __dst, Register __shift)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __shift, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xF3);
        emitByte(0xC0 | __encode);
    }

    public final void psllq(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        // XMM6 is for /6 encoding: 66 0F 73 /6 ib
        int __encode = simdPrefixAndEncode(AMD64.xmm6, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x73);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void psrad(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        // XMM4 is for /2 encoding: 66 0F 72 /4 ib
        int __encode = simdPrefixAndEncode(AMD64.xmm4, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x72);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void psrld(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        // XMM2 is for /2 encoding: 66 0F 72 /2 ib
        int __encode = simdPrefixAndEncode(AMD64.xmm2, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x72);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void psrlq(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        // XMM2 is for /2 encoding: 66 0F 73 /2 ib
        int __encode = simdPrefixAndEncode(AMD64.xmm2, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x73);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void psrldq(Register __dst, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(AMD64.xmm3, __dst, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x73);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void pshufd(Register __dst, Register __src, int __imm8)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x70);
        emitByte(0xC0 | __encode);
        emitByte(__imm8);
    }

    public final void psubd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xFA);
        emitByte(0xC0 | __encode);
    }

    public final void rcpps(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, true, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x53);
        emitByte(0xC0 | __encode);
    }

    public final void ret(int __imm16)
    {
        if (__imm16 == 0)
        {
            emitByte(0xC3);
        }
        else
        {
            emitByte(0xC2);
            emitShort(__imm16);
        }
    }

    public final void sarl(Register __dst, int __imm8)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        if (__imm8 == 1)
        {
            emitByte(0xD1);
            emitByte(0xF8 | __encode);
        }
        else
        {
            emitByte(0xC1);
            emitByte(0xF8 | __encode);
            emitByte(__imm8);
        }
    }

    public final void shll(Register __dst, int __imm8)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        if (__imm8 == 1)
        {
            emitByte(0xD1);
            emitByte(0xE0 | __encode);
        }
        else
        {
            emitByte(0xC1);
            emitByte(0xE0 | __encode);
            emitByte(__imm8);
        }
    }

    public final void shll(Register __dst)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xD3);
        emitByte(0xE0 | __encode);
    }

    public final void shrl(Register __dst, int __imm8)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xC1);
        emitByte(0xE8 | __encode);
        emitByte(__imm8);
    }

    public final void shrl(Register __dst)
    {
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xD3);
        emitByte(0xE8 | __encode);
    }

    public final void subl(AMD64Address __dst, int __imm32)
    {
        AMD64BinaryArithmetic.SUB.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void subl(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.SUB.getMIOpcode(OperandSize.DWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.DWORD, __dst, __imm32);
    }

    public final void subl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.SUB.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void subpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x5C);
        emitByte(0xC0 | __encode);
    }

    public final void subsd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x5C);
        emitByte(0xC0 | __encode);
    }

    public final void subsd(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x5C);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void testl(Register __dst, int __imm32)
    {
        // not using emitArith because test
        // doesn't support sign-extension of
        // 8bit operands
        int __encode = __dst.encoding;
        if (__encode == 0)
        {
            emitByte(0xA9);
        }
        else
        {
            __encode = prefixAndEncode(__encode);
            emitByte(0xF7);
            emitByte(0xC0 | __encode);
        }
        emitInt(__imm32);
    }

    public final void testl(Register __dst, Register __src)
    {
        int __encode = prefixAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x85);
        emitByte(0xC0 | __encode);
    }

    public final void testl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x85);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void unpckhpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x15);
        emitByte(0xC0 | __encode);
    }

    public final void unpcklpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x14);
        emitByte(0xC0 | __encode);
    }

    public final void xorl(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.XOR.___rmOp.emit(this, OperandSize.DWORD, __dst, __src);
    }

    public final void xorpd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x57);
        emitByte(0xC0 | __encode);
    }

    public final void xorps(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x57);
        emitByte(0xC0 | __encode);
    }

    protected final void decl(Register __dst)
    {
        // Use two-byte form (one-byte form is a REX prefix in 64-bit mode).
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xFF);
        emitByte(0xC8 | __encode);
    }

    protected final void incl(Register __dst)
    {
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode).
        int __encode = prefixAndEncode(__dst.encoding);
        emitByte(0xFF);
        emitByte(0xC0 | __encode);
    }

    private int prefixAndEncode(int __regEnc)
    {
        return prefixAndEncode(__regEnc, false);
    }

    private int prefixAndEncode(int __regEnc, boolean __byteinst)
    {
        if (__regEnc >= 8)
        {
            emitByte(Prefix.REXB);
            return __regEnc - 8;
        }
        else if (__byteinst && __regEnc >= 4)
        {
            emitByte(Prefix.REX);
        }
        return __regEnc;
    }

    private int prefixqAndEncode(int __regEnc)
    {
        if (__regEnc < 8)
        {
            emitByte(Prefix.REXW);
            return __regEnc;
        }
        else
        {
            emitByte(Prefix.REXWB);
            return __regEnc - 8;
        }
    }

    private int prefixAndEncode(int __dstEnc, int __srcEnc)
    {
        return prefixAndEncode(__dstEnc, false, __srcEnc, false);
    }

    private int prefixAndEncode(int __dstEncoding, boolean __dstIsByte, int __srcEncoding, boolean __srcIsByte)
    {
        int __srcEnc = __srcEncoding;
        int __dstEnc = __dstEncoding;
        if (__dstEnc < 8)
        {
            if (__srcEnc >= 8)
            {
                emitByte(Prefix.REXB);
                __srcEnc -= 8;
            }
            else if ((__srcIsByte && __srcEnc >= 4) || (__dstIsByte && __dstEnc >= 4))
            {
                emitByte(Prefix.REX);
            }
        }
        else
        {
            if (__srcEnc < 8)
            {
                emitByte(Prefix.REXR);
            }
            else
            {
                emitByte(Prefix.REXRB);
                __srcEnc -= 8;
            }
            __dstEnc -= 8;
        }
        return __dstEnc << 3 | __srcEnc;
    }

    ///
    // Creates prefix and the encoding of the lower 6 bits of the ModRM-Byte. It emits an operand
    // prefix. If the given operands exceed 3 bits, the 4th bit is encoded in the prefix.
    //
    // @param regEncoding the encoding of the register part of the ModRM-Byte
    // @param rmEncoding the encoding of the r/m part of the ModRM-Byte
    // @return the lower 6 bits of the ModRM-Byte that should be emitted
    ///
    private int prefixqAndEncode(int __regEncoding, int __rmEncoding)
    {
        int __rmEnc = __rmEncoding;
        int __regEnc = __regEncoding;
        if (__regEnc < 8)
        {
            if (__rmEnc < 8)
            {
                emitByte(Prefix.REXW);
            }
            else
            {
                emitByte(Prefix.REXWB);
                __rmEnc -= 8;
            }
        }
        else
        {
            if (__rmEnc < 8)
            {
                emitByte(Prefix.REXWR);
            }
            else
            {
                emitByte(Prefix.REXWRB);
                __rmEnc -= 8;
            }
            __regEnc -= 8;
        }
        return __regEnc << 3 | __rmEnc;
    }

    private void vexPrefix(int __rxb, int __ndsEncoding, int __pre, int __opc, AMD64InstructionAttr __attributes)
    {
        int __vectorLen = __attributes.getVectorLen();
        boolean __vexW = __attributes.isRexVexW();
        boolean __isXorB = ((__rxb & 0x3) > 0);
        if (__isXorB || __vexW || (__opc == VexOpcode.VEX_OPCODE_0F_38) || (__opc == VexOpcode.VEX_OPCODE_0F_3A))
        {
            emitByte(Prefix.VEX_3BYTES);

            int __byte1 = (__rxb << 5);
            __byte1 = ((~__byte1) & 0xE0) | __opc;
            emitByte(__byte1);

            int __byte2 = ((~__ndsEncoding) & 0xf) << 3;
            __byte2 |= (__vexW ? VexPrefix.VEX_W : 0) | ((__vectorLen > 0) ? 4 : 0) | __pre;
            emitByte(__byte2);
        }
        else
        {
            emitByte(Prefix.VEX_2BYTES);

            int __byte1 = ((__rxb & 0x4) > 0) ? VexPrefix.VEX_R : 0;
            __byte1 = (~__byte1) & 0x80;
            __byte1 |= ((~__ndsEncoding) & 0xf) << 3;
            __byte1 |= ((__vectorLen > 0) ? 4 : 0) | __pre;
            emitByte(__byte1);
        }
    }

    private void vexPrefix(AMD64Address __adr, Register __nds, Register __src, int __pre, int __opc, AMD64InstructionAttr __attributes)
    {
        int __rxb = getRXB(__src, __adr);
        int __ndsEncoding = __nds.isValid() ? __nds.encoding : 0;
        vexPrefix(__rxb, __ndsEncoding, __pre, __opc, __attributes);
        setCurAttributes(__attributes);
    }

    private int vexPrefixAndEncode(Register __dst, Register __nds, Register __src, int __pre, int __opc, AMD64InstructionAttr __attributes)
    {
        int __rxb = getRXB(__dst, __src);
        int __ndsEncoding = __nds.isValid() ? __nds.encoding : 0;
        vexPrefix(__rxb, __ndsEncoding, __pre, __opc, __attributes);
        // return modrm byte components for operands
        return (((__dst.encoding & 7) << 3) | (__src.encoding & 7));
    }

    private void simdPrefix(Register __xreg, Register __nds, AMD64Address __adr, int __pre, int __opc, AMD64InstructionAttr __attributes)
    {
        if (supports(CPUFeature.AVX))
        {
            vexPrefix(__adr, __nds, __xreg, __pre, __opc, __attributes);
        }
        else
        {
            switch (__pre)
            {
                case VexSimdPrefix.VEX_SIMD_66:
                {
                    emitByte(0x66);
                    break;
                }
                case VexSimdPrefix.VEX_SIMD_F2:
                {
                    emitByte(0xF2);
                    break;
                }
                case VexSimdPrefix.VEX_SIMD_F3:
                {
                    emitByte(0xF3);
                    break;
                }
            }
            if (__attributes.isRexVexW())
            {
                prefixq(__adr, __xreg);
            }
            else
            {
                prefix(__adr, __xreg);
            }
            switch (__opc)
            {
                case VexOpcode.VEX_OPCODE_0F:
                {
                    emitByte(0x0F);
                    break;
                }
                case VexOpcode.VEX_OPCODE_0F_38:
                {
                    emitByte(0x0F);
                    emitByte(0x38);
                    break;
                }
                case VexOpcode.VEX_OPCODE_0F_3A:
                {
                    emitByte(0x0F);
                    emitByte(0x3A);
                    break;
                }
            }
        }
    }

    private int simdPrefixAndEncode(Register __dst, Register __nds, Register __src, int __pre, int __opc, AMD64InstructionAttr __attributes)
    {
        if (supports(CPUFeature.AVX))
        {
            return vexPrefixAndEncode(__dst, __nds, __src, __pre, __opc, __attributes);
        }
        else
        {
            switch (__pre)
            {
                case VexSimdPrefix.VEX_SIMD_66:
                {
                    emitByte(0x66);
                    break;
                }
                case VexSimdPrefix.VEX_SIMD_F2:
                {
                    emitByte(0xF2);
                    break;
                }
                case VexSimdPrefix.VEX_SIMD_F3:
                {
                    emitByte(0xF3);
                    break;
                }
            }
            int __encode;
            int __dstEncoding = __dst.encoding;
            int __srcEncoding = __src.encoding;
            if (__attributes.isRexVexW())
            {
                __encode = prefixqAndEncode(__dstEncoding, __srcEncoding);
            }
            else
            {
                __encode = prefixAndEncode(__dstEncoding, __srcEncoding);
            }
            switch (__opc)
            {
                case VexOpcode.VEX_OPCODE_0F:
                {
                    emitByte(0x0F);
                    break;
                }
                case VexOpcode.VEX_OPCODE_0F_38:
                {
                    emitByte(0x0F);
                    emitByte(0x38);
                    break;
                }
                case VexOpcode.VEX_OPCODE_0F_3A:
                {
                    emitByte(0x0F);
                    emitByte(0x3A);
                    break;
                }
            }
            return __encode;
        }
    }

    private static boolean needsRex(Register __reg)
    {
        return __reg.encoding >= MinEncodingNeedsRex;
    }

    private void prefix(AMD64Address __adr)
    {
        if (needsRex(__adr.getBase()))
        {
            if (needsRex(__adr.getIndex()))
            {
                emitByte(Prefix.REXXB);
            }
            else
            {
                emitByte(Prefix.REXB);
            }
        }
        else
        {
            if (needsRex(__adr.getIndex()))
            {
                emitByte(Prefix.REXX);
            }
        }
    }

    private void prefixq(AMD64Address __adr)
    {
        if (needsRex(__adr.getBase()))
        {
            if (needsRex(__adr.getIndex()))
            {
                emitByte(Prefix.REXWXB);
            }
            else
            {
                emitByte(Prefix.REXWB);
            }
        }
        else
        {
            if (needsRex(__adr.getIndex()))
            {
                emitByte(Prefix.REXWX);
            }
            else
            {
                emitByte(Prefix.REXW);
            }
        }
    }

    private void prefix(AMD64Address __adr, Register __reg)
    {
        prefix(__adr, __reg, false);
    }

    private void prefix(AMD64Address __adr, Register __reg, boolean __byteinst)
    {
        if (__reg.encoding < 8)
        {
            if (needsRex(__adr.getBase()))
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXXB);
                }
                else
                {
                    emitByte(Prefix.REXB);
                }
            }
            else
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXX);
                }
                else if (__byteinst && __reg.encoding >= 4)
                {
                    emitByte(Prefix.REX);
                }
            }
        }
        else
        {
            if (needsRex(__adr.getBase()))
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXRXB);
                }
                else
                {
                    emitByte(Prefix.REXRB);
                }
            }
            else
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXRX);
                }
                else
                {
                    emitByte(Prefix.REXR);
                }
            }
        }
    }

    private void prefixq(AMD64Address __adr, Register __src)
    {
        if (__src.encoding < 8)
        {
            if (needsRex(__adr.getBase()))
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXWXB);
                }
                else
                {
                    emitByte(Prefix.REXWB);
                }
            }
            else
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXWX);
                }
                else
                {
                    emitByte(Prefix.REXW);
                }
            }
        }
        else
        {
            if (needsRex(__adr.getBase()))
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXWRXB);
                }
                else
                {
                    emitByte(Prefix.REXWRB);
                }
            }
            else
            {
                if (needsRex(__adr.getIndex()))
                {
                    emitByte(Prefix.REXWRX);
                }
                else
                {
                    emitByte(Prefix.REXWR);
                }
            }
        }
    }

    public final void addq(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.ADD.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void addq(AMD64Address __dst, int __imm32)
    {
        AMD64BinaryArithmetic.ADD.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void addq(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.ADD.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void addq(AMD64Address __dst, Register __src)
    {
        AMD64BinaryArithmetic.ADD.___mrOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void andq(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.AND.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void bsrq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding(), __src.encoding());
        emitByte(0x0F);
        emitByte(0xBD);
        emitByte(0xC0 | __encode);
    }

    public final void bswapq(Register __reg)
    {
        int __encode = prefixqAndEncode(__reg.encoding);
        emitByte(0x0F);
        emitByte(0xC8 | __encode);
    }

    public final void cdqq()
    {
        emitByte(Prefix.REXW);
        emitByte(0x99);
    }

    public final void cmovq(ConditionFlag __cc, Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x0F);
        emitByte(0x40 | __cc.getValue());
        emitByte(0xC0 | __encode);
    }

    public final void setb(ConditionFlag __cc, Register __dst)
    {
        int __encode = prefixAndEncode(__dst.encoding, true);
        emitByte(0x0F);
        emitByte(0x90 | __cc.getValue());
        emitByte(0xC0 | __encode);
    }

    public final void cmovq(ConditionFlag __cc, Register __dst, AMD64Address __src)
    {
        prefixq(__src, __dst);
        emitByte(0x0F);
        emitByte(0x40 | __cc.getValue());
        emitOperandHelper(__dst, __src, 0);
    }

    public final void cmpq(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.CMP.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void cmpq(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.CMP.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void cmpq(Register __dst, AMD64Address __src)
    {
        AMD64BinaryArithmetic.CMP.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void cmpxchgq(Register __reg, AMD64Address __adr)
    {
        prefixq(__adr, __reg);
        emitByte(0x0F);
        emitByte(0xB1);
        emitOperandHelper(__reg, __adr, 0);
    }

    public final void cvtdq2pd(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xE6);
        emitByte(0xC0 | __encode);
    }

    public final void cvtsi2sdq(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, __dst, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x2A);
        emitByte(0xC0 | __encode);
    }

    public final void cvttsd2siq(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x2C);
        emitByte(0xC0 | __encode);
    }

    public final void cvttpd2dq(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0xE6);
        emitByte(0xC0 | __encode);
    }

    protected final void decq(Register __dst)
    {
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode).
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xFF);
        emitByte(0xC8 | __encode);
    }

    public final void decq(AMD64Address __dst)
    {
        AMD64MOp.DEC.emit(this, OperandSize.QWORD, __dst);
    }

    public final void imulq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x0F);
        emitByte(0xAF);
        emitByte(0xC0 | __encode);
    }

    public final void incq(Register __dst)
    {
        // Don't use it directly. Use Macroincrementq() instead.
        // Use two-byte form (one-byte from is a REX prefix in 64-bit mode).
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xFF);
        emitByte(0xC0 | __encode);
    }

    public final void incq(AMD64Address __dst)
    {
        AMD64MOp.INC.emit(this, OperandSize.QWORD, __dst);
    }

    public final void movq(Register __dst, long __imm64)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xB8 | __encode);
        emitLong(__imm64);
    }

    public final void movslq(Register __dst, int __imm32)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xC7);
        emitByte(0xC0 | __encode);
        emitInt(__imm32);
    }

    public final void movdq(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6E);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movdq(AMD64Address __dst, Register __src)
    {
        // swap src/dst to get correct prefix
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
        simdPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x7E);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void movdq(Register __dst, Register __src)
    {
        if (__dst.getRegisterCategory().equals(AMD64.XMM) && __src.getRegisterCategory().equals(AMD64.CPU))
        {
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
            int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x6E);
            emitByte(0xC0 | __encode);
        }
        else if (__src.getRegisterCategory().equals(AMD64.XMM) && __dst.getRegisterCategory().equals(AMD64.CPU))
        {
            // swap src/dst to get correct prefix
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, true, false, false, false, this.___target);
            int __encode = simdPrefixAndEncode(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x7E);
            emitByte(0xC0 | __encode);
        }
        else
        {
            throw new InternalError("should not reach here");
        }
    }

    public final void movdl(Register __dst, Register __src)
    {
        if (__dst.getRegisterCategory().equals(AMD64.XMM) && __src.getRegisterCategory().equals(AMD64.CPU))
        {
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
            int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x6E);
            emitByte(0xC0 | __encode);
        }
        else if (__src.getRegisterCategory().equals(AMD64.XMM) && __dst.getRegisterCategory().equals(AMD64.CPU))
        {
            // swap src/dst to get correct prefix
            AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
            int __encode = simdPrefixAndEncode(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
            emitByte(0x7E);
            emitByte(0xC0 | __encode);
        }
        else
        {
            throw new InternalError("should not reach here");
        }
    }

    public final void movdl(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_66, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6E);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movddup(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F2, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x12);
        emitByte(0xC0 | __encode);
    }

    public final void movdqu(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        simdPrefix(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6F);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movdqu(Register __dst, Register __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        int __encode = simdPrefixAndEncode(__dst, Register.None, __src, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6F);
        emitByte(0xC0 | __encode);
    }

    public final void vmovdqu(Register __dst, AMD64Address __src)
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_256bit, false, false, false, false, this.___target);
        vexPrefix(__src, Register.None, __dst, VexSimdPrefix.VEX_SIMD_F3, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x6F);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void vzeroupper()
    {
        AMD64InstructionAttr __attributes = new AMD64InstructionAttr(AvxVectorLen.AVX_128bit, false, false, false, false, this.___target);
        vexPrefixAndEncode(AMD64.xmm0, AMD64.xmm0, AMD64.xmm0, VexSimdPrefix.VEX_SIMD_NONE, VexOpcode.VEX_OPCODE_0F, __attributes);
        emitByte(0x77);
    }

    public final void movslq(AMD64Address __dst, int __imm32)
    {
        prefixq(__dst);
        emitByte(0xC7);
        emitOperandHelper(0, __dst, 4);
        emitInt(__imm32);
    }

    public final void movslq(Register __dst, AMD64Address __src)
    {
        prefixq(__src, __dst);
        emitByte(0x63);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void movslq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x63);
        emitByte(0xC0 | __encode);
    }

    public final void negq(Register __dst)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xF7);
        emitByte(0xD8 | __encode);
    }

    public final void orq(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.OR.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void shlq(Register __dst, int __imm8)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        if (__imm8 == 1)
        {
            emitByte(0xD1);
            emitByte(0xE0 | __encode);
        }
        else
        {
            emitByte(0xC1);
            emitByte(0xE0 | __encode);
            emitByte(__imm8);
        }
    }

    public final void shlq(Register __dst)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xD3);
        emitByte(0xE0 | __encode);
    }

    public final void shrq(Register __dst, int __imm8)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        if (__imm8 == 1)
        {
            emitByte(0xD1);
            emitByte(0xE8 | __encode);
        }
        else
        {
            emitByte(0xC1);
            emitByte(0xE8 | __encode);
            emitByte(__imm8);
        }
    }

    public final void shrq(Register __dst)
    {
        int __encode = prefixqAndEncode(__dst.encoding);
        emitByte(0xD3);
        emitByte(0xE8 | __encode);
    }

    public final void sbbq(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.SBB.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void subq(Register __dst, int __imm32)
    {
        AMD64BinaryArithmetic.SUB.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void subq(AMD64Address __dst, int __imm32)
    {
        AMD64BinaryArithmetic.SUB.getMIOpcode(OperandSize.QWORD, NumUtil.isByte(__imm32)).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void subqWide(Register __dst, int __imm32)
    {
        // don't use the sign-extending version, forcing a 32-bit immediate
        AMD64BinaryArithmetic.SUB.getMIOpcode(OperandSize.QWORD, false).emit(this, OperandSize.QWORD, __dst, __imm32);
    }

    public final void subq(Register __dst, Register __src)
    {
        AMD64BinaryArithmetic.SUB.___rmOp.emit(this, OperandSize.QWORD, __dst, __src);
    }

    public final void testq(Register __dst, Register __src)
    {
        int __encode = prefixqAndEncode(__dst.encoding, __src.encoding);
        emitByte(0x85);
        emitByte(0xC0 | __encode);
    }

    public final void btrq(Register __src, int __imm8)
    {
        int __encode = prefixqAndEncode(__src.encoding);
        emitByte(0x0F);
        emitByte(0xBA);
        emitByte(0xF0 | __encode);
        emitByte(__imm8);
    }

    public final void xaddl(AMD64Address __dst, Register __src)
    {
        prefix(__dst, __src);
        emitByte(0x0F);
        emitByte(0xC1);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void xaddq(AMD64Address __dst, Register __src)
    {
        prefixq(__dst, __src);
        emitByte(0x0F);
        emitByte(0xC1);
        emitOperandHelper(__src, __dst, 0);
    }

    public final void xchgl(Register __dst, AMD64Address __src)
    {
        prefix(__src, __dst);
        emitByte(0x87);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void xchgq(Register __dst, AMD64Address __src)
    {
        prefixq(__src, __dst);
        emitByte(0x87);
        emitOperandHelper(__dst, __src, 0);
    }

    public final void membar(int __barriers)
    {
        if (this.___target.isMP)
        {
            // We only have to handle StoreLoad.
            if ((__barriers & MemoryBarriers.STORE_LOAD) != 0)
            {
                // All usable chips support "locked" instructions which suffice as barriers,
                // and are much faster than the alternative of using cpuid instruction.
                // We use here a locked add [rsp],0. This is conveniently otherwise a no-op except
                // for blowing flags. Any change to this code may need to revisit other places
                // in the code where this idiom is used, in particular the orderAccess code.
                lock();
                addl(new AMD64Address(AMD64.rsp, 0), 0); // Assert the lock# signal here.
            }
        }
    }

    @Override
    protected final void patchJumpTarget(int __branch, int __branchTarget)
    {
        int __op = getByte(__branch);

        if (__op == 0x00)
        {
            int __offsetToJumpTableBase = getShort(__branch + 1);
            int __jumpTableBase = __branch - __offsetToJumpTableBase;
            int __imm32 = __branchTarget - __jumpTableBase;
            emitInt(__imm32, __branch);
        }
        else if (__op == 0xEB || (__op & 0xF0) == 0x70)
        {
            // short offset operators (jmp and jcc)
            final int __imm8 = __branchTarget - (__branch + 2);
            // Since a wrongly patched short branch can potentially lead to working but really bad
            // behaving code we should always fail with an exception instead of having an assert.
            if (!NumUtil.isByte(__imm8))
            {
                throw new InternalError("branch displacement out of range: " + __imm8);
            }
            emitByte(__imm8, __branch + 1);
        }
        else
        {
            int __off = 1;
            if (__op == 0x0F)
            {
                __off = 2;
            }

            int __imm32 = __branchTarget - (__branch + 4 + __off);
            emitInt(__imm32, __branch + __off);
        }
    }

    public void nullCheck(AMD64Address __address)
    {
        testl(AMD64.rax, __address);
    }

    @Override
    public void align(int __modulus)
    {
        if (position() % __modulus != 0)
        {
            nop(__modulus - (position() % __modulus));
        }
    }

    ///
    // Emits a direct call instruction. Note that the actual call target is not specified, because
    // all calls need patching anyway. Therefore, 0 is emitted as the call target, and the user is
    // responsible to add the call address to the appropriate patching tables.
    ///
    public final void call()
    {
        emitByte(0xE8);
        emitInt(0);
    }

    public final void call(Register __src)
    {
        int __encode = prefixAndEncode(__src.encoding);
        emitByte(0xFF);
        emitByte(0xD0 | __encode);
    }

    public final void int3()
    {
        emitByte(0xCC);
    }

    public final void pause()
    {
        emitByte(0xF3);
        emitByte(0x90);
    }

    private void emitx87(int __b1, int __b2, int __i)
    {
        emitByte(__b1);
        emitByte(__b2 + __i);
    }

    public final void fldd(AMD64Address __src)
    {
        emitByte(0xDD);
        emitOperandHelper(0, __src, 0);
    }

    public final void flds(AMD64Address __src)
    {
        emitByte(0xD9);
        emitOperandHelper(0, __src, 0);
    }

    public final void fstps(AMD64Address __src)
    {
        emitByte(0xD9);
        emitOperandHelper(3, __src, 0);
    }

    public final void fstpd(AMD64Address __src)
    {
        emitByte(0xDD);
        emitOperandHelper(3, __src, 0);
    }

    private void emitFPUArith(int __b1, int __b2, int __i)
    {
        emitByte(__b1);
        emitByte(__b2 + __i);
    }

    public void ffree(int __i)
    {
        emitFPUArith(0xDD, 0xC0, __i);
    }

    public void fincstp()
    {
        emitByte(0xD9);
        emitByte(0xF7);
    }

    public void fxch(int __i)
    {
        emitFPUArith(0xD9, 0xC8, __i);
    }

    public void fnstswAX()
    {
        emitByte(0xDF);
        emitByte(0xE0);
    }

    public void fwait()
    {
        emitByte(0x9B);
    }

    public void fprem()
    {
        emitByte(0xD9);
        emitByte(0xF8);
    }

    @Override
    public AMD64Address makeAddress(Register __base, int __displacement)
    {
        return new AMD64Address(__base, __displacement);
    }

    @Override
    public AMD64Address getPlaceholder(int __instructionStartPosition)
    {
        return new AMD64Address(AMD64.rip, Register.None, Scale.Times1, 0, __instructionStartPosition);
    }

    private void prefetchPrefix(AMD64Address __src)
    {
        prefix(__src);
        emitByte(0x0F);
    }

    public void prefetchnta(AMD64Address __src)
    {
        prefetchPrefix(__src);
        emitByte(0x18);
        emitOperandHelper(0, __src, 0);
    }

    void prefetchr(AMD64Address __src)
    {
        prefetchPrefix(__src);
        emitByte(0x0D);
        emitOperandHelper(0, __src, 0);
    }

    public void prefetcht0(AMD64Address __src)
    {
        prefetchPrefix(__src);
        emitByte(0x18);
        emitOperandHelper(1, __src, 0);
    }

    public void prefetcht1(AMD64Address __src)
    {
        prefetchPrefix(__src);
        emitByte(0x18);
        emitOperandHelper(2, __src, 0);
    }

    public void prefetcht2(AMD64Address __src)
    {
        prefix(__src);
        emitByte(0x0f);
        emitByte(0x18);
        emitOperandHelper(3, __src, 0);
    }

    public void prefetchw(AMD64Address __src)
    {
        prefix(__src);
        emitByte(0x0f);
        emitByte(0x0D);
        emitOperandHelper(1, __src, 0);
    }

    public void rdtsc()
    {
        emitByte(0x0F);
        emitByte(0x31);
    }

    ///
    // Emits an instruction which is considered to be illegal. This is used if we deliberately want
    // to crash the program (debugging etc.).
    ///
    public void illegal()
    {
        emitByte(0x0f);
        emitByte(0x0b);
    }

    public void lfence()
    {
        emitByte(0x0f);
        emitByte(0xae);
        emitByte(0xe8);
    }
}
