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
// @class Interval
public final class Interval
{
    /**
     * A set of interval lists, one per {@linkplain RegisterBinding binding} type.
     */
    // @class Interval.RegisterBindingLists
    static final class RegisterBindingLists
    {
        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Fixed}.
         */
        // @field
        public Interval fixed;

        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Any}.
         */
        // @field
        public Interval any;

        /**
         * List of intervals whose binding is currently {@link RegisterBinding#Stack}.
         */
        // @field
        public Interval stack;

        // @cons
        RegisterBindingLists(Interval __fixed, Interval __any, Interval __stack)
        {
            super();
            this.fixed = __fixed;
            this.any = __any;
            this.stack = __stack;
        }

        /**
         * Gets the list for a specified binding.
         *
         * @param binding specifies the list to be returned
         * @return the list of intervals whose binding is {@code binding}
         */
        public Interval get(RegisterBinding __binding)
        {
            switch (__binding)
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
        public void set(RegisterBinding __binding, Interval __list)
        {
            switch (__binding)
            {
                case Any:
                    any = __list;
                    break;
                case Fixed:
                    fixed = __list;
                    break;
                case Stack:
                    stack = __list;
                    break;
            }
        }

        /**
         * Adds an interval to a list sorted by {@linkplain Interval#currentFrom() current from} positions.
         *
         * @param binding specifies the list to be updated
         * @param interval the interval to add
         */
        public void addToListSortedByCurrentFromPositions(RegisterBinding __binding, Interval __interval)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur.currentFrom() < __interval.currentFrom())
            {
                __prev = __cur;
                __cur = __cur.next;
            }
            Interval __result = __list;
            if (__prev == null)
            {
                // add to head of list
                __result = __interval;
            }
            else
            {
                // add before 'cur'
                __prev.next = __interval;
            }
            __interval.next = __cur;
            set(__binding, __result);
        }

        /**
         * Adds an interval to a list sorted by {@linkplain Interval#from() start} positions and
         * {@linkplain Interval#firstUsage(RegisterPriority) first usage} positions.
         *
         * @param binding specifies the list to be updated
         * @param interval the interval to add
         */
        public void addToListSortedByStartAndUsePositions(RegisterBinding __binding, Interval __interval)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur.from() < __interval.from() || (__cur.from() == __interval.from() && __cur.firstUsage(RegisterPriority.None) < __interval.firstUsage(RegisterPriority.None)))
            {
                __prev = __cur;
                __cur = __cur.next;
            }
            if (__prev == null)
            {
                __list = __interval;
            }
            else
            {
                __prev.next = __interval;
            }
            __interval.next = __cur;
            set(__binding, __list);
        }

        /**
         * Removes an interval from a list.
         *
         * @param binding specifies the list to be updated
         * @param i the interval to remove
         */
        public void remove(RegisterBinding __binding, Interval __i)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur != __i)
            {
                __prev = __cur;
                __cur = __cur.next;
            }
            if (__prev == null)
            {
                set(__binding, __cur.next);
            }
            else
            {
                __prev.next = __cur.next;
            }
        }
    }

    /**
     * Constants denoting the register usage priority for an interval. The constants are declared in
     * increasing order of priority are are used to optimize spilling when multiple overlapping
     * intervals compete for limited registers.
     */
    // @enum Interval.RegisterPriority
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

        // @def
        public static final RegisterPriority[] VALUES = values();

        /**
         * Determines if this priority is higher than or equal to a given priority.
         */
        public boolean greaterEqual(RegisterPriority __other)
        {
            return ordinal() >= __other.ordinal();
        }

        /**
         * Determines if this priority is lower than a given priority.
         */
        public boolean lessThan(RegisterPriority __other)
        {
            return ordinal() < __other.ordinal();
        }
    }

    /**
     * Constants denoting whether an interval is bound to a specific register. This models platform
     * dependencies on register usage for certain instructions.
     */
    // @enum Interval.RegisterBinding
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

        // @def
        public static final RegisterBinding[] VALUES = values();
    }

    /**
     * Constants denoting the linear-scan states an interval may be in with respect to the
     * {@linkplain Interval#from() start} {@code position} of the interval being processed.
     */
    // @enum Interval.State
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
    // @enum Interval.SpillState
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

        // @def
        public static final EnumSet<SpillState> ALWAYS_IN_MEMORY = EnumSet.of(SpillInDominator, StoreAtDefinition, StartInMemory);
    }

    /**
     * List of use positions. Each entry in the list records the use position and register priority
     * associated with the use position. The entries in the list are in descending order of use position.
     */
    // @class Interval.UsePosList
    public static final class UsePosList
    {
        // @field
        private IntList list;

        /**
         * Creates a use list.
         *
         * @param initialCapacity the initial capacity of the list in terms of entries
         */
        // @cons
        public UsePosList(int __initialCapacity)
        {
            super();
            list = new IntList(__initialCapacity * 2);
        }

        // @cons
        private UsePosList(IntList __list)
        {
            super();
            this.list = __list;
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
        public UsePosList splitAt(int __splitPos)
        {
            int __i = size() - 1;
            int __len = 0;
            while (__i >= 0 && usePos(__i) < __splitPos)
            {
                --__i;
                __len += 2;
            }
            int __listSplitIndex = (__i + 1) * 2;
            IntList __childList = list;
            list = IntList.copy(this.list, __listSplitIndex, __len);
            __childList.setSize(__listSplitIndex);
            return new UsePosList(__childList);
        }

        /**
         * Gets the use position at a specified index in this list.
         *
         * @param index the index of the entry for which the use position is returned
         * @return the use position of entry {@code index} in this list
         */
        public int usePos(int __index)
        {
            return list.get(__index << 1);
        }

        /**
         * Gets the register priority for the use position at a specified index in this list.
         *
         * @param index the index of the entry for which the register priority is returned
         * @return the register priority of entry {@code index} in this list
         */
        public RegisterPriority registerPriority(int __index)
        {
            return RegisterPriority.VALUES[list.get((__index << 1) + 1)];
        }

        public void add(int __usePos, RegisterPriority __registerPriority)
        {
            list.add(__usePos);
            list.add(__registerPriority.ordinal());
        }

        public int size()
        {
            return list.size() >> 1;
        }

        public void removeLowestUsePos()
        {
            list.setSize(list.size() - 2);
        }

        public void setRegisterPriority(int __index, RegisterPriority __registerPriority)
        {
            list.set((__index << 1) + 1, __registerPriority.ordinal());
        }
    }

    // @def
    protected static final int END_MARKER_OPERAND_NUMBER = Integer.MIN_VALUE;

    /**
     * The {@linkplain RegisterValue register} or {@linkplain Variable variable} for this interval
     * prior to register allocation.
     */
    // @field
    public final AllocatableValue operand;

    /**
     * The operand number for this interval's {@linkplain #operand operand}.
     */
    // @field
    public final int operandNumber;

    /**
     * The {@linkplain RegisterValue register} or {@linkplain StackSlot spill slot} assigned to this
     * interval. In case of a spilled interval which is re-materialized this is {@link Value#ILLEGAL}.
     */
    // @field
    private AllocatableValue location;

    /**
     * The stack slot to which all splits of this interval are spilled if necessary.
     */
    // @field
    private AllocatableValue spillSlot;

    /**
     * The kind of this interval.
     */
    // @field
    private ValueKind<?> kind;

    /**
     * The head of the list of ranges describing this interval. This list is sorted by
     * {@linkplain LIRInstruction#id instruction ids}.
     */
    // @field
    private Range first;

    /**
     * List of (use-positions, register-priorities) pairs, sorted by use-positions.
     */
    // @field
    private UsePosList usePosList;

    /**
     * Iterator used to traverse the ranges of an interval.
     */
    // @field
    private Range current;

    /**
     * Link to next interval in a sorted list of intervals that ends with LinearScan.intervalEndMarker.
     */
    // @field
    Interval next;

    /**
     * The linear-scan state of this interval.
     */
    // @field
    State state;

    // @field
    private int cachedTo; // cached value: to of last range (-1: not cached)

    /**
     * The interval from which this one is derived. If this is a {@linkplain #isSplitParent() split
     * parent}, it points to itself.
     */
    // @field
    private Interval splitParent;

    /**
     * List of all intervals that are split off from this interval. This is only used if this is a
     * {@linkplain #isSplitParent() split parent}.
     */
    // @field
    private List<Interval> splitChildren = Collections.emptyList();

    /**
     * Current split child that has been active or inactive last (always stored in split parents).
     */
    // @field
    private Interval currentSplitChild;

    /**
     * Specifies if move is inserted between currentSplitChild and this interval when interval gets
     * active the first time.
     */
    // @field
    private boolean insertMoveWhenActivated;

    /**
     * For spill move optimization.
     */
    // @field
    private SpillState spillState;

    /**
     * Position where this interval is defined (if defined only once).
     */
    // @field
    private int spillDefinitionPos;

    /**
     * This interval should be assigned the same location as the hint interval.
     */
    // @field
    private Interval locationHint;

    /**
     * The value with which a spilled child interval can be re-materialized. Currently this must be
     * a Constant.
     */
    // @field
    private Constant materializedValue;

    /**
     * The number of times {@link #addMaterializationValue(Constant)} is called.
     */
    // @field
    private int numMaterializationValuesAdded;

    void assignLocation(AllocatableValue __newLocation)
    {
        if (ValueUtil.isRegister(__newLocation))
        {
            if (__newLocation.getValueKind().equals(LIRKind.Illegal) && !kind.equals(LIRKind.Illegal))
            {
                this.location = ValueUtil.asRegister(__newLocation).asValue(kind);
                return;
            }
        }
        this.location = __newLocation;
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

    public void setKind(ValueKind<?> __kind)
    {
        this.kind = __kind;
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

    public void setLocationHint(Interval __interval)
    {
        locationHint = __interval;
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

    public void setSpillSlot(AllocatableValue __slot)
    {
        splitParent().spillSlot = __slot;
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

    void setInsertMoveWhenActivated(boolean __b)
    {
        insertMoveWhenActivated = __b;
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

    public void setSpillState(SpillState __state)
    {
        splitParent().spillState = __state;
    }

    public void setSpillDefinitionPos(int __pos)
    {
        splitParent().spillDefinitionPos = __pos;
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
    boolean intersects(Interval __i)
    {
        return first.intersects(__i.first);
    }

    int intersectsAt(Interval __i)
    {
        return first.intersectsAt(__i.first);
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

    boolean currentIntersects(Interval __it)
    {
        return current.intersects(__it.current);
    }

    int currentIntersectsAt(Interval __it)
    {
        return current.intersectsAt(__it.current);
    }

    // @cons
    Interval(AllocatableValue __operand, int __operandNumber, Interval __intervalEndMarker, Range __rangeEndMarker)
    {
        super();
        this.operand = __operand;
        this.operandNumber = __operandNumber;
        if (ValueUtil.isRegister(__operand))
        {
            location = __operand;
        }
        this.kind = LIRKind.Illegal;
        this.first = __rangeEndMarker;
        this.usePosList = new UsePosList(4);
        this.current = __rangeEndMarker;
        this.next = __intervalEndMarker;
        this.cachedTo = -1;
        this.spillState = SpillState.NoDefinitionFound;
        this.spillDefinitionPos = -1;
        splitParent = this;
        currentSplitChild = this;
    }

    /**
     * Sets the value which is used for re-materialization.
     */
    public void addMaterializationValue(Constant __value)
    {
        if (numMaterializationValuesAdded == 0)
        {
            materializedValue = __value;
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
        Range __r = first;
        while (!__r.next.isEndMarker())
        {
            __r = __r.next;
        }
        return __r.to;
    }

    public Interval locationHint(boolean __searchSplitChild)
    {
        if (!__searchSplitChild)
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
                int __len = locationHint.splitChildren.size();
                for (int __i = 0; __i < __len; __i++)
                {
                    Interval __interval = locationHint.splitChildren.get(__i);
                    if (__interval.location != null && ValueUtil.isRegister(__interval.location))
                    {
                        return __interval;
                    }
                }
            }
        }

        // no hint interval found that has a register assigned
        return null;
    }

    Interval getSplitChildAtOpId(int __opId, LIRInstruction.OperandMode __mode, LinearScan __allocator)
    {
        if (splitChildren.isEmpty())
        {
            return this;
        }
        else
        {
            Interval __result = null;
            int __len = splitChildren.size();

            // in outputMode, the end of the interval (opId == cur.to()) is not valid
            int __toOffset = (__mode == LIRInstruction.OperandMode.DEF ? 0 : 1);

            int __i;
            for (__i = 0; __i < __len; __i++)
            {
                Interval __cur = splitChildren.get(__i);
                if (__cur.from() <= __opId && __opId < __cur.to() + __toOffset)
                {
                    if (__i > 0)
                    {
                        // exchange current split child to start of list (faster access for next call)
                        Util.atPutGrow(splitChildren, __i, splitChildren.get(0), null);
                        Util.atPutGrow(splitChildren, 0, __cur, null);
                    }

                    // interval found
                    __result = __cur;
                    break;
                }
            }

            return __result;
        }
    }

    // returns the interval that covers the given opId or null if there is none
    Interval getIntervalCoveringOpId(int __opId)
    {
        if (__opId >= from())
        {
            return this;
        }

        Interval __parent = splitParent();
        Interval __result = null;

        int __len = __parent.splitChildren.size();

        for (int __i = __len - 1; __i >= 0; __i--)
        {
            Interval __cur = __parent.splitChildren.get(__i);
            if (__cur.from() <= __opId && __opId < __cur.to())
            {
                __result = __cur;
            }
        }

        return __result;
    }

    // returns the last split child that ends before the given opId
    Interval getSplitChildBeforeOpId(int __opId)
    {
        Interval __parent = splitParent();
        Interval __result = null;

        int __len = __parent.splitChildren.size();

        for (int __i = __len - 1; __i >= 0; __i--)
        {
            Interval __cur = __parent.splitChildren.get(__i);
            if (__cur.to() <= __opId && (__result == null || __result.to() < __cur.to()))
            {
                __result = __cur;
            }
        }

        return __result;
    }

    // checks if opId is covered by any split child
    boolean splitChildCovers(int __opId, LIRInstruction.OperandMode __mode)
    {
        if (splitChildren.isEmpty())
        {
            // simple case if interval was not split
            return covers(__opId, __mode);
        }
        else
        {
            // extended case: check all split children
            int __len = splitChildren.size();
            for (int __i = 0; __i < __len; __i++)
            {
                Interval __cur = splitChildren.get(__i);
                if (__cur.covers(__opId, __mode))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private RegisterPriority adaptPriority(RegisterPriority __priority)
    {
        /*
         * In case of re-materialized values we require that use-operands are registers,
         * because we don't have the value in a stack location.
         * (Note that ShouldHaveRegister means that the operand can also be a StackSlot).
         */
        if (__priority == RegisterPriority.ShouldHaveRegister && canMaterialize())
        {
            return RegisterPriority.MustHaveRegister;
        }
        return __priority;
    }

    // note: use positions are sorted descending = first use has highest index
    int firstUsage(RegisterPriority __minRegisterPriority)
    {
        for (int __i = usePosList.size() - 1; __i >= 0; --__i)
        {
            RegisterPriority __registerPriority = adaptPriority(usePosList.registerPriority(__i));
            if (__registerPriority.greaterEqual(__minRegisterPriority))
            {
                return usePosList.usePos(__i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsage(RegisterPriority __minRegisterPriority, int __from)
    {
        for (int __i = usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = usePosList.usePos(__i);
            if (__usePos >= __from && adaptPriority(usePosList.registerPriority(__i)).greaterEqual(__minRegisterPriority))
            {
                return __usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsageExact(RegisterPriority __exactRegisterPriority, int __from)
    {
        for (int __i = usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = usePosList.usePos(__i);
            if (__usePos >= __from && adaptPriority(usePosList.registerPriority(__i)) == __exactRegisterPriority)
            {
                return __usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int previousUsage(RegisterPriority __minRegisterPriority, int __from)
    {
        int __prev = -1;
        for (int __i = usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = usePosList.usePos(__i);
            if (__usePos > __from)
            {
                return __prev;
            }
            if (adaptPriority(usePosList.registerPriority(__i)).greaterEqual(__minRegisterPriority))
            {
                __prev = __usePos;
            }
        }
        return __prev;
    }

    public void addUsePos(int __pos, RegisterPriority __registerPriority)
    {
        // do not add use positions for precolored intervals because they are never used
        if (__registerPriority != RegisterPriority.None && LIRValueUtil.isVariable(operand))
        {
            // note: addUse is called in descending order, so list gets sorted automatically by just appending new use positions
            int __len = usePosList.size();
            if (__len == 0 || usePosList.usePos(__len - 1) > __pos)
            {
                usePosList.add(__pos, __registerPriority);
            }
            else if (usePosList.registerPriority(__len - 1).lessThan(__registerPriority))
            {
                usePosList.setRegisterPriority(__len - 1, __registerPriority);
            }
        }
    }

    public void addRange(int __from, int __to)
    {
        if (first.from <= __to)
        {
            // join intersecting ranges
            first.from = Math.min(__from, first().from);
            first.to = Math.max(__to, first().to);
        }
        else
        {
            // insert new range
            first = new Range(__from, __to, first());
        }
    }

    Interval newSplitChild(LinearScan __allocator)
    {
        // allocate new interval
        Interval __parent = splitParent();
        Interval __result = __allocator.createDerivedInterval(__parent);
        __result.setKind(kind());

        __result.splitParent = __parent;
        __result.setLocationHint(__parent);

        // insert new interval in children-list of parent
        if (__parent.splitChildren.isEmpty())
        {
            // create new non-shared list
            __parent.splitChildren = new ArrayList<>(4);
            __parent.splitChildren.add(this);
        }
        __parent.splitChildren.add(__result);

        return __result;
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
    Interval split(int __splitPos, LinearScan __allocator)
    {
        // allocate new interval
        Interval __result = newSplitChild(__allocator);

        // split the ranges
        Range __prev = null;
        Range __cur = first;
        while (!__cur.isEndMarker() && __cur.to <= __splitPos)
        {
            __prev = __cur;
            __cur = __cur.next;
        }

        if (__cur.from < __splitPos)
        {
            __result.first = new Range(__splitPos, __cur.to, __cur.next);
            __cur.to = __splitPos;
            __cur.next = __allocator.rangeEndMarker;
        }
        else
        {
            __result.first = __cur;
            __prev.next = __allocator.rangeEndMarker;
        }
        __result.current = __result.first;
        cachedTo = -1; // clear cached value

        // split list of use positions
        __result.usePosList = usePosList.splitAt(__splitPos);

        return __result;
    }

    /**
     * Splits this interval at a specified position and returns the head as a new interval (this interval is the tail).
     *
     * Currently, only the first range can be split, and the new interval must not have split positions
     */
    Interval splitFromStart(int __splitPos, LinearScan __allocator)
    {
        // allocate new interval
        Interval __result = newSplitChild(__allocator);

        // the new interval has only one range (checked by assertion above,
        // so the splitting of the ranges is very simple
        __result.addRange(first.from, __splitPos);

        if (__splitPos == first.to)
        {
            first = first.next;
        }
        else
        {
            first.from = __splitPos;
        }

        return __result;
    }

    // returns true if the opId is inside the interval
    boolean covers(int __opId, LIRInstruction.OperandMode __mode)
    {
        Range __cur = first;

        while (!__cur.isEndMarker() && __cur.to < __opId)
        {
            __cur = __cur.next;
        }
        if (!__cur.isEndMarker())
        {
            if (__mode == LIRInstruction.OperandMode.DEF)
            {
                return __cur.from <= __opId && __opId < __cur.to;
            }
            else
            {
                return __cur.from <= __opId && __opId <= __cur.to;
            }
        }
        return false;
    }

    // returns true if the interval has any hole between holeFrom and holeTo
    // (even if the hole has only the length 1)
    boolean hasHoleBetween(int __holeFrom, int __holeTo)
    {
        Range __cur = first;
        while (!__cur.isEndMarker())
        {
            // hole-range starts before this range . hole
            if (__holeFrom < __cur.from)
            {
                return true;

                // hole-range completely inside this range . no hole
            }
            else
            {
                if (__holeTo <= __cur.to)
                {
                    return false;

                    // overlapping of hole-range with this range . hole
                }
                else
                {
                    if (__holeFrom <= __cur.to)
                    {
                        return true;
                    }
                }
            }

            __cur = __cur.next;
        }

        return false;
    }

    /**
     * Gets the use position information for this interval.
     */
    public UsePosList usePosList()
    {
        return usePosList;
    }

    List<Interval> getSplitChildren()
    {
        return Collections.unmodifiableList(splitChildren);
    }
}
