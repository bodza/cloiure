package graalvm.compiler.lir.stackslotalloc;

import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.debug.IntervalDumper;

import jdk.vm.ci.meta.Value;

class StackIntervalDumper implements IntervalDumper {
    private final StackInterval[] intervals;

    StackIntervalDumper(StackInterval[] intervals) {
        this.intervals = intervals;
    }

    @Override
    public void visitIntervals(IntervalVisitor visitor) {
        for (StackInterval interval : intervals) {
            if (interval != null) {
                printInterval(interval, visitor);
            }
        }
    }

    private static void printInterval(StackInterval interval, IntervalVisitor visitor) {
        Value hint = interval.locationHint() != null ? interval.locationHint().getOperand() : null;
        VirtualStackSlot operand = interval.getOperand();
        String type = operand.getValueKind().getPlatformKind().toString();
        visitor.visitIntervalStart(operand, operand, interval.location(), hint, type);

        // print ranges
        visitor.visitRange(interval.from(), interval.to());

        // no use positions

        visitor.visitIntervalEnd("NOT_SUPPORTED");
    }

}
