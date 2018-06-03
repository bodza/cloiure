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

import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.DiagnosticLIRGeneratorTool.ZapRegistersAfterInstruction;

// @class AMD64Call
public final class AMD64Call
{
    // @class AMD64Call.CallOp
    public abstract static class CallOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<CallOp> TYPE = LIRInstructionClass.create(CallOp.class);

        @Def({OperandFlag.REG, OperandFlag.ILLEGAL})
        // @field
        protected Value result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected Value[] parameters;
        @Temp({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected Value[] temps;
        // @State
        // @field
        protected LIRFrameState state;

        // @cons
        protected CallOp(LIRInstructionClass<? extends CallOp> __c, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c);
            this.result = __result;
            this.parameters = __parameters;
            this.state = __state;
            this.temps = addStackSlotsToTemporaries(__parameters, __temps);
        }

        @Override
        public boolean destroysCallerSavedRegisters()
        {
            return true;
        }
    }

    // @class AMD64Call.MethodCallOp
    public abstract static class MethodCallOp extends CallOp
    {
        // @def
        public static final LIRInstructionClass<MethodCallOp> TYPE = LIRInstructionClass.create(MethodCallOp.class);

        // @field
        protected final ResolvedJavaMethod callTarget;

        // @cons
        protected MethodCallOp(LIRInstructionClass<? extends MethodCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __result, __parameters, __temps, __state);
            this.callTarget = __callTarget;
        }
    }

    @Opcode
    // @class AMD64Call.DirectCallOp
    public static class DirectCallOp extends MethodCallOp
    {
        // @def
        public static final LIRInstructionClass<DirectCallOp> TYPE = LIRInstructionClass.create(DirectCallOp.class);

        // @cons
        public DirectCallOp(ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            this(TYPE, __callTarget, __result, __parameters, __temps, __state);
        }

        // @cons
        protected DirectCallOp(LIRInstructionClass<? extends DirectCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __callTarget, __result, __parameters, __temps, __state);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, callTarget, null, true, state);
        }

        public int emitCall(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            return directCall(__crb, __masm, callTarget, null, true, state);
        }
    }

    @Opcode
    // @class AMD64Call.IndirectCallOp
    public static class IndirectCallOp extends MethodCallOp
    {
        // @def
        public static final LIRInstructionClass<IndirectCallOp> TYPE = LIRInstructionClass.create(IndirectCallOp.class);

        @Use({OperandFlag.REG})
        // @field
        protected Value targetAddress;

        // @cons
        public IndirectCallOp(ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, Value __targetAddress, LIRFrameState __state)
        {
            this(TYPE, __callTarget, __result, __parameters, __temps, __targetAddress, __state);
        }

        // @cons
        protected IndirectCallOp(LIRInstructionClass<? extends IndirectCallOp> __c, ResolvedJavaMethod __callTarget, Value __result, Value[] __parameters, Value[] __temps, Value __targetAddress, LIRFrameState __state)
        {
            super(__c, __callTarget, __result, __parameters, __temps, __state);
            this.targetAddress = __targetAddress;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            indirectCall(__crb, __masm, ValueUtil.asRegister(targetAddress), callTarget, state);
        }
    }

    // @class AMD64Call.ForeignCallOp
    public abstract static class ForeignCallOp extends CallOp implements ZapRegistersAfterInstruction
    {
        // @def
        public static final LIRInstructionClass<ForeignCallOp> TYPE = LIRInstructionClass.create(ForeignCallOp.class);

        // @field
        protected final ForeignCallLinkage callTarget;

        // @cons
        public ForeignCallOp(LIRInstructionClass<? extends ForeignCallOp> __c, ForeignCallLinkage __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(__c, __result, __parameters, __temps, __state);
            this.callTarget = __callTarget;
        }

        @Override
        public boolean destroysCallerSavedRegisters()
        {
            return callTarget.destroysRegisters();
        }
    }

    @Opcode
    // @class AMD64Call.DirectNearForeignCallOp
    public static final class DirectNearForeignCallOp extends ForeignCallOp
    {
        // @def
        public static final LIRInstructionClass<DirectNearForeignCallOp> TYPE = LIRInstructionClass.create(DirectNearForeignCallOp.class);

        // @cons
        public DirectNearForeignCallOp(ForeignCallLinkage __linkage, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(TYPE, __linkage, __result, __parameters, __temps, __state);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, callTarget, null, false, state);
        }
    }

    @Opcode
    // @class AMD64Call.DirectFarForeignCallOp
    public static final class DirectFarForeignCallOp extends ForeignCallOp
    {
        // @def
        public static final LIRInstructionClass<DirectFarForeignCallOp> TYPE = LIRInstructionClass.create(DirectFarForeignCallOp.class);

        @Temp({OperandFlag.REG})
        // @field
        protected AllocatableValue callTemp;

        // @cons
        public DirectFarForeignCallOp(ForeignCallLinkage __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state)
        {
            super(TYPE, __callTarget, __result, __parameters, __temps, __state);
            // The register allocator does not support virtual registers that are used at the call site, so use a fixed register.
            callTemp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            directCall(__crb, __masm, callTarget, ((RegisterValue) callTemp).getRegister(), false, state);
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
        __offset += __crb.target.arch.getMachineCodeCallDisplacementOffset();
        int __modulus = __crb.target.wordSize;
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

    public static void directConditionalJmp(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, InvokeTarget __target, ConditionFlag __cond)
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
