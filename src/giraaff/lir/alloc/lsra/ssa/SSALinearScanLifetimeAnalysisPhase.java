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
    SSALinearScanLifetimeAnalysisPhase(LinearScan linearScan)
    {
        super(linearScan);
    }

    @Override
    protected void addRegisterHint(final LIRInstruction op, final Value targetValue, OperandMode mode, EnumSet<OperandFlag> flags, final boolean hintAtDef)
    {
        super.addRegisterHint(op, targetValue, mode, flags, hintAtDef);

        if (hintAtDef && op instanceof LabelOp)
        {
            LabelOp label = (LabelOp) op;

            Interval to = allocator.getOrCreateInterval((AllocatableValue) targetValue);

            SSAUtil.forEachPhiRegisterHint(allocator.getLIR(), allocator.blockForId(label.id()), label, targetValue, mode, (ValueConsumer) (registerHint, valueMode, valueFlags) ->
            {
                if (LinearScan.isVariableOrRegister(registerHint))
                {
                    Interval from = allocator.getOrCreateInterval((AllocatableValue) registerHint);

                    setHint(op, to, from);
                    setHint(op, from, to);
                }
            });
        }
    }

    public static void setHint(final LIRInstruction op, Interval target, Interval source)
    {
        Interval currentHint = target.locationHint(false);
        if (currentHint == null || currentHint.from() > target.from())
        {
            // Update hint if there was none or if the hint interval starts after the hinted interval.
            target.setLocationHint(source);
        }
    }

    @Override
    protected RegisterPriority registerPriorityOfOutputOperand(LIRInstruction op)
    {
        if (op instanceof LabelOp)
        {
            LabelOp label = (LabelOp) op;
            if (label.isPhiIn())
            {
                return RegisterPriority.None;
            }
        }
        return super.registerPriorityOfOutputOperand(op);
    }
}
