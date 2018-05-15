package graalvm.compiler.lir.alloc.trace;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.MemUseTrackerKey;
import graalvm.compiler.debug.TimerKey;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.lir.phases.LIRPhase.LIRPhaseStatistics;

import jdk.vm.ci.code.TargetDescription;

public abstract class TraceAllocationPhase<C extends TraceAllocationPhase.TraceAllocationContext> {

    public static class TraceAllocationContext {
        public final MoveFactory spillMoveFactory;
        public final RegisterAllocationConfig registerAllocationConfig;
        public final TraceBuilderResult resultTraces;
        public final GlobalLivenessInfo livenessInfo;

        public TraceAllocationContext(MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo) {
            this.spillMoveFactory = spillMoveFactory;
            this.registerAllocationConfig = registerAllocationConfig;
            this.resultTraces = resultTraces;
            this.livenessInfo = livenessInfo;
        }
    }

    /**
     * Records time spent within {@link #apply}.
     */
    private final TimerKey timer;

    /**
     * Records memory usage within {@link #apply}.
     */
    private final MemUseTrackerKey memUseTracker;

    /**
     * Records the number of traces allocated with this phase.
     */
    private final CounterKey allocatedTraces;

    public static final class AllocationStatistics {
        private final CounterKey allocatedTraces;

        public AllocationStatistics(Class<?> clazz) {
            allocatedTraces = DebugContext.counter("TraceRA[%s]", clazz);
        }
    }

    private static final ClassValue<AllocationStatistics> counterClassValue = new ClassValue<AllocationStatistics>() {
        @Override
        protected AllocationStatistics computeValue(Class<?> c) {
            return new AllocationStatistics(c);
        }
    };

    private static AllocationStatistics getAllocationStatistics(Class<?> c) {
        return counterClassValue.get(c);
    }

    public TraceAllocationPhase() {
        LIRPhaseStatistics statistics = LIRPhase.getLIRPhaseStatistics(getClass());
        timer = statistics.timer;
        memUseTracker = statistics.memUseTracker;
        allocatedTraces = getAllocationStatistics(getClass()).allocatedTraces;
    }

    public final CharSequence getName() {
        return LIRPhase.createName(getClass());
    }

    @Override
    public final String toString() {
        return getName().toString();
    }

    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, C context) {
        apply(target, lirGenRes, trace, context, true);
    }

    @SuppressWarnings("try")
    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, C context, boolean dumpTrace) {
        DebugContext debug = lirGenRes.getLIR().getDebug();
        try (DebugContext.Scope s = debug.scope(getName(), this)) {
            try (DebugCloseable a = timer.start(debug); DebugCloseable c = memUseTracker.start(debug)) {
                if (dumpTrace) {
                    if (debug.isDumpEnabled(DebugContext.DETAILED_LEVEL)) {
                        debug.dump(DebugContext.DETAILED_LEVEL, trace, "Before %s (Trace%s: %s)", getName(), trace.getId(), trace);
                    }
                }
                run(target, lirGenRes, trace, context);
                allocatedTraces.increment(debug);
                if (dumpTrace) {
                    if (debug.isDumpEnabled(DebugContext.VERBOSE_LEVEL)) {
                        debug.dump(DebugContext.VERBOSE_LEVEL, trace, "After %s (Trace%s: %s)", getName(), trace.getId(), trace);
                    }
                }
            }
        } catch (Throwable e) {
            throw debug.handle(e);
        }
    }

    protected abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, C context);
}
