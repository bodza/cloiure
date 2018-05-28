package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.util.IntList;
import giraaff.core.common.util.Util;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Variable;
import giraaff.util.GraalError;

/**
 * Represents an interval in the {@linkplain LinearScan linear scan register allocator}.
 */
public final class Interval
{
    /**
     * A set of interval lists, one per {@linkplain RegisterBinding binding} type.
     */
    static final class RegisterBindingLists
    {
        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Fixed}.
         */
        public Interval fixed;

        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Any}.
         */
        public Interval any;

        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Stack}.
         */
        public Interval stack;

        RegisterBindingLists(Interval fixed, Interval any, Interval stack)
        {
            this.fixed = fixed;
            this.any = any;
            this.stack = stack;
        }

        /**
         * Gets the list for a specified binding.
         *
         * @param binding specifies the list to be returned
         * @return the list of intervals whose binding is {@code binding}
         */
        public Interval get(RegisterBinding binding)
        {
            switch (binding)
            {
                case Any:
                    return any;
                case Fixed:
                    return fixed;
                case Stack:
                    return stack;
            }
            throw GraalError.shouldNotReachHere();
        }

        /**
         * Sets the list for a specified binding.
         *
         * @param binding specifies the list to be replaced
         * @param list a list of intervals whose binding is {@code binding}
         */
        public void set(RegisterBinding binding, Interval list)
        {
            switch (binding)
            {
                case Any:
                    any = list;
                    break;
                case Fixed:
                    fixed = list;
                    break;
                case Stack:
                    stack = list;
                    break;
            }
        }

        /**
         * Adds an interval to a list sorted by {@linkplain Interval#currentFrom() current from} positions.
         *
         * @param binding specifies the list to be updated
         * @param interval the interval to add
         */
        public void addToListSortedByCurrentFromPositions(RegisterBinding binding, Interval interval)
        {
            Interval list = get(binding);
            Interval prev = null;
            Interval cur = list;
            while (cur.currentFrom() < interval.currentFrom())
            {
                prev = cur;
                cur = cur.next;
            }
            Interval result = list;
            if (prev == null)
            {
                // add to head of list
                result = interval;
            }
            else
            {
                // add before 'cur'
                prev.next = interval;
            }
            interval.next = cur;
            set(binding, result);
        }

        /**
         * Adds an interval to a list sorted by {@linkplain Interval#from() start} positions and
         * {@linkplain Interval#firstUsage(RegisterPriority) first usage} positions.
         *
         * @param binding specifies the list to be updated
         * @param interval the interval to add
         */
        public void addToListSortedByStartAndUsePositions(RegisterBinding binding, Interval interval)
        {
            Interval list = get(binding);
            Interval prev = null;
            Interval cur = list;
            while (cur.from() < interval.from() || (cur.from() == interval.from() && cur.firstUsage(RegisterPriority.None) < interval.firstUsage(RegisterPriority.None)))
            {
                prev = cur;
                cur = cur.next;
            }
            if (prev == null)
            {
                list = interval;
            }
            else
            {
                prev.next = interval;
            }
            interval.next = cur;
            set(binding, list);
        }

        /**
         * Removes an interval from a list.
         *
         * @param binding specifies the list to be updated
         * @param i the interval to remove
         */
        public void remove(RegisterBinding binding, Interval i)
        {
            Interval list = get(binding);
            Interval prev = null;
            Interval cur = list;
            while (cur != i)
            {
                prev = cur;
                cur = cur.next;
            }
            if (prev == null)
            {
                set(binding, cur.next);
            }
            else
            {
                prev.next = cur.next;
            }
        }
    }

    /**
     * Constants denoting the register usage priority for an interval. The constants are declared in
     * increasing order of priority are are used to optimize spilling when multiple overlapping
     * intervals compete for limited registers.
     */
    public enum RegisterPriority
    {
        /**
         * No special reason for an interval to be allocated a register.
         */
        None,

        /**
         * Priority level for intervals live at the end of a loop.
         */
        LiveAtLoopEnd,

        /**
         * Priority level for intervals that should be allocated to a register.
         */
        ShouldHaveRegister,

        /**
         * Priority level for intervals that must be allocated to a register.
         */
        MustHaveRegister;

        public static final RegisterPriority[] VALUES = values();

        /**
         * Determines if this priority is higher than or equal to a given priority.
         */
        public boolean greaterEqual(RegisterPriority other)
        {
            return ordinal() >= other.ordinal();
        }

        /**
         * Determines if this priority is lower than a given priority.
         */
        public boolean lessThan(RegisterPriority other)
        {
            return ordinal() < other.ordinal();
        }
    }

    /**
     * Constants denoting whether an interval is bound to a specific register. This models platform
     * dependencies on register usage for certain instructions.
     */
    enum RegisterBinding
    {
        /**
         * Interval is bound to a specific register as required by the platform.
         */
        Fixed,

        /**
         * Interval has no specific register requirements.
         */
        Any,

        /**
         * Interval is bound to a stack slot.
         */
        Stack;

        public static final RegisterBinding[] VALUES = values();
    }

    /**
     * Constants denoting the linear-scan states an interval may be in with respect to the
     * {@linkplain Interval#from() start} {@code position} of the interval being processed.
     */
    enum State
    {
        /**
         * An interval that starts after {@code position}.
         */
        Unhandled,

        /**
         * An interval that {@linkplain Interval#covers covers} {@code position} and has an assigned register.
         */
        Active,

        /**
         * An interval that starts before and ends after {@code position} but does not
         * {@linkplain Interval#covers cover} it due to a lifetime hole.
         */
        Inactive,

        /**
         * An interval that ends before {@code position} or is spilled to memory.
         */
        Handled;
    }

    /**
     * Constants used in optimization of spilling of an interval.
     */
    public enum SpillState
    {
        /**
         * Starting state of calculation: no definition found yet.
         */
        NoDefinitionFound,

        /**
         * One definition has already been found. Two consecutive definitions are treated as one
         * (e.g. a consecutive move and add because of two-operand LIR form). The position of this
         * definition is given by {@link Interval#spillDefinitionPos()}.
         */
        NoSpillStore,

        /**
         * One spill move has already been inserted.
         */
        OneSpillStore,

        /**
         * The interval is spilled multiple times or is spilled in a loop. Place the store somewhere
         * on the dominator path between the definition and the usages.
         */
        SpillInDominator,

        /**
         * The interval should be stored immediately after its definition to prevent multiple
         * redundant stores.
         */
        StoreAtDefinition,

        /**
         * The interval starts in memory (e.g. method parameter), so a store is never necessary.
         */
        StartInMemory,

        /**
         * The interval has more than one definition (e.g. resulting from phi moves), so stores to
         * memory are not optimized.
         */
        NoOptimization;

        public static final EnumSet<SpillState> ALWAYS_IN_MEMORY = EnumSet.of(SpillInDominator, StoreAtDefinition, StartInMemory);
    }

    /**
     * List of use positions. Each entry in the list records the use position and register priority
     * associated with the use position. The entries in the list are in descending order of use position.
     */
    public static final class UsePosList
    {
        private IntList list;

        /**
         * Creates a use list.
         *
         * @param initialCapacity the initial capacity of the list in terms of entries
         */
        public UsePosList(int initialCapacity)
        {
            list = new IntList(initialCapacity * 2);
        }

        private UsePosList(IntList list)
        {
            this.list = list;
        }

        /**
         * Splits this list around a given position. All entries in this list with a use position
         * greater or equal than {@code splitPos} are removed from this list and added to the
         * returned list.
         *
         * @param splitPos the position for the split
         * @return a use position list containing all entries removed from this list that have a use
         *         position greater or equal than {@code splitPos}
         */
        public UsePosList splitAt(int splitPos)
        {
            int i = size() - 1;
            int len = 0;
            while (i >= 0 && usePos(i) < splitPos)
            {
                --i;
                len += 2;
            }
            int listSplitIndex = (i + 1) * 2;
            IntList childList = list;
            list = IntList.copy(this.list, listSplitIndex, len);
            childList.setSize(listSplitIndex);
            return new UsePosList(childList);
        }

        /**
         * Gets the use position at a specified index in this list.
         *
         * @param index the index of the entry for which the use position is returned
         * @return the use position of entry {@code index} in this list
         */
        public int usePos(int index)
        {
            return list.get(index << 1);
        }

        /**
         * Gets the register priority for the use position at a specified index in this list.
         *
         * @param index the index of the entry for which the register priority is returned
         * @return the register priority of entry {@code index} in this list
         */
        public RegisterPriority registerPriority(int index)
        {
            return RegisterPriority.VALUES[list.get((index << 1) + 1)];
        }

        public void add(int usePos, RegisterPriority registerPriority)
        {
            list.add(usePos);
            list.add(registerPriority.ordinal());
        }

        public int size()
        {
            return list.size() >> 1;
        }

        public void removeLowestUsePos()
        {
            list.setSize(list.size() - 2);
        }

        public void setRegisterPriority(int index, RegisterPriority registerPriority)
        {
            list.set((index << 1) + 1, registerPriority.ordinal());
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder("[");
            for (int i = size() - 1; i >= 0; --i)
            {
                if (buf.length() != 1)
                {
                    buf.append(", ");
                }
                RegisterPriority prio = registerPriority(i);
                buf.append(usePos(i)).append(" -> ").append(prio.ordinal()).append(':').append(prio);
            }
            return buf.append("]").toString();
        }
    }

    protected static final int END_MARKER_OPERAND_NUMBER = Integer.MIN_VALUE;

    /**
     * The {@linkplain RegisterValue register} or {@linkplain Variable variable} for this interval
     * prior to register allocation.
     */
    public final AllocatableValue operand;

    /**
     * The operand number for this interval's {@linkplain #operand operand}.
     */
    public final int operandNumber;

    /**
     * The {@linkplain RegisterValue register} or {@linkplain StackSlot spill slot} assigned to this
     * interval. In case of a spilled interval which is re-materialized this is
     * {@link Value#ILLEGAL}.
     */
    private AllocatableValue location;

    /**
     * The stack slot to which all splits of this interval are spilled if necessary.
     */
    private AllocatableValue spillSlot;

    /**
     * The kind of this interval.
     */
    private ValueKind<?> kind;

    /**
     * The head of the list of ranges describing this interval. This list is sorted by
     * {@linkplain LIRInstruction#id instruction ids}.
     */
    private Range first;

    /**
     * List of (use-positions, register-priorities) pairs, sorted by use-positions.
     */
    private UsePosList usePosList;

    /**
     * Iterator used to traverse the ranges of an interval.
     */
    private Range current;

    /**
     * Link to next interval in a sorted list of intervals that ends with LinearScan.intervalEndMarker.
     */
    Interval next;

    /**
     * The linear-scan state of this interval.
     */
    State state;

    private int cachedTo; // cached value: to of last range (-1: not cached)

    /**
     * The interval from which this one is derived. If this is a {@linkplain #isSplitParent() split
     * parent}, it points to itself.
     */
    private Interval splitParent;

    /**
     * List of all intervals that are split off from this interval. This is only used if this is a
     * {@linkplain #isSplitParent() split parent}.
     */
    private List<Interval> splitChildren = Collections.emptyList();

    /**
     * Current split child that has been active or inactive last (always stored in split parents).
     */
    private Interval currentSplitChild;

    /**
     * Specifies if move is inserted between currentSplitChild and this interval when interval gets
     * active the first time.
     */
    private boolean insertMoveWhenActivated;

    /**
     * For spill move optimization.
     */
    private SpillState spillState;

    /**
     * Position where this interval is defined (if defined only once).
     */
    private int spillDefinitionPos;

    /**
     * This interval should be assigned the same location as the hint interval.
     */
    private Interval locationHint;

    /**
     * The value with which a spilled child interval can be re-materialized. Currently this must be
     * a Constant.
     */
    private Constant materializedValue;

    /**
     * The number of times {@link #addMaterializationValue(Constant)} is called.
     */
    private int numMaterializationValuesAdded;

    void assignLocation(AllocatableValue newLocation)
    {
        if (ValueUtil.isRegister(newLocation))
        {
            if (newLocation.getValueKind().equals(LIRKind.Illegal) && !kind.equals(LIRKind.Illegal))
            {
                this.location = ValueUtil.asRegister(newLocation).asValue(kind);
                return;
            }
        }
        this.location = newLocation;
    }

    /**
     * Returns true is this is the sentinel interval that denotes the end of an interval list.
     */
    public boolean isEndMarker()
    {
        return operandNumber == END_MARKER_OPERAND_NUMBER;
    }

    /**
     * Gets the {@linkplain RegisterValue register} or {@linkplain StackSlot spill slot} assigned to
     * this interval.
     */
    public AllocatableValue location()
    {
        return location;
    }

    public ValueKind<?> kind()
    {
        return kind;
    }

    public void setKind(ValueKind<?> kind)
    {
        this.kind = kind;
    }

    public Range first()
    {
        return first;
    }

    public int from()
    {
        return first.from;
    }

    int to()
    {
        if (cachedTo == -1)
        {
            cachedTo = calcTo();
        }
        return cachedTo;
    }

    int numUsePositions()
    {
        return usePosList.size();
    }

    public void setLocationHint(Interval interval)
    {
        locationHint = interval;
    }

    public boolean isSplitParent()
    {
        return splitParent == this;
    }

    boolean isSplitChild()
    {
        return splitParent != this;
    }

    /**
     * Gets the split parent for this interval.
     */
    public Interval splitParent()
    {
        return splitParent;
    }

    /**
     * Gets the canonical spill slot for this interval.
     */
    public AllocatableValue spillSlot()
    {
        return splitParent().spillSlot;
    }

    public void setSpillSlot(AllocatableValue slot)
    {
        splitParent().spillSlot = slot;
    }

    Interval currentSplitChild()
    {
        return splitParent().currentSplitChild;
    }

    void makeCurrentSplitChild()
    {
        splitParent().currentSplitChild = this;
    }

    boolean insertMoveWhenActivated()
    {
        return insertMoveWhenActivated;
    }

    void setInsertMoveWhenActivated(boolean b)
    {
        insertMoveWhenActivated = b;
    }

    // for spill optimization
    public SpillState spillState()
    {
        return splitParent().spillState;
    }

    public int spillDefinitionPos()
    {
        return splitParent().spillDefinitionPos;
    }

    public void setSpillState(SpillState state)
    {
        splitParent().spillState = state;
    }

    public void setSpillDefinitionPos(int pos)
    {
        splitParent().spillDefinitionPos = pos;
    }

    // returns true if this interval has a shadow copy on the stack that is always correct
    public boolean alwaysInMemory()
    {
        return SpillState.ALWAYS_IN_MEMORY.contains(spillState()) && !canMaterialize();
    }

    void removeFirstUsePos()
    {
        usePosList.removeLowestUsePos();
    }

    // test intersection
    boolean intersects(Interval i)
    {
        return first.intersects(i.first);
    }

    int intersectsAt(Interval i)
    {
        return first.intersectsAt(i.first);
    }

    // range iteration
    void rewindRange()
    {
        current = first;
    }

    void nextRange()
    {
        current = current.next;
    }

    int currentFrom()
    {
        return current.from;
    }

    int currentTo()
    {
        return current.to;
    }

    boolean currentAtEnd()
    {
        return current.isEndMarker();
    }

    boolean currentIntersects(Interval it)
    {
        return current.intersects(it.current);
    }

    int currentIntersectsAt(Interval it)
    {
        return current.intersectsAt(it.current);
    }

    Interval(AllocatableValue operand, int operandNumber, Interval intervalEndMarker, Range rangeEndMarker)
    {
        this.operand = operand;
        this.operandNumber = operandNumber;
        if (ValueUtil.isRegister(operand))
        {
            location = operand;
        }
        this.kind = LIRKind.Illegal;
        this.first = rangeEndMarker;
        this.usePosList = new UsePosList(4);
        this.current = rangeEndMarker;
        this.next = intervalEndMarker;
        this.cachedTo = -1;
        this.spillState = SpillState.NoDefinitionFound;
        this.spillDefinitionPos = -1;
        splitParent = this;
        currentSplitChild = this;
    }

    /**
     * Sets the value which is used for re-materialization.
     */
    public void addMaterializationValue(Constant value)
    {
        if (numMaterializationValuesAdded == 0)
        {
            materializedValue = value;
        }
        else
        {
            // Interval is defined on multiple places -> no materialization is possible.
            materializedValue = null;
        }
        numMaterializationValuesAdded++;
    }

    /**
     * Returns true if this interval can be re-materialized when spilled. This means that no
     * spill-moves are needed. Instead of restore-moves the {@link #materializedValue} is restored.
     */
    public boolean canMaterialize()
    {
        return getMaterializedValue() != null;
    }

    /**
     * Returns a value which can be moved to a register instead of a restore-move from stack.
     */
    public Constant getMaterializedValue()
    {
        return splitParent().materializedValue;
    }

    int calcTo()
    {
        Range r = first;
        while (!r.next.isEndMarker())
        {
            r = r.next;
        }
        return r.to;
    }

    public Interval locationHint(boolean searchSplitChild)
    {
        if (!searchSplitChild)
        {
            return locationHint;
        }

        if (locationHint != null)
        {
            if (locationHint.location != null && ValueUtil.isRegister(locationHint.location))
            {
                return locationHint;
            }
            else if (!locationHint.splitChildren.isEmpty())
            {
                // search the first split child that has a register assigned
                int len = locationHint.splitChildren.size();
                for (int i = 0; i < len; i++)
                {
                    Interval interval = locationHint.splitChildren.get(i);
                    if (interval.location != null && ValueUtil.isRegister(interval.location))
                    {
                        return interval;
                    }
                }
            }
        }

        // no hint interval found that has a register assigned
        return null;
    }

    Interval getSplitChildAtOpId(int opId, LIRInstruction.OperandMode mode, LinearScan allocator)
    {
        if (splitChildren.isEmpty())
        {
            return this;
        }
        else
        {
            Interval result = null;
            int len = splitChildren.size();

            // in outputMode, the end of the interval (opId == cur.to()) is not valid
            int toOffset = (mode == LIRInstruction.OperandMode.DEF ? 0 : 1);

            int i;
            for (i = 0; i < len; i++)
            {
                Interval cur = splitChildren.get(i);
                if (cur.from() <= opId && opId < cur.to() + toOffset)
                {
                    if (i > 0)
                    {
                        // exchange current split child to start of list (faster access for next
                        // call)
                        Util.atPutGrow(splitChildren, i, splitChildren.get(0), null);
                        Util.atPutGrow(splitChildren, 0, cur, null);
                    }

                    // interval found
                    result = cur;
                    break;
                }
            }

            return result;
        }
    }

    private boolean checkSplitChild(Interval result, int opId, LinearScan allocator, int toOffset, LIRInstruction.OperandMode mode)
    {
        if (result == null)
        {
            // this is an error
            StringBuilder msg = new StringBuilder(this.toString()).append(" has no child at ").append(opId);
            if (!splitChildren.isEmpty())
            {
                Interval firstChild = splitChildren.get(0);
                Interval lastChild = splitChildren.get(splitChildren.size() - 1);
                msg.append(" (first = ").append(firstChild).append(", last = ").append(lastChild).append(")");
            }
            throw new GraalError("Linear Scan Error: %s", msg);
        }

        if (!splitChildren.isEmpty())
        {
            for (Interval interval : splitChildren)
            {
                if (interval != result && interval.from() <= opId && opId < interval.to() + toOffset)
                {
                    // Should not happen: Try another compilation as it is very unlikely to happen again.
                    throw new GraalError("two valid result intervals found for opId %d: %d and %d\n%s\n", opId, result.operandNumber, interval.operandNumber, result.logString(allocator), interval.logString(allocator));
                }
            }
        }
        return true;
    }

    // returns the interval that covers the given opId or null if there is none
    Interval getIntervalCoveringOpId(int opId)
    {
        if (opId >= from())
        {
            return this;
        }

        Interval parent = splitParent();
        Interval result = null;

        int len = parent.splitChildren.size();

        for (int i = len - 1; i >= 0; i--)
        {
            Interval cur = parent.splitChildren.get(i);
            if (cur.from() <= opId && opId < cur.to())
            {
                result = cur;
            }
        }

        return result;
    }

    // returns the last split child that ends before the given opId
    Interval getSplitChildBeforeOpId(int opId)
    {
        Interval parent = splitParent();
        Interval result = null;

        int len = parent.splitChildren.size();

        for (int i = len - 1; i >= 0; i--)
        {
            Interval cur = parent.splitChildren.get(i);
            if (cur.to() <= opId && (result == null || result.to() < cur.to()))
            {
                result = cur;
            }
        }

        return result;
    }

    // checks if opId is covered by any split child
    boolean splitChildCovers(int opId, LIRInstruction.OperandMode mode)
    {
        if (splitChildren.isEmpty())
        {
            // simple case if interval was not split
            return covers(opId, mode);
        }
        else
        {
            // extended case: check all split children
            int len = splitChildren.size();
            for (int i = 0; i < len; i++)
            {
                Interval cur = splitChildren.get(i);
                if (cur.covers(opId, mode))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private RegisterPriority adaptPriority(RegisterPriority priority)
    {
        /*
         * In case of re-materialized values we require that use-operands are registers,
         * because we don't have the value in a stack location.
         * (Note that ShouldHaveRegister means that the operand can also be a StackSlot).
         */
        if (priority == RegisterPriority.ShouldHaveRegister && canMaterialize())
        {
            return RegisterPriority.MustHaveRegister;
        }
        return priority;
    }

    // note: use positions are sorted descending = first use has highest index
    int firstUsage(RegisterPriority minRegisterPriority)
    {
        for (int i = usePosList.size() - 1; i >= 0; --i)
        {
            RegisterPriority registerPriority = adaptPriority(usePosList.registerPriority(i));
            if (registerPriority.greaterEqual(minRegisterPriority))
            {
                return usePosList.usePos(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsage(RegisterPriority minRegisterPriority, int from)
    {
        for (int i = usePosList.size() - 1; i >= 0; --i)
        {
            int usePos = usePosList.usePos(i);
            if (usePos >= from && adaptPriority(usePosList.registerPriority(i)).greaterEqual(minRegisterPriority))
            {
                return usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsageExact(RegisterPriority exactRegisterPriority, int from)
    {
        for (int i = usePosList.size() - 1; i >= 0; --i)
        {
            int usePos = usePosList.usePos(i);
            if (usePos >= from && adaptPriority(usePosList.registerPriority(i)) == exactRegisterPriority)
            {
                return usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int previousUsage(RegisterPriority minRegisterPriority, int from)
    {
        int prev = -1;
        for (int i = usePosList.size() - 1; i >= 0; --i)
        {
            int usePos = usePosList.usePos(i);
            if (usePos > from)
            {
                return prev;
            }
            if (adaptPriority(usePosList.registerPriority(i)).greaterEqual(minRegisterPriority))
            {
                prev = usePos;
            }
        }
        return prev;
    }

    public void addUsePos(int pos, RegisterPriority registerPriority)
    {
        // do not add use positions for precolored intervals because they are never used
        if (registerPriority != RegisterPriority.None && LIRValueUtil.isVariable(operand))
        {
            // note: addUse is called in descending order, so list gets sorted automatically by just appending new use positions
            int len = usePosList.size();
            if (len == 0 || usePosList.usePos(len - 1) > pos)
            {
                usePosList.add(pos, registerPriority);
            }
            else if (usePosList.registerPriority(len - 1).lessThan(registerPriority))
            {
                usePosList.setRegisterPriority(len - 1, registerPriority);
            }
        }
    }

    public void addRange(int from, int to)
    {
        if (first.from <= to)
        {
            // join intersecting ranges
            first.from = Math.min(from, first().from);
            first.to = Math.max(to, first().to);
        }
        else
        {
            // insert new range
            first = new Range(from, to, first());
        }
    }

    Interval newSplitChild(LinearScan allocator)
    {
        // allocate new interval
        Interval parent = splitParent();
        Interval result = allocator.createDerivedInterval(parent);
        result.setKind(kind());

        result.splitParent = parent;
        result.setLocationHint(parent);

        // insert new interval in children-list of parent
        if (parent.splitChildren.isEmpty())
        {
            // create new non-shared list
            parent.splitChildren = new ArrayList<>(4);
            parent.splitChildren.add(this);
        }
        parent.splitChildren.add(result);

        return result;
    }

    /**
     * Splits this interval at a specified position and returns the remainder as a new <i>child</i>
     * interval of this interval's {@linkplain #splitParent() parent} interval.
     *
     * When an interval is split, a bi-directional link is established between the original
     * <i>parent</i> interval and the <i>children</i> intervals that are split off this interval.
     * When a split child is split again, the new created interval is a direct child of the original
     * parent. That is, there is no tree of split children stored, just a flat list. All split
     * children are spilled to the same {@linkplain #spillSlot spill slot}.
     *
     * @param splitPos the position at which to split this interval
     * @param allocator the register allocator context
     * @return the child interval split off from this interval
     */
    Interval split(int splitPos, LinearScan allocator)
    {
        // allocate new interval
        Interval result = newSplitChild(allocator);

        // split the ranges
        Range prev = null;
        Range cur = first;
        while (!cur.isEndMarker() && cur.to <= splitPos)
        {
            prev = cur;
            cur = cur.next;
        }

        if (cur.from < splitPos)
        {
            result.first = new Range(splitPos, cur.to, cur.next);
            cur.to = splitPos;
            cur.next = allocator.rangeEndMarker;
        }
        else
        {
            result.first = cur;
            prev.next = allocator.rangeEndMarker;
        }
        result.current = result.first;
        cachedTo = -1; // clear cached value

        // split list of use positions
        result.usePosList = usePosList.splitAt(splitPos);

        return result;
    }

    /**
     * Splits this interval at a specified position and returns the head as a new interval (this interval is the tail).
     *
     * Currently, only the first range can be split, and the new interval must not have split positions
     */
    Interval splitFromStart(int splitPos, LinearScan allocator)
    {
        // allocate new interval
        Interval result = newSplitChild(allocator);

        // the new interval has only one range (checked by assertion above,
        // so the splitting of the ranges is very simple
        result.addRange(first.from, splitPos);

        if (splitPos == first.to)
        {
            first = first.next;
        }
        else
        {
            first.from = splitPos;
        }

        return result;
    }

    // returns true if the opId is inside the interval
    boolean covers(int opId, LIRInstruction.OperandMode mode)
    {
        Range cur = first;

        while (!cur.isEndMarker() && cur.to < opId)
        {
            cur = cur.next;
        }
        if (!cur.isEndMarker())
        {
            if (mode == LIRInstruction.OperandMode.DEF)
            {
                return cur.from <= opId && opId < cur.to;
            }
            else
            {
                return cur.from <= opId && opId <= cur.to;
            }
        }
        return false;
    }

    // returns true if the interval has any hole between holeFrom and holeTo
    // (even if the hole has only the length 1)
    boolean hasHoleBetween(int holeFrom, int holeTo)
    {
        Range cur = first;
        while (!cur.isEndMarker())
        {
            // hole-range starts before this range . hole
            if (holeFrom < cur.from)
            {
                return true;

                // hole-range completely inside this range . no hole
            }
            else
            {
                if (holeTo <= cur.to)
                {
                    return false;

                    // overlapping of hole-range with this range . hole
                }
                else
                {
                    if (holeFrom <= cur.to)
                    {
                        return true;
                    }
                }
            }

            cur = cur.next;
        }

        return false;
    }

    @Override
    public String toString()
    {
        String from = "?";
        String to = "?";
        if (first != null && !first.isEndMarker())
        {
            from = String.valueOf(from());
            // to() may cache a computed value, modifying the current object, which is a bad idea
            // for a printing function. Compute it directly instead.
            to = String.valueOf(calcTo());
        }
        String locationString = this.location == null ? "" : "@" + this.location;
        return operandNumber + ":" + operand + (ValueUtil.isRegister(operand) ? "" : locationString) + "[" + from + "," + to + "]";
    }

    /**
     * Gets the use position information for this interval.
     */
    public UsePosList usePosList()
    {
        return usePosList;
    }

    /**
     * Gets a single line string for logging the details of this interval to a log stream.
     *
     * @param allocator the register allocator context
     */
    public String logString(LinearScan allocator)
    {
        StringBuilder buf = new StringBuilder(100);
        buf.append(operandNumber).append(':').append(operand).append(' ');
        if (!ValueUtil.isRegister(operand))
        {
            if (location != null)
            {
                buf.append("location{").append(location).append("} ");
            }
        }

        buf.append("hints{").append(splitParent.operandNumber);
        Interval hint = locationHint(false);
        if (hint != null && hint.operandNumber != splitParent.operandNumber)
        {
            buf.append(", ").append(hint.operandNumber);
        }
        buf.append("} ranges{");

        // print ranges
        Range cur = first;
        while (!cur.isEndMarker())
        {
            if (cur != first)
            {
                buf.append(", ");
            }
            buf.append(cur);
            cur = cur.next;
        }
        buf.append("} uses{");

        // print use positions
        int prev = -1;
        for (int i = usePosList.size() - 1; i >= 0; --i)
        {
            if (i != usePosList.size() - 1)
            {
                buf.append(", ");
            }
            buf.append(usePosList.usePos(i)).append(':').append(usePosList.registerPriority(i));
            prev = usePosList.usePos(i);
        }
        buf.append("} spill-state{").append(spillState()).append("}");
        if (canMaterialize())
        {
            buf.append(" (remat:").append(getMaterializedValue().toString()).append(")");
        }
        return buf.toString();
    }

    List<Interval> getSplitChildren()
    {
        return Collections.unmodifiableList(splitChildren);
    }
}
