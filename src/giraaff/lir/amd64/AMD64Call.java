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
        public static final LIRInstructionClass<CallOp> TYPE = LIRInstructionClass.create(CallOp.class);

        @Def({OperandFlag.REG, OperandFlag.ILLEGAL}) protected Value result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected Value[] parameters;
        @Temp({OperandFlag.REG, OperandFlag.STACK}) protected Value[] temps;
        // @State
        protected LIRFrameState state;

        // @cons
        protected CallOp(LIRInstructionClass<? extends CallOp> c, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(c);
            this.result = result;
            this.parameters = parameters;
            this.state = state;
            this.temps = addStackSlotsToTemporaries(parameters, temps);
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
        public static final LIRInstructionClass<MethodCallOp> TYPE = LIRInstructionClass.create(MethodCallOp.class);

        protected final ResolvedJavaMethod callTarget;

        // @cons
        protected MethodCallOp(LIRInstructionClass<? extends MethodCallOp> c, ResolvedJavaMethod callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(c, result, parameters, temps, state);
            this.callTarget = callTarget;
        }
    }

    @Opcode
    // @class AMD64Call.DirectCallOp
    public static class DirectCallOp extends MethodCallOp
    {
        public static final LIRInstructionClass<DirectCallOp> TYPE = LIRInstructionClass.create(DirectCallOp.class);

        // @cons
        public DirectCallOp(ResolvedJavaMethod callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            this(TYPE, callTarget, result, parameters, temps, state);
        }

        // @cons
        protected DirectCallOp(LIRInstructionClass<? extends DirectCallOp> c, ResolvedJavaMethod callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(c, callTarget, result, parameters, temps, state);
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            directCall(crb, masm, callTarget, null, true, state);
        }

        public int emitCall(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            return directCall(crb, masm, callTarget, null, true, state);
        }
    }

    @Opcode
    // @class AMD64Call.IndirectCallOp
    public static class IndirectCallOp extends MethodCallOp
    {
        public static final LIRInstructionClass<IndirectCallOp> TYPE = LIRInstructionClass.create(IndirectCallOp.class);

        @Use({OperandFlag.REG}) protected Value targetAddress;

        // @cons
        public IndirectCallOp(ResolvedJavaMethod callTarget, Value result, Value[] parameters, Value[] temps, Value targetAddress, LIRFrameState state)
        {
            this(TYPE, callTarget, result, parameters, temps, targetAddress, state);
        }

        // @cons
        protected IndirectCallOp(LIRInstructionClass<? extends IndirectCallOp> c, ResolvedJavaMethod callTarget, Value result, Value[] parameters, Value[] temps, Value targetAddress, LIRFrameState state)
        {
            super(c, callTarget, result, parameters, temps, state);
            this.targetAddress = targetAddress;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            indirectCall(crb, masm, ValueUtil.asRegister(targetAddress), callTarget, state);
        }
    }

    // @class AMD64Call.ForeignCallOp
    public abstract static class ForeignCallOp extends CallOp implements ZapRegistersAfterInstruction
    {
        public static final LIRInstructionClass<ForeignCallOp> TYPE = LIRInstructionClass.create(ForeignCallOp.class);

        protected final ForeignCallLinkage callTarget;

        // @cons
        public ForeignCallOp(LIRInstructionClass<? extends ForeignCallOp> c, ForeignCallLinkage callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(c, result, parameters, temps, state);
            this.callTarget = callTarget;
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
        public static final LIRInstructionClass<DirectNearForeignCallOp> TYPE = LIRInstructionClass.create(DirectNearForeignCallOp.class);

        // @cons
        public DirectNearForeignCallOp(ForeignCallLinkage linkage, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(TYPE, linkage, result, parameters, temps, state);
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            directCall(crb, masm, callTarget, null, false, state);
        }
    }

    @Opcode
    // @class AMD64Call.DirectFarForeignCallOp
    public static final class DirectFarForeignCallOp extends ForeignCallOp
    {
        public static final LIRInstructionClass<DirectFarForeignCallOp> TYPE = LIRInstructionClass.create(DirectFarForeignCallOp.class);

        @Temp({OperandFlag.REG}) protected AllocatableValue callTemp;

        // @cons
        public DirectFarForeignCallOp(ForeignCallLinkage callTarget, Value result, Value[] parameters, Value[] temps, LIRFrameState state)
        {
            super(TYPE, callTarget, result, parameters, temps, state);
            // The register allocator does not support virtual registers that are used at the call site, so use a fixed register.
            callTemp = AMD64.rax.asValue(LIRKind.value(AMD64Kind.QWORD));
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            directCall(crb, masm, callTarget, ((RegisterValue) callTemp).getRegister(), false, state);
        }
    }

    public static int directCall(CompilationResultBuilder crb, AMD64MacroAssembler masm, InvokeTarget callTarget, Register scratch, boolean align, LIRFrameState info)
    {
        if (align)
        {
            emitAlignmentForDirectCall(crb, masm);
        }
        int callPCOffset;
        if (scratch != null)
        {
            // offset might not fit a 32-bit immediate, generate an indirect call with a 64-bit immediate
            masm.movq(scratch, 0L);
            callPCOffset = masm.position();
            masm.call(scratch);
        }
        else
        {
            callPCOffset = masm.position();
            masm.call();
        }
        crb.recordExceptionHandlers(masm.position(), info);
        masm.ensureUniquePC();
        return callPCOffset;
    }

    protected static void emitAlignmentForDirectCall(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // make sure that the displacement word of the call ends up word aligned
        int offset = masm.position();
        offset += crb.target.arch.getMachineCodeCallDisplacementOffset();
        int modulus = crb.target.wordSize;
        if (offset % modulus != 0)
        {
            masm.nop(modulus - offset % modulus);
        }
    }

    public static void directJmp(CompilationResultBuilder crb, AMD64MacroAssembler masm, InvokeTarget target)
    {
        masm.jmp(0, true);
        masm.ensureUniquePC();
    }

    public static void directConditionalJmp(CompilationResultBuilder crb, AMD64MacroAssembler masm, InvokeTarget target, ConditionFlag cond)
    {
        masm.jcc(cond, 0, true);
        masm.ensureUniquePC();
    }

    public static int indirectCall(CompilationResultBuilder crb, AMD64MacroAssembler masm, Register dst, InvokeTarget callTarget, LIRFrameState info)
    {
        int before = masm.position();
        masm.call(dst);
        crb.recordExceptionHandlers(masm.position(), info);
        masm.ensureUniquePC();
        return before;
    }
}
