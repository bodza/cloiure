package giraaff.hotspot.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.CompressEncoding;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64HotSpotMove
public final class AMD64HotSpotMove
{
    // @cons
    private AMD64HotSpotMove()
    {
        super();
    }

    // @class AMD64HotSpotMove.HotSpotLoadObjectConstantOp
    public static final class HotSpotLoadObjectConstantOp extends AMD64LIRInstruction implements LoadConstantOp
    {
        // @def
        public static final LIRInstructionClass<HotSpotLoadObjectConstantOp> TYPE = LIRInstructionClass.create(HotSpotLoadObjectConstantOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        private AllocatableValue result;
        // @field
        private final HotSpotObjectConstant input;

        // @cons
        public HotSpotLoadObjectConstantOp(AllocatableValue __result, HotSpotObjectConstant __input)
        {
            super(TYPE);
            this.result = __result;
            this.input = __input;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            boolean __compressed = input.isCompressed();
            if (__crb.target.inlineObjects)
            {
                __crb.recordInlineDataInCode(input);
                if (ValueUtil.isRegister(result))
                {
                    if (__compressed)
                    {
                        __masm.movl(ValueUtil.asRegister(result), 0xDEADDEAD);
                    }
                    else
                    {
                        __masm.movq(ValueUtil.asRegister(result), 0xDEADDEADDEADDEADL);
                    }
                }
                else
                {
                    if (__compressed)
                    {
                        __masm.movl((AMD64Address) __crb.asAddress(result), 0xDEADDEAD);
                    }
                    else
                    {
                        throw GraalError.shouldNotReachHere("Cannot store 64-bit constants to memory");
                    }
                }
            }
            else
            {
                if (ValueUtil.isRegister(result))
                {
                    AMD64Address __address = (AMD64Address) __crb.recordDataReferenceInCode(input, __compressed ? 4 : 8);
                    if (__compressed)
                    {
                        __masm.movl(ValueUtil.asRegister(result), __address);
                    }
                    else
                    {
                        __masm.movq(ValueUtil.asRegister(result), __address);
                    }
                }
                else
                {
                    throw GraalError.shouldNotReachHere("Cannot directly store data patch to memory");
                }
            }
        }

        @Override
        public Constant getConstant()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }

    // @class AMD64HotSpotMove.BaseMove
    public static final class BaseMove extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<BaseMove> TYPE = LIRInstructionClass.create(BaseMove.class);

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue result;

        // @cons
        public BaseMove(AllocatableValue __result)
        {
            super(TYPE);
            this.result = __result;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __masm.movq(ValueUtil.asRegister(result), __masm.getPlaceholder(-1));
            __crb.recordMark(HotSpotRuntime.narrowKlassBaseAddressMark);
        }
    }

    // @class AMD64HotSpotMove.HotSpotLoadMetaspaceConstantOp
    public static final class HotSpotLoadMetaspaceConstantOp extends AMD64LIRInstruction implements LoadConstantOp
    {
        // @def
        public static final LIRInstructionClass<HotSpotLoadMetaspaceConstantOp> TYPE = LIRInstructionClass.create(HotSpotLoadMetaspaceConstantOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        private AllocatableValue result;
        // @field
        private final HotSpotMetaspaceConstant input;

        // @cons
        public HotSpotLoadMetaspaceConstantOp(AllocatableValue __result, HotSpotMetaspaceConstant __input)
        {
            super(TYPE);
            this.result = __result;
            this.input = __input;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            boolean __compressed = input.isCompressed();
            if (ValueUtil.isRegister(result))
            {
                if (__compressed)
                {
                    __crb.recordInlineDataInCode(input);
                    __masm.movl(ValueUtil.asRegister(result), 0xDEADDEAD);
                }
                else
                {
                    __crb.recordInlineDataInCode(input);
                    __masm.movq(ValueUtil.asRegister(result), 0xDEADDEADDEADDEADL);
                }
            }
            else
            {
                if (__compressed)
                {
                    __crb.recordInlineDataInCode(input);
                    __masm.movl((AMD64Address) __crb.asAddress(result), 0xDEADDEAD);
                }
                else
                {
                    throw GraalError.shouldNotReachHere("Cannot store 64-bit constants to memory");
                }
            }
        }

        @Override
        public Constant getConstant()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }

    public static void decodeKlassPointer(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __register, Register __scratch, AMD64Address __address)
    {
        CompressEncoding __encoding = HotSpotRuntime.klassEncoding;
        __masm.movl(__register, __address);
        if (__encoding.getShift() != 0)
        {
            __masm.shlq(__register, __encoding.getShift());
        }
        if (__encoding.hasBase())
        {
            __masm.movq(__scratch, __encoding.getBase());
            __masm.addq(__register, __scratch);
        }
    }
}
