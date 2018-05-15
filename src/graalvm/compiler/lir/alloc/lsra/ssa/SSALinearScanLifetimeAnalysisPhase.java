package graalvm.compiler.lir.alloc.lsra.ssa;

import java.util.EnumSet;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.ValueConsumer;
import graalvm.compiler.lir.alloc.lsra.Interval;
import graalvm.compiler.lir.alloc.lsra.Interval.RegisterPriority;
import graalvm.compiler.lir.alloc.lsra.LinearScan;
import graalvm.compiler.lir.alloc.lsra.LinearScanLifetimeAnalysisPhase;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

public class SSALinearScanLifetimeAnalysisPhase extends LinearScanLifetimeAnalysisPhase {

    SSALinearScanLifetimeAnalysisPhase(LinearScan linearScan) {
        super(linearScan);
    }

    @Override
    protected void addRegisterHint(final LIRInstruction op, final Value targetValue, OperandMode mode, EnumSet<OperandFlag> flags, final boolean hintAtDef) {
        super.addRegisterHint(op, targetValue, mode, flags, hintAtDef);

        if (hintAtDef && op instanceof LabelOp) {
            LabelOp label = (LabelOp) op;

            Interval to = allocator.getOrCreateInterval((AllocatableValue) targetValue);

            SSAUtil.forEachPhiRegisterHint(allocator.getLIR(), allocator.blockForId(label.id()), label, targetValue, mode, (ValueConsumer) (registerHint, valueMode, valueFlags) -> {
                if (LinearScan.isVariableOrRegister(registerHint)) {
                    Interval from = allocator.getOrCreateInterval((AllocatableValue) registerHint);

                    setHint(debug, op, to, from);
                    setHint(debug, op, from, to);
                }
            });
        }
    }

    public static void setHint(DebugContext debug, final LIRInstruction op, Interval target, Interval source) {
        Interval currentHint = target.locationHint(false);
        if (currentHint == null || currentHint.from() > target.from()) {
            /*
             * Update hint if there was none or if the hint interval starts after the hinted
             * interval.
             */
            target.setLocationHint(source);
            if (debug.isLogEnabled()) {
                debug.log("operation at opId %d: added hint from interval %d to %d", op.id(), source.operandNumber, target.operandNumber);
            }
        }
    }

    @Override
    protected RegisterPriority registerPriorityOfOutputOperand(LIRInstruction op) {
        if (op instanceof LabelOp) {
            LabelOp label = (LabelOp) op;
            if (label.isPhiIn()) {
                return RegisterPriority.None;
            }
        }
        return super.registerPriorityOfOutputOperand(op);
    }
}
