package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.util.GraalError;

// @iface StaticDeoptimizingNode
public interface StaticDeoptimizingNode extends ValueNodeInterface
{
    DeoptimizationReason getReason();

    void setReason(DeoptimizationReason __reason);

    DeoptimizationAction getAction();

    void setAction(DeoptimizationAction __action);

    JavaConstant getSpeculation();

    ///
    // Describes how much information is gathered when deoptimization triggers.
    //
    // This enum is {@link Comparable} and orders its element from highest priority to lowest priority.
    ///
    // @enum StaticDeoptimizingNode.GuardPriority
    enum GuardPriority
    {
        Speculation,
        Profile,
        None;

        public boolean isHigherPriorityThan(StaticDeoptimizingNode.GuardPriority __other)
        {
            return this.compareTo(__other) < 0;
        }

        public boolean isLowerPriorityThan(StaticDeoptimizingNode.GuardPriority __other)
        {
            return this.compareTo(__other) > 0;
        }

        public static StaticDeoptimizingNode.GuardPriority highest()
        {
            return Speculation;
        }
    }

    default StaticDeoptimizingNode.GuardPriority computePriority()
    {
        if (getSpeculation() != null && getSpeculation().isNonNull())
        {
            return GuardNode.GuardPriority.Speculation;
        }
        switch (getAction())
        {
            case InvalidateReprofile:
            case InvalidateRecompile:
                return GuardNode.GuardPriority.Profile;
            case RecompileIfTooManyDeopts:
            case InvalidateStopCompiling:
            case None:
                return GuardNode.GuardPriority.None;
        }
        throw GraalError.shouldNotReachHere();
    }

    static DeoptimizationAction mergeActions(DeoptimizationAction __a1, DeoptimizationAction __a2)
    {
        if (__a1 == __a2)
        {
            return __a1;
        }
        if (__a1 == DeoptimizationAction.InvalidateRecompile && __a2 == DeoptimizationAction.InvalidateReprofile || __a1 == DeoptimizationAction.InvalidateReprofile && __a2 == DeoptimizationAction.InvalidateRecompile)
        {
            return DeoptimizationAction.InvalidateReprofile;
        }
        return null;
    }
}
