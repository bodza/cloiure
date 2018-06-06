package giraaff.phases;

import java.util.EnumSet;
import java.util.Set;

import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ProfilingInfo;

import giraaff.core.common.GraalOptions;

// @class OptimisticOptimizations
public final class OptimisticOptimizations
{
    // @def
    public static final OptimisticOptimizations ALL = new OptimisticOptimizations(EnumSet.allOf(OptimisticOptimizations.Optimization.class));
    // @def
    public static final OptimisticOptimizations NONE = new OptimisticOptimizations(EnumSet.noneOf(OptimisticOptimizations.Optimization.class));

    // @enum OptimisticOptimizations.Optimization
    public enum Optimization
    {
        RemoveNeverExecutedCode,
        UseTypeCheckedInlining,
        UseTypeCheckHints,
        UseExceptionProbabilityForOperations,
        UseExceptionProbability,
        UseLoopLimitChecks
    }

    // @field
    private final Set<OptimisticOptimizations.Optimization> ___enabledOpts;

    // @cons OptimisticOptimizations
    public OptimisticOptimizations(ProfilingInfo __info)
    {
        super();
        this.___enabledOpts = EnumSet.noneOf(OptimisticOptimizations.Optimization.class);

        this.___enabledOpts.add(OptimisticOptimizations.Optimization.UseExceptionProbabilityForOperations);
        addOptimization(__info, DeoptimizationReason.UnreachedCode, OptimisticOptimizations.Optimization.RemoveNeverExecutedCode);
        addOptimization(__info, DeoptimizationReason.TypeCheckedInliningViolated, OptimisticOptimizations.Optimization.UseTypeCheckedInlining);
        addOptimization(__info, DeoptimizationReason.OptimizedTypeCheckViolated, OptimisticOptimizations.Optimization.UseTypeCheckHints);
        addOptimization(__info, DeoptimizationReason.NotCompiledExceptionHandler, OptimisticOptimizations.Optimization.UseExceptionProbability);
        addOptimization(__info, DeoptimizationReason.LoopLimitCheck, OptimisticOptimizations.Optimization.UseLoopLimitChecks);
    }

    private void addOptimization(ProfilingInfo __info, DeoptimizationReason __deoptReason, OptimisticOptimizations.Optimization __optimization)
    {
        if (checkDeoptimizations(__info, __deoptReason))
        {
            this.___enabledOpts.add(__optimization);
        }
    }

    public OptimisticOptimizations remove(OptimisticOptimizations.Optimization... __optimizations)
    {
        Set<OptimisticOptimizations.Optimization> __newOptimizations = EnumSet.copyOf(this.___enabledOpts);
        for (OptimisticOptimizations.Optimization __o : __optimizations)
        {
            __newOptimizations.remove(__o);
        }
        return new OptimisticOptimizations(__newOptimizations);
    }

    public OptimisticOptimizations add(OptimisticOptimizations.Optimization... __optimizations)
    {
        Set<OptimisticOptimizations.Optimization> __newOptimizations = EnumSet.copyOf(this.___enabledOpts);
        for (OptimisticOptimizations.Optimization __o : __optimizations)
        {
            __newOptimizations.add(__o);
        }
        return new OptimisticOptimizations(__newOptimizations);
    }

    // @cons OptimisticOptimizations
    private OptimisticOptimizations(Set<OptimisticOptimizations.Optimization> __enabledOpts)
    {
        super();
        this.___enabledOpts = __enabledOpts;
    }

    public boolean removeNeverExecutedCode()
    {
        return GraalOptions.removeNeverExecutedCode && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.RemoveNeverExecutedCode);
    }

    public boolean useTypeCheckHints()
    {
        return GraalOptions.useTypeCheckHints && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseTypeCheckHints);
    }

    public boolean inlineMonomorphicCalls()
    {
        return GraalOptions.inlineMonomorphicCalls && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseTypeCheckedInlining);
    }

    public boolean inlinePolymorphicCalls()
    {
        return GraalOptions.inlinePolymorphicCalls && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseTypeCheckedInlining);
    }

    public boolean inlineMegamorphicCalls()
    {
        return GraalOptions.inlineMegamorphicCalls && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseTypeCheckedInlining);
    }

    public boolean devirtualizeInvokes()
    {
        return GraalOptions.optDevirtualizeInvokesOptimistically && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseTypeCheckedInlining);
    }

    public boolean useExceptionProbability()
    {
        return GraalOptions.useExceptionProbability && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseExceptionProbability);
    }

    public boolean useExceptionProbabilityForOperations()
    {
        return this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseExceptionProbabilityForOperations);
    }

    public boolean useLoopLimitChecks()
    {
        return GraalOptions.useLoopLimitChecks && this.___enabledOpts.contains(OptimisticOptimizations.Optimization.UseLoopLimitChecks);
    }

    public boolean lessOptimisticThan(OptimisticOptimizations __other)
    {
        for (OptimisticOptimizations.Optimization __opt : OptimisticOptimizations.Optimization.values())
        {
            if (!this.___enabledOpts.contains(__opt) && __other.___enabledOpts.contains(__opt))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean checkDeoptimizations(ProfilingInfo __profilingInfo, DeoptimizationReason __reason)
    {
        return __profilingInfo.getDeoptimizationCount(__reason) < GraalOptions.deoptsToDisableOptimisticOptimization;
    }
}
