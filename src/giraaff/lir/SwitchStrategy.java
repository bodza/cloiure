package giraaff.lir;

import java.util.Arrays;
import java.util.Comparator;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.asm.Assembler;
import giraaff.asm.Label;
import giraaff.core.common.calc.Condition;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * This class encapsulates different strategies on how to generate code for switch instructions.
 *
 * The {@link #getBestStrategy(double[], JavaConstant[], LabelRef[])} method can be used to get
 * strategy with the smallest average effort (average number of comparisons until a decision is
 * reached). The strategy returned by this method will have its averageEffort set, while a strategy
 * constructed directly will not.
 */
// @class SwitchStrategy
public abstract class SwitchStrategy
{
    // @iface SwitchStrategy.SwitchClosure
    private interface SwitchClosure
    {
        /**
         * Generates a conditional or unconditional jump. The jump will be unconditional if
         * condition is null. If defaultTarget is true, then the jump will go the default.
         *
         * @param index Index of the value and the jump target (only used if defaultTarget == false)
         * @param condition The condition on which to jump (can be null)
         * @param defaultTarget true if the jump should go to the default target, false if index
         *            should be used.
         */
        void conditionalJump(int index, Condition condition, boolean defaultTarget);

        /**
         * Generates a conditional jump to the target with the specified index. The fall through
         * should go to the default target.
         *
         * @param index Index of the value and the jump target
         * @param condition The condition on which to jump
         * @param canFallThrough true if this is the last instruction in the switch statement, to
         *            allow for fall-through optimizations.
         */
        void conditionalJumpOrDefault(int index, Condition condition, boolean canFallThrough);

        /**
         * Create a new label and generate a conditional jump to it.
         *
         * @param index Index of the value and the jump target
         * @param condition The condition on which to jump
         * @return a new Label
         */
        Label conditionalJump(int index, Condition condition);

        /**
         * Binds a label returned by {@link #conditionalJump(int, Condition)}.
         */
        void bind(Label label);

        /**
         * Return true iff the target of both indexes is the same.
         */
        boolean isSameTarget(int index1, int index2);
    }

    /**
     * Backends can subclass this abstract class and generate code for switch strategies by
     * implementing the {@link #conditionalJump(int, Condition, Label)} method.
     */
    // @class SwitchStrategy.BaseSwitchClosure
    public abstract static class BaseSwitchClosure implements SwitchClosure
    {
        // @field
        private final CompilationResultBuilder crb;
        // @field
        private final Assembler masm;
        // @field
        private final LabelRef[] keyTargets;
        // @field
        private final LabelRef defaultTarget;

        // @cons
        public BaseSwitchClosure(CompilationResultBuilder __crb, Assembler __masm, LabelRef[] __keyTargets, LabelRef __defaultTarget)
        {
            super();
            this.crb = __crb;
            this.masm = __masm;
            this.keyTargets = __keyTargets;
            this.defaultTarget = __defaultTarget;
        }

        /**
         * This method generates code for a comparison between the actual value and the constant at
         * the given index and a condition jump to target.
         */
        protected abstract void conditionalJump(int index, Condition condition, Label target);

        @Override
        public void conditionalJump(int __index, Condition __condition, boolean __targetDefault)
        {
            Label __target = __targetDefault ? defaultTarget.label() : keyTargets[__index].label();
            if (__condition == null)
            {
                masm.jmp(__target);
            }
            else
            {
                conditionalJump(__index, __condition, __target);
            }
        }

        @Override
        public void conditionalJumpOrDefault(int __index, Condition __condition, boolean __canFallThrough)
        {
            if (__canFallThrough && crb.isSuccessorEdge(defaultTarget))
            {
                conditionalJump(__index, __condition, keyTargets[__index].label());
            }
            else if (__canFallThrough && crb.isSuccessorEdge(keyTargets[__index]))
            {
                conditionalJump(__index, __condition.negate(), defaultTarget.label());
            }
            else
            {
                conditionalJump(__index, __condition, keyTargets[__index].label());
                masm.jmp(defaultTarget.label());
            }
        }

        @Override
        public Label conditionalJump(int __index, Condition __condition)
        {
            Label __label = new Label();
            conditionalJump(__index, __condition, __label);
            return __label;
        }

        @Override
        public void bind(Label __label)
        {
            masm.bind(__label);
        }

        @Override
        public boolean isSameTarget(int __index1, int __index2)
        {
            return keyTargets[__index1] == keyTargets[__index2];
        }
    }

    /**
     * This closure is used internally to determine the average effort for a certain strategy on a
     * given switch instruction.
     */
    // @class SwitchStrategy.EffortClosure
    // @closure
    private final class EffortClosure implements SwitchClosure
    {
        // @field
        private int defaultEffort;
        // @field
        private int defaultCount;
        // @field
        private final int[] keyEfforts = new int[SwitchStrategy.this.keyProbabilities.length];
        // @field
        private final int[] keyCounts = new int[SwitchStrategy.this.keyProbabilities.length];
        // @field
        private final LabelRef[] keyTargets;

        // @cons
        EffortClosure(LabelRef[] __keyTargets)
        {
            super();
            this.keyTargets = __keyTargets;
        }

        @Override
        public void conditionalJump(int __index, Condition __condition, boolean __defaultTarget)
        {
            // nothing to do
        }

        @Override
        public void conditionalJumpOrDefault(int __index, Condition __condition, boolean __canFallThrough)
        {
            // nothing to do
        }

        @Override
        public Label conditionalJump(int __index, Condition __condition)
        {
            // nothing to do
            return null;
        }

        @Override
        public void bind(Label __label)
        {
            // nothing to do
        }

        @Override
        public boolean isSameTarget(int __index1, int __index2)
        {
            return keyTargets[__index1] == keyTargets[__index2];
        }

        public double getAverageEffort()
        {
            double __defaultProbability = 1;
            double __effort = 0;
            for (int __i = 0; __i < SwitchStrategy.this.keyProbabilities.length; __i++)
            {
                __effort += keyEfforts[__i] * SwitchStrategy.this.keyProbabilities[__i] / keyCounts[__i];
                __defaultProbability -= SwitchStrategy.this.keyProbabilities[__i];
            }
            return __effort + defaultEffort * __defaultProbability / defaultCount;
        }
    }

    // @field
    public final double[] keyProbabilities;
    // @field
    private double averageEffort = -1;
    // @field
    private EffortClosure effortClosure;

    // @cons
    public SwitchStrategy(double[] __keyProbabilities)
    {
        super();
        this.keyProbabilities = __keyProbabilities;
    }

    public abstract Constant[] getKeyConstants();

    public double getAverageEffort()
    {
        return averageEffort;
    }

    /**
     * Tells the system that the given (inclusive) range of keys is reached after depth number
     * of comparisons, which is used to calculate the average effort.
     */
    protected void registerEffort(int __rangeStart, int __rangeEnd, int __depth)
    {
        if (effortClosure != null)
        {
            for (int __i = __rangeStart; __i <= __rangeEnd; __i++)
            {
                effortClosure.keyEfforts[__i] += __depth;
                effortClosure.keyCounts[__i]++;
            }
        }
    }

    /**
     * Tells the system that the default successor is reached after depth number of comparisons,
     * which is used to calculate average effort.
     */
    protected void registerDefaultEffort(int __depth)
    {
        if (effortClosure != null)
        {
            effortClosure.defaultEffort += __depth;
            effortClosure.defaultCount++;
        }
    }

    /**
     * This strategy orders the keys according to their probability and creates one equality
     * comparison per key.
     */
    // @class SwitchStrategy.SequentialStrategy
    public static final class SequentialStrategy extends SwitchStrategy
    {
        // @field
        private final Integer[] indexes;
        // @field
        private final Constant[] keyConstants;

        // @cons
        public SequentialStrategy(final double[] __keyProbabilities, Constant[] __keyConstants)
        {
            super(__keyProbabilities);

            this.keyConstants = __keyConstants;
            int __keyCount = __keyConstants.length;
            indexes = new Integer[__keyCount];
            for (int __i = 0; __i < __keyCount; __i++)
            {
                indexes[__i] = __i;
            }
            // @closure
            Arrays.sort(indexes, new Comparator<Integer>()
            {
                @Override
                public int compare(Integer __o1, Integer __o2)
                {
                    return __keyProbabilities[__o1] < __keyProbabilities[__o2] ? 1 : __keyProbabilities[__o1] > __keyProbabilities[__o2] ? -1 : 0;
                }
            });
        }

        @Override
        public Constant[] getKeyConstants()
        {
            return keyConstants;
        }

        @Override
        public void run(SwitchClosure __closure)
        {
            for (int __i = 0; __i < keyConstants.length - 1; __i++)
            {
                __closure.conditionalJump(indexes[__i], Condition.EQ, false);
                registerEffort(indexes[__i], indexes[__i], __i + 1);
            }
            __closure.conditionalJumpOrDefault(indexes[keyConstants.length - 1], Condition.EQ, true);
            registerEffort(indexes[keyConstants.length - 1], indexes[keyConstants.length - 1], keyConstants.length);
            registerDefaultEffort(keyConstants.length);
        }
    }

    /**
     * Base class for strategies that rely on primitive integer keys.
     */
    // @class SwitchStrategy.PrimitiveStrategy
    private abstract static class PrimitiveStrategy extends SwitchStrategy
    {
        // @field
        protected final JavaConstant[] keyConstants;

        // @cons
        protected PrimitiveStrategy(double[] __keyProbabilities, JavaConstant[] __keyConstants)
        {
            super(__keyProbabilities);
            this.keyConstants = __keyConstants;
        }

        @Override
        public JavaConstant[] getKeyConstants()
        {
            return keyConstants;
        }

        /**
         * Looks for the end of a stretch of key constants that are successive numbers and have the
         * same target.
         */
        protected int getSliceEnd(SwitchClosure __closure, int __pos)
        {
            int __slice = __pos;
            while (__slice < (keyConstants.length - 1) && keyConstants[__slice + 1].asLong() == keyConstants[__slice].asLong() + 1 && __closure.isSameTarget(__slice, __slice + 1))
            {
                __slice++;
            }
            return __slice;
        }
    }

    /**
     * This strategy divides the keys into ranges of successive keys with the same target and
     * creates comparisons for these ranges.
     */
    // @class SwitchStrategy.RangesStrategy
    public static final class RangesStrategy extends PrimitiveStrategy
    {
        // @field
        private final Integer[] indexes;

        // @cons
        public RangesStrategy(final double[] __keyProbabilities, JavaConstant[] __keyConstants)
        {
            super(__keyProbabilities, __keyConstants);

            int __keyCount = __keyConstants.length;
            indexes = new Integer[__keyCount];
            for (int __i = 0; __i < __keyCount; __i++)
            {
                indexes[__i] = __i;
            }
            // @closure
            Arrays.sort(indexes, new Comparator<Integer>()
            {
                @Override
                public int compare(Integer __o1, Integer __o2)
                {
                    return __keyProbabilities[__o1] < __keyProbabilities[__o2] ? 1 : __keyProbabilities[__o1] > __keyProbabilities[__o2] ? -1 : 0;
                }
            });
        }

        @Override
        public void run(SwitchClosure __closure)
        {
            int __depth = 0;
            __closure.conditionalJump(0, Condition.LT, true);
            registerDefaultEffort(++__depth);
            int __rangeStart = 0;
            int __rangeEnd = getSliceEnd(__closure, __rangeStart);
            while (__rangeEnd != keyConstants.length - 1)
            {
                if (__rangeStart == __rangeEnd)
                {
                    __closure.conditionalJump(__rangeStart, Condition.EQ, false);
                    registerEffort(__rangeStart, __rangeEnd, ++__depth);
                }
                else
                {
                    if (__rangeStart == 0 || keyConstants[__rangeStart - 1].asLong() + 1 != keyConstants[__rangeStart].asLong())
                    {
                        __closure.conditionalJump(__rangeStart, Condition.LT, true);
                        registerDefaultEffort(++__depth);
                    }
                    __closure.conditionalJump(__rangeEnd, Condition.LE, false);
                    registerEffort(__rangeStart, __rangeEnd, ++__depth);
                }
                __rangeStart = __rangeEnd + 1;
                __rangeEnd = getSliceEnd(__closure, __rangeStart);
            }
            if (__rangeStart == __rangeEnd)
            {
                __closure.conditionalJumpOrDefault(__rangeStart, Condition.EQ, true);
                registerEffort(__rangeStart, __rangeEnd, ++__depth);
                registerDefaultEffort(__depth);
            }
            else
            {
                if (__rangeStart == 0 || keyConstants[__rangeStart - 1].asLong() + 1 != keyConstants[__rangeStart].asLong())
                {
                    __closure.conditionalJump(__rangeStart, Condition.LT, true);
                    registerDefaultEffort(++__depth);
                }
                __closure.conditionalJumpOrDefault(__rangeEnd, Condition.LE, true);
                registerEffort(__rangeStart, __rangeEnd, ++__depth);
                registerDefaultEffort(__depth);
            }
        }
    }

    /**
     * This strategy recursively subdivides the list of keys to create a binary search based on probabilities.
     */
    // @class SwitchStrategy.BinaryStrategy
    public static final class BinaryStrategy extends PrimitiveStrategy
    {
        // @def
        private static final double MIN_PROBABILITY = 0.00001;

        // @field
        private final double[] probabilitySums;

        // @cons
        public BinaryStrategy(double[] __keyProbabilities, JavaConstant[] __keyConstants)
        {
            super(__keyProbabilities, __keyConstants);
            probabilitySums = new double[__keyProbabilities.length + 1];
            double __sum = 0;
            for (int __i = 0; __i < __keyConstants.length; __i++)
            {
                __sum += Math.max(__keyProbabilities[__i], MIN_PROBABILITY);
                probabilitySums[__i + 1] = __sum;
            }
        }

        @Override
        public void run(SwitchClosure __closure)
        {
            recurseBinarySwitch(__closure, 0, keyConstants.length - 1, 0);
        }

        /**
         * Recursively generate a list of comparisons that always subdivides the keys in the given
         * (inclusive) range in the middle (in terms of probability, not index). If left is bigger
         * than zero, then we always know that the value is equal to or bigger than the left key.
         * This does not hold for the right key, as there may be a gap afterwards.
         */
        private void recurseBinarySwitch(SwitchClosure __closure, int __left, int __right, int __startDepth)
        {
            int __depth = __startDepth;
            boolean __leftBorder = __left == 0;
            boolean __rightBorder = __right == keyConstants.length - 1;

            if (__left + 1 == __right)
            {
                // only two possible values
                if (__leftBorder || __rightBorder || keyConstants[__right].asLong() + 1 != keyConstants[__right + 1].asLong() || keyConstants[__left].asLong() + 1 != keyConstants[__right].asLong())
                {
                    __closure.conditionalJump(__left, Condition.EQ, false);
                    registerEffort(__left, __left, ++__depth);
                    __closure.conditionalJumpOrDefault(__right, Condition.EQ, __rightBorder);
                    registerEffort(__right, __right, ++__depth);
                    registerDefaultEffort(__depth);
                }
                else
                {
                    // here we know that the value can only be one of these two keys in the range
                    __closure.conditionalJump(__left, Condition.EQ, false);
                    registerEffort(__left, __left, ++__depth);
                    __closure.conditionalJump(__right, null, false);
                    registerEffort(__right, __right, __depth);
                }
                return;
            }
            double __probabilityStart = probabilitySums[__left];
            double __probabilityMiddle = (__probabilityStart + probabilitySums[__right + 1]) / 2;
            int __middle = __left;
            while (getSliceEnd(__closure, __middle + 1) < __right && probabilitySums[getSliceEnd(__closure, __middle + 1)] < __probabilityMiddle)
            {
                __middle = getSliceEnd(__closure, __middle + 1);
            }
            __middle = getSliceEnd(__closure, __middle);

            if (getSliceEnd(__closure, __left) == __middle)
            {
                if (__left == 0)
                {
                    __closure.conditionalJump(0, Condition.LT, true);
                    registerDefaultEffort(++__depth);
                }
                __closure.conditionalJump(__middle, Condition.LE, false);
                registerEffort(__left, __middle, ++__depth);

                if (__middle + 1 == __right)
                {
                    __closure.conditionalJumpOrDefault(__right, Condition.EQ, __rightBorder);
                    registerEffort(__right, __right, ++__depth);
                    registerDefaultEffort(__depth);
                }
                else
                {
                    if (keyConstants[__middle].asLong() + 1 != keyConstants[__middle + 1].asLong())
                    {
                        __closure.conditionalJump(__middle + 1, Condition.LT, true);
                        registerDefaultEffort(++__depth);
                    }
                    if (getSliceEnd(__closure, __middle + 1) == __right)
                    {
                        if (__right == keyConstants.length - 1 || keyConstants[__right].asLong() + 1 != keyConstants[__right + 1].asLong())
                        {
                            __closure.conditionalJumpOrDefault(__right, Condition.LE, __rightBorder);
                            registerEffort(__middle + 1, __right, ++__depth);
                            registerDefaultEffort(__depth);
                        }
                        else
                        {
                            __closure.conditionalJump(__middle + 1, null, false);
                            registerEffort(__middle + 1, __right, __depth);
                        }
                    }
                    else
                    {
                        recurseBinarySwitch(__closure, __middle + 1, __right, __depth);
                    }
                }
            }
            else if (getSliceEnd(__closure, __middle + 1) == __right)
            {
                if (__rightBorder || keyConstants[__right].asLong() + 1 != keyConstants[__right + 1].asLong())
                {
                    __closure.conditionalJump(__right, Condition.GT, true);
                    registerDefaultEffort(++__depth);
                }
                __closure.conditionalJump(__middle + 1, Condition.GE, false);
                registerEffort(__middle + 1, __right, ++__depth);
                recurseBinarySwitch(__closure, __left, __middle, __depth);
            }
            else
            {
                Label __label = __closure.conditionalJump(__middle + 1, Condition.GE);
                __depth++;
                recurseBinarySwitch(__closure, __left, __middle, __depth);
                __closure.bind(__label);
                recurseBinarySwitch(__closure, __middle + 1, __right, __depth);
            }
        }
    }

    public abstract void run(SwitchClosure closure);

    private static SwitchStrategy[] getStrategies(double[] __keyProbabilities, JavaConstant[] __keyConstants, LabelRef[] __keyTargets)
    {
        SwitchStrategy[] __strategies = new SwitchStrategy[] { new SequentialStrategy(__keyProbabilities, __keyConstants), new RangesStrategy(__keyProbabilities, __keyConstants), new BinaryStrategy(__keyProbabilities, __keyConstants) };
        for (SwitchStrategy __strategy : __strategies)
        {
            __strategy.effortClosure = __strategy.new EffortClosure(__keyTargets);
            __strategy.run(__strategy.effortClosure);
            __strategy.averageEffort = __strategy.effortClosure.getAverageEffort();
            __strategy.effortClosure = null;
        }
        return __strategies;
    }

    /**
     * Creates all switch strategies for the given switch, evaluates them (based on average effort)
     * and returns the best one.
     */
    public static SwitchStrategy getBestStrategy(double[] __keyProbabilities, JavaConstant[] __keyConstants, LabelRef[] __keyTargets)
    {
        SwitchStrategy[] __strategies = getStrategies(__keyProbabilities, __keyConstants, __keyTargets);
        double __bestEffort = Integer.MAX_VALUE;
        SwitchStrategy __bestStrategy = null;
        for (SwitchStrategy __strategy : __strategies)
        {
            if (__strategy.getAverageEffort() < __bestEffort)
            {
                __bestEffort = __strategy.getAverageEffort();
                __bestStrategy = __strategy;
            }
        }
        return __bestStrategy;
    }
}
