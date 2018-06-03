package giraaff.lir.alloc.lsra.ssa;

import java.util.EnumSet;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.ValueConsumer;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.alloc.lsra.Interval.RegisterPriority;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.LinearScanLifetimeAnalysisPhase;
import giraaff.lir.ssa.SSAUtil;

// @class SSALinearScanLifetimeAnalysisPhase
public final class SSALinearScanLifetimeAnalysisPhase extends LinearScanLifetimeAnalysisPhase
{
    // @cons
    SSALinearScanLifetimeAnalysisPhase(LinearScan __linearScan)
    {
        super(__linearScan);
    }

    @Override
    protected void addRegisterHint(final LIRInstruction __op, final Value __targetValue, OperandMode __mode, EnumSet<OperandFlag> __flags, final boolean __hintAtDef)
    {
        super.addRegisterHint(__op, __targetValue, __mode, __flags, __hintAtDef);

        if (__hintAtDef && __op instanceof LabelOp)
        {
            LabelOp __label = (LabelOp) __op;

            Interval __to = this.___allocator.getOrCreateInterval((AllocatableValue) __targetValue);

            SSAUtil.forEachPhiRegisterHint(this.___allocator.getLIR(), this.___allocator.blockForId(__label.id()), __label, __targetValue, __mode, (ValueConsumer) (__registerHint, __valueMode, __valueFlags) ->
            {
                if (LinearScan.isVariableOrRegister(__registerHint))
                {
                    Interval __from = this.___allocator.getOrCreateInterval((AllocatableValue) __registerHint);

                    setHint(__op, __to, __from);
                    setHint(__op, __from, __to);
                }
            });
        }
    }

    public static void setHint(final LIRInstruction __op, Interval __target, Interval __source)
    {
        Interval __currentHint = __target.locationHint(false);
        if (__currentHint == null || __currentHint.from() > __target.from())
        {
            // Update hint if there was none or if the hint interval starts after the hinted interval.
            __target.setLocationHint(__source);
        }
    }

    @Override
    protected RegisterPriority registerPriorityOfOutputOperand(LIRInstruction __op)
    {
        if (__op instanceof LabelOp)
        {
            LabelOp __label = (LabelOp) __op;
            if (__label.isPhiIn())
            {
                return RegisterPriority.None;
            }
        }
        return super.registerPriorityOfOutputOperand(__op);
    }
}
