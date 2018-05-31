package giraaff.phases;

import java.util.EnumSet;
import java.util.Set;

import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ProfilingInfo;

import giraaff.core.common.GraalOptions;

// @class OptimisticOptimizations
public final class OptimisticOptimizations
{
    public static final OptimisticOptimizations ALL = new OptimisticOptimizations(EnumSet.allOf(Optimization.class));
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

    private final Set<Optimization> enabledOpts;

    // @cons
    public OptimisticOptimizations(ProfilingInfo info)
    {
        super();
        this.enabledOpts = EnumSet.noneOf(Optimization.class);

        enabledOpts.add(Optimization.UseExceptionProbabilityForOperations);
        addOptimization(info, DeoptimizationReason.UnreachedCode, Optimization.RemoveNeverExecutedCode);
        addOptimization(info, DeoptimizationReason.TypeCheckedInliningViolated, Optimization.UseTypeCheckedInlining);
        addOptimization(info, DeoptimizationReason.OptimizedTypeCheckViolated, Optimization.UseTypeCheckHints);
        addOptimization(info, DeoptimizationReason.NotCompiledExceptionHandler, Optimization.UseExceptionProbability);
        addOptimization(info, DeoptimizationReason.LoopLimitCheck, Optimization.UseLoopLimitChecks);
    }

    private void addOptimization(ProfilingInfo info, DeoptimizationReason deoptReason, Optimization optimization)
    {
        if (checkDeoptimizations(info, deoptReason))
        {
            enabledOpts.add(optimization);
        }
    }

    public OptimisticOptimizations remove(Optimization... optimizations)
    {
        Set<Optimization> newOptimizations = EnumSet.copyOf(enabledOpts);
        for (Optimization o : optimizations)
        {
            newOptimizations.remove(o);
        }
        return new OptimisticOptimizations(newOptimizations);
    }

    public OptimisticOptimizations add(Optimization... optimizations)
    {
        Set<Optimization> newOptimizations = EnumSet.copyOf(enabledOpts);
        for (Optimization o : optimizations)
        {
            newOptimizations.add(o);
        }
        return new OptimisticOptimizations(newOptimizations);
    }

    // @cons
    private OptimisticOptimizations(Set<Optimization> enabledOpts)
    {
        super();
        this.enabledOpts = enabledOpts;
    }

    public boolean removeNeverExecutedCode()
    {
        return GraalOptions.removeNeverExecutedCode && enabledOpts.contains(Optimization.RemoveNeverExecutedCode);
    }

    public boolean useTypeCheckHints()
    {
        return GraalOptions.useTypeCheckHints && enabledOpts.contains(Optimization.UseTypeCheckHints);
    }

    public boolean inlineMonomorphicCalls()
    {
        return GraalOptions.inlineMonomorphicCalls && enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean inlinePolymorphicCalls()
    {
        return GraalOptions.inlinePolymorphicCalls && enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean inlineMegamorphicCalls()
    {
        return GraalOptions.inlineMegamorphicCalls && enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean devirtualizeInvokes()
    {
        return GraalOptions.optDevirtualizeInvokesOptimistically && enabledOpts.contains(Optimization.UseTypeCheckedInlining);
    }

    public boolean useExceptionProbability()
    {
        return GraalOptions.useExceptionProbability && enabledOpts.contains(Optimization.UseExceptionProbability);
    }

    public boolean useExceptionProbabilityForOperations()
    {
        return enabledOpts.contains(Optimization.UseExceptionProbabilityForOperations);
    }

    public boolean useLoopLimitChecks()
    {
        return GraalOptions.useLoopLimitChecks && enabledOpts.contains(Optimization.UseLoopLimitChecks);
    }

    public boolean lessOptimisticThan(OptimisticOptimizations other)
    {
        for (Optimization opt : Optimization.values())
        {
            if (!enabledOpts.contains(opt) && other.enabledOpts.contains(opt))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean checkDeoptimizations(ProfilingInfo profilingInfo, DeoptimizationReason reason)
    {
        return profilingInfo.getDeoptimizationCount(reason) < GraalOptions.deoptsToDisableOptimisticOptimization;
    }
}
