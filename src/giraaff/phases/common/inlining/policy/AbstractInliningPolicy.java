package giraaff.phases.common.inlining.policy;

import java.util.Map;

import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.InliningPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.info.elem.Inlineable;

// @class AbstractInliningPolicy
public abstract class AbstractInliningPolicy implements InliningPolicy
{
    // @def
    public static final float RelevanceCapForInlining = 1.0f;
    // @def
    public static final float CapInheritedRelevance = 1.0f;
    // @field
    protected final Map<Invoke, Double> hints;

    // @cons
    public AbstractInliningPolicy(Map<Invoke, Double> __hints)
    {
        super();
        this.hints = __hints;
    }

    protected double computeMaximumSize(double __relevance, int __configuredMaximum)
    {
        double __inlineRatio = Math.min(RelevanceCapForInlining, __relevance);
        return __configuredMaximum * __inlineRatio;
    }

    protected double getInliningBonus(InlineInfo __info)
    {
        if (hints != null && hints.containsKey(__info.invoke()))
        {
            return hints.get(__info.invoke());
        }
        return 1;
    }

    protected boolean isIntrinsic(Replacements __replacements, InlineInfo __info)
    {
        if (GraalOptions.alwaysInlineIntrinsics)
        {
            return onlyIntrinsics(__replacements, __info);
        }
        else
        {
            return onlyForcedIntrinsics(__replacements, __info);
        }
    }

    private static boolean onlyIntrinsics(Replacements __replacements, InlineInfo __info)
    {
        for (int __i = 0; __i < __info.numberOfMethods(); __i++)
        {
            if (!InliningUtil.canIntrinsify(__replacements, __info.methodAt(__i), __info.invoke().bci()))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean onlyForcedIntrinsics(Replacements __replacements, InlineInfo __info)
    {
        if (!onlyIntrinsics(__replacements, __info))
        {
            return false;
        }
        if (!__info.shouldInline())
        {
            return false;
        }
        return true;
    }

    protected static int previousLowLevelGraphSize(InlineInfo __info)
    {
        int __size = 0;
        for (int __i = 0; __i < __info.numberOfMethods(); __i++)
        {
            ResolvedJavaMethod __m = __info.methodAt(__i);
            ProfilingInfo __profile = __info.graph().getProfilingInfo(__m);
            int __compiledGraphSize = __profile.getCompilerIRSize(StructuredGraph.class);
            if (__compiledGraphSize > 0)
            {
                __size += __compiledGraphSize;
            }
        }
        return __size;
    }

    protected static double determineInvokeProbability(InlineInfo __info)
    {
        double __invokeProbability = 0;
        for (int __i = 0; __i < __info.numberOfMethods(); __i++)
        {
            Inlineable __callee = __info.inlineableElementAt(__i);
            Iterable<Invoke> __invokes = __callee.getInvokes();
            if (__invokes.iterator().hasNext())
            {
                for (Invoke __invoke : __invokes)
                {
                    __invokeProbability += __callee.getProbability(__invoke);
                }
            }
        }
        return __invokeProbability;
    }
}
