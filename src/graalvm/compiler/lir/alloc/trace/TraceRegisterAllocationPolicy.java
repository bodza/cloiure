package graalvm.compiler.lir.alloc.trace;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Manages the selection of allocation strategies.
 */
public final class TraceRegisterAllocationPolicy
{
    protected abstract class AllocationStrategy
    {
        TraceAllocationPhase<TraceAllocationContext> allocator;

        public final TraceAllocationPhase<TraceAllocationContext> getAllocator()
        {
            if (allocator == null)
            {
                allocator = initAllocator(target, lirGenRes, spillMoveFactory, registerAllocationConfig, cachedStackSlots, resultTraces, neverSpillConstants, livenessInfo, strategies);
            }
            return allocator;
        }

        protected final LIR getLIR()
        {
            return lirGenRes.getLIR();
        }

        protected final LIRGenerationResult getLIRGenerationResult()
        {
            return lirGenRes;
        }

        protected final TraceBuilderResult getTraceBuilderResult()
        {
            return resultTraces;
        }

        protected final GlobalLivenessInfo getGlobalLivenessInfo()
        {
            return livenessInfo;
        }

        protected final RegisterAllocationConfig getRegisterAllocationConfig()
        {
            return registerAllocationConfig;
        }

        protected final TargetDescription getTarget()
        {
            return target;
        }

        /**
         * Returns {@code true} if the allocation strategy should be used for {@code trace}.
         *
         * This method must not alter any state of the strategy, nor rely on being called in a
         * specific trace order.
         */
        public abstract boolean shouldApplyTo(Trace trace);

        @SuppressWarnings("hiding")
        protected abstract TraceAllocationPhase<TraceAllocationContext> initAllocator(TargetDescription target, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, AllocatableValue[] cachedStackSlots, TraceBuilderResult resultTraces, boolean neverSpillConstant, GlobalLivenessInfo livenessInfo, ArrayList<AllocationStrategy> strategies);
    }

    private final TargetDescription target;
    private final LIRGenerationResult lirGenRes;
    private final MoveFactory spillMoveFactory;
    private final RegisterAllocationConfig registerAllocationConfig;
    private final AllocatableValue[] cachedStackSlots;
    private final TraceBuilderResult resultTraces;
    private final boolean neverSpillConstants;
    private final GlobalLivenessInfo livenessInfo;

    private final ArrayList<AllocationStrategy> strategies;

    public TraceRegisterAllocationPolicy(TargetDescription target, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, AllocatableValue[] cachedStackSlots, TraceBuilderResult resultTraces, boolean neverSpillConstant, GlobalLivenessInfo livenessInfo)
    {
        this.target = target;
        this.lirGenRes = lirGenRes;
        this.spillMoveFactory = spillMoveFactory;
        this.registerAllocationConfig = registerAllocationConfig;
        this.cachedStackSlots = cachedStackSlots;
        this.resultTraces = resultTraces;
        this.neverSpillConstants = neverSpillConstant;
        this.livenessInfo = livenessInfo;

        this.strategies = new ArrayList<>(3);
    }

    protected OptionValues getOptions()
    {
        return lirGenRes.getLIR().getOptions();
    }

    public void appendStrategy(AllocationStrategy strategy)
    {
        strategies.add(strategy);
    }

    public TraceAllocationPhase<TraceAllocationContext> selectStrategy(Trace trace)
    {
        for (AllocationStrategy strategy : strategies)
        {
            if (strategy.shouldApplyTo(trace))
            {
                return strategy.getAllocator();
            }
        }
        throw JVMCIError.shouldNotReachHere("No Allocation Strategy found!");
    }
}
