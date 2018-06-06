package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.InvokeTarget;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64Call
public final class AMD64Call
{
    // @class AMD64Call.CallOp
    public abstract static class CallOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.CallOp> TYPE = LIRInstructionClass.create(AMD64Call.CallOp.class);

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
        // @field
        protected Value ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected Value[] ___parameters;
        @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected Value[] ___temps;
        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64Call.CallOp
        protected CallOp(LIRInstructionClass<? extends AMD64Call.CallOp> __c, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c);
            this.___result = __result;
            this.___parameters = __parameters;
            this.___state = __state;
            this.___temps = addStackSlotsToTemporaries(__parameters, __temps);
        }

        @Override
        public boolean destroysCallerSavedRegisters()
        {
            return true;
        }
    }

    // @class AMD64Call.MethodCallOp
    public abstract static class MethodCallOp extends AMD64Call.CallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.MethodCallOp> TYPE = LIRInstructionClass.create(AMD64Call.MethodCallOp.class);

        // @field
        protected final ResolvedJavaMethod ___callTarget;

        // @cons AMD64Call.MethodCallOp
        protected MethodCallOp(LIRInstructionClass<? extends AMD64Call.MethodCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __result, __parameters, __temps, __state);
            this.___callTarget = __callTarget;
        }
    }

    @LIROpcode
    // @class AMD64Call.DirectCallOp
    public static class DirectCallOp extends AMD64Call.MethodCallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.DirectCallOp> TYPE = LIRInstructionClass.create(AMD64Call.DirectCallOp.class);

        // @cons AMD64Call.DirectCallOp
        public DirectCallOp(ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            this(TYPE, __callTarget, __result, __parameters, __temps, __state);
        }

        // @cons AMD64Call.DirectCallOp
        protected DirectCallOp(LIRInstructionClass<? extends AMD64Call.DirectCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __callTarget, __result, __parameters, __temps, __state);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, this.___callTarget, null, true, this.___state);
        }

        public int emitCall(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            return directCall(__crb, __masm, this.___callTarget, null, true, this.___state);
        }
    }

    @LIROpcode
    // @class AMD64Call.IndirectCallOp
    public static class IndirectCallOp extends AMD64Call.MethodCallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.IndirectCallOp> TYPE = LIRInstructionClass.create(AMD64Call.IndirectCallOp.class);

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected Value ___targetAddress;

        // @cons AMD64Call.IndirectCallOp
        public IndirectCallOp(ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, Value __targetAddress, LIRFrameState __state)
        {
            this(TYPE, __callTarget, __result, __parameters, __temps, __targetAddress, __state);
        }

        // @cons AMD64Call.IndirectCallOp
        protected IndirectCallOp(LIRInstructionClass<? extends AMD64Call.IndirectCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, Value __targetAddress, LIRFrameState __state)
        {
            super(__c, __callTarget, __result, __parameters, __temps, __state);
            this.___targetAddress = __targetAddress;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            indirectCall(__crb, __masm, ValueUtil.asRegister(this.___targetAddress), this.___callTarget, this.___state);
        }
    }

    // @class AMD64Call.ForeignCallOp
    public abstract static class ForeignCallOp extends AMD64Call.CallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.ForeignCallOp> TYPE = LIRInstructionClass.create(AMD64Call.ForeignCallOp.class);

        // @field
        protected final ForeignCallLinkage ___callTarget;

        // @cons AMD64Call.ForeignCallOp
        public ForeignCallOp(LIRInstructionClass<? extends AMD64Call.ForeignCallOp> __c, ForeignCallLinkage __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __result, __parameters, __temps, __state);
            this.___callTarget = __callTarget;
        }

        @Override
        public boolean destroysCallerSavedRegisters()
        {
            return this.___callTarget.destroysRegisters();
        }
    }

    @LIROpcode
    // @class AMD64Call.DirectNearForeignCallOp
    public static final class DirectNearForeignCallOp extends AMD64Call.ForeignCallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.DirectNearForeignCallOp> TYPE = LIRInstructionClass.create(AMD64Call.DirectNearForeignCallOp.class);

        // @cons AMD64Call.DirectNearForeignCallOp
        public DirectNearForeignCallOp(ForeignCallLinkage __linkage, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(TYPE, __linkage, __result, __parameters, __temps, __state);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, this.___callTarget, null, false, this.___state);
        }
    }

    @LIROpcode
    // @class AMD64Call.DirectFarForeignCallOp
    public static final class DirectFarForeignCallOp extends AMD64Call.ForeignCallOp
    {
        // @def
        public static final LIRInstructionClass<AMD64Call.DirectFarForeignCallOp> TYPE = LIRInstructionClass.create(AMD64Call.DirectFarForeignCallOp.class);

        @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___callTemp;

        // @cons AMD64Call.DirectFarForeignCallOp
        public DirectFarForeignCallOp(ForeignCallLinkage __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(TYPE, __callTarget, __result, __parameters, __temps, __state);
            // The register allocator does not support virtual registers that are used at the call site, so use a fixed register.
            this.___callTemp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, this.___callTarget, ((RegisterValue) this.___callTemp).getRegister(), false, this.___state);
        }
    }

    public static int directCall(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, InvokeTarget __callTarget, Register __scratch, boolean __align, LIRFrameState __info)
    {
        if (__align)
        {
            emitAlignmentForDirectCall(__crb, __masm);
        }
        int __callPCOffset;
        if (__scratch != null)
        {
            // offset might not fit a 32-bit immediate, generate an indirect call with a 64-bit immediate
            __masm.movq(__scratch, 0L);
            __callPCOffset = __masm.position();
            __masm.call(__scratch);
        }
        else
        {
            __callPCOffset = __masm.position();
            __masm.call();
        }
        __crb.recordExceptionHandlers(__masm.position(), __info);
        __masm.ensureUniquePC();
        return __callPCOffset;
    }

    protected static void emitAlignmentForDirectCall(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // make sure that the displacement word of the call ends up word aligned
        int __offset = __masm.position();
        __offset += __crb.___target.arch.getMachineCodeCallDisplacementOffset();
        int __modulus = __crb.___target.wordSize;
        if (__offset % __modulus != 0)
        {
            __masm.nop(__modulus - __offset % __modulus);
        }
    }

    public static void directJmp(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, InvokeTarget __target)
    {
        __masm.jmp(0, true);
        __masm.ensureUniquePC();
    }

    public static void directConditionalJmp(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, InvokeTarget __target, AMD64Assembler.ConditionFlag __cond)
    {
        __masm.jcc(__cond, 0, true);
        __masm.ensureUniquePC();
    }

    public static int indirectCall(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __dst, InvokeTarget __callTarget, LIRFrameState __info)
    {
        int __before = __masm.position();
        __masm.call(__dst);
        __crb.recordExceptionHandlers(__masm.position(), __info);
        __masm.ensureUniquePC();
        return __before;
    }
}
