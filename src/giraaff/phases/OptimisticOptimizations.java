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
    public static final OptimisticOptimizations ALL = new OptimisticOptimizations(EnumSet.allOf(Optimization.class));
    // @def
    public static final OptimisticOptimizations NONE = new OptimisticOptimizations(EnumSet.noneOf(Optimization.class));

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
    private final Set<Optimization> ___enabledOpts;

    // @cons
    public OptimisticOptimizations(ProfilingInfo __info)
    {
        super();
        this.___enabledOpts = EnumSet.noneOf(Optimization.class);

        this.___enabledOpts.add(Optimization.UseExceptionProbabilityForOperations);
        addOptimization(__info, DeoptimizationReason.UnreachedCode, Optimization.RemoveNeverExecutedCode);
        addOptimization(__info, DeoptimizationReason.TypeCheckedInliningViolated, Optimization.UseTypeCheckedInlining);
        addOptimization(__info, DeoptimizationReason.OptimizedTypeCheckViolated, Optimization.UseTypeCheckHints);
        addOptimization(__info, DeoptimizationReason.NotCompiledExceptionHandler, Optimization.UseExceptionProbability);
        addOptimization(__info, DeoptimizationReason.LoopLimitCheck, Optimization.UseLoopLimitChecks);
    }

    private void addOptimization(ProfilingInfo __info, DeoptimizationReason __deoptReason, Optimization __optimization)
    {
        if (checkDeoptimizations(__info, __deoptReason))
        {
            this.___enabledOpts.add(__optimization);
        }
    }

    public OptimisticOptimizations remove(Optimization... __optimizations)
    {
        Set<Optimization> __newOptimizations = EnumSet.copyOf(this.___enabledOpts);
        for (Optimization __o : __optimizations)
        {
            __newOptimizations.remove(__o);
        }
        return new OptimisticOptimizations(__newOptimizations);
    }

    public OptimisticOptimizations add(Optimization... __optimizations)
    {
        Set<Optimization> __newOptimizations = EnumSet.copyOf(this.___enabledOpts);
        for (Optimization __o : __optimizations)
        {
            __newOptimizations.add(__o);
        }
        return new OptimisticOptimizations(__newOptimizations);
    }

    // @cons
    private OptimisticOptimizations(Set<Optimization> __enabledOpts)
    {
        super();
        this.___enabledOpts = __enabledOpts;
    }

    public boolean removeNeverExecutedCode()
    {
        return GraalOptions.removeNeverExecutedCode && this.___enabledOpts.contains(Optimization.RemoveNeverExecutedCode);
    }

    public boolean useTypeCheckHints()
    {
        return GraalOptions.useTypeCheckHints && this.___enabledOpts.contains(Optimization.UseTypeCheckHints);
    }

    public boolean inlineMonomorphicCalls()
    {
        return GraalOptions.inlineMonomorphicCalls && this.___enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean inlinePolymorphicCalls()
    {
        return GraalOptions.inlinePolymorphicCalls && this.___enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean inlineMegamorphicCalls()
    {
        return GraalOptions.inlineMegamorphicCalls && this.___enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean devirtualizeInvokes()
    {
        return GraalOptions.optDevirtualizeInvokesOptimistically && this.___enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean useExceptionProbability()
    {
        return GraalOptions.useExceptionProbability && this.___enabledOpts.contains(Optimization.UseExceptionProbability);
    }

    public boolean useExceptionProbabilityForOperations()
    {
        return this.___enabledOpts.contains(Optimization.UseExceptionProbabilityForOperations);
    }

    public boolean useLoopLimitChecks()
    {
        return GraalOptions.useLoopLimitChecks && this.___enabledOpts.contains(Optimization.UseLoopLimitChecks);
    }

    public boolean lessOptimisticThan(OptimisticOptimizations __other)
    {
        for (Optimization __opt : Optimization.values())
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
