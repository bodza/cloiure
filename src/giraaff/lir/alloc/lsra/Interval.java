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

///
// Represents an interval in the {@linkplain LinearScan linear scan register allocator}.
///
// @class Interval
public final class Interval
{
    ///
    // A set of interval lists, one per {@linkplain RegisterBinding binding} type.
    ///
    // @class Interval.RegisterBindingLists
    static final class RegisterBindingLists
    {
        ///
        // List of intervals whose binding is currently {@link RegisterBinding#Fixed}.
        ///
        // @field
        public Interval ___fixed;

        ///
        // List of intervals whose binding is currently {@link RegisterBinding#Any}.
        ///
        // @field
        public Interval ___any;

        ///
        // List of intervals whose binding is currently {@link RegisterBinding#Stack}.
        ///
        // @field
        public Interval ___stack;

        // @cons
        RegisterBindingLists(Interval __fixed, Interval __any, Interval __stack)
        {
            super();
            this.___fixed = __fixed;
            this.___any = __any;
            this.___stack = __stack;
        }

        ///
        // Gets the list for a specified binding.
        //
        // @param binding specifies the list to be returned
        // @return the list of intervals whose binding is {@code binding}
        ///
        public Interval get(RegisterBinding __binding)
        {
            switch (__binding)
            {
                case Any:
                    return this.___any;
                case Fixed:
                    return this.___fixed;
                case Stack:
                    return this.___stack;
            }
            throw GraalError.shouldNotReachHere();
        }

        ///
        // Sets the list for a specified binding.
        //
        // @param binding specifies the list to be replaced
        // @param list a list of intervals whose binding is {@code binding}
        ///
        public void set(RegisterBinding __binding, Interval __list)
        {
            switch (__binding)
            {
                case Any:
                {
                    this.___any = __list;
                    break;
                }
                case Fixed:
                {
                    this.___fixed = __list;
                    break;
                }
                case Stack:
                {
                    this.___stack = __list;
                    break;
                }
            }
        }

        ///
        // Adds an interval to a list sorted by {@linkplain Interval#currentFrom() current from} positions.
        //
        // @param binding specifies the list to be updated
        // @param interval the interval to add
        ///
        public void addToListSortedByCurrentFromPositions(RegisterBinding __binding, Interval __interval)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur.currentFrom() < __interval.currentFrom())
            {
                __prev = __cur;
                __cur = __cur.___next;
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
                __prev.___next = __interval;
            }
            __interval.___next = __cur;
            set(__binding, __result);
        }

        ///
        // Adds an interval to a list sorted by {@linkplain Interval#from() start} positions and
        // {@linkplain Interval#firstUsage(RegisterPriority) first usage} positions.
        //
        // @param binding specifies the list to be updated
        // @param interval the interval to add
        ///
        public void addToListSortedByStartAndUsePositions(RegisterBinding __binding, Interval __interval)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur.from() < __interval.from() || (__cur.from() == __interval.from() && __cur.firstUsage(RegisterPriority.None) < __interval.firstUsage(RegisterPriority.None)))
            {
                __prev = __cur;
                __cur = __cur.___next;
            }
            if (__prev == null)
            {
                __list = __interval;
            }
            else
            {
                __prev.___next = __interval;
            }
            __interval.___next = __cur;
            set(__binding, __list);
        }

        ///
        // Removes an interval from a list.
        //
        // @param binding specifies the list to be updated
        // @param i the interval to remove
        ///
        public void remove(RegisterBinding __binding, Interval __i)
        {
            Interval __list = get(__binding);
            Interval __prev = null;
            Interval __cur = __list;
            while (__cur != __i)
            {
                __prev = __cur;
                __cur = __cur.___next;
            }
            if (__prev == null)
            {
                set(__binding, __cur.___next);
            }
            else
            {
                __prev.___next = __cur.___next;
            }
        }
    }

    ///
    // Constants denoting the register usage priority for an interval. The constants are declared in
    // increasing order of priority are are used to optimize spilling when multiple overlapping
    // intervals compete for limited registers.
    ///
    // @enum Interval.RegisterPriority
    public enum RegisterPriority
    {
        ///
        // No special reason for an interval to be allocated a register.
        ///
        None,

        ///
        // Priority level for intervals live at the end of a loop.
        ///
        LiveAtLoopEnd,

        ///
        // Priority level for intervals that should be allocated to a register.
        ///
        ShouldHaveRegister,

        ///
        // Priority level for intervals that must be allocated to a register.
        ///
        MustHaveRegister;

        // @def
        public static final RegisterPriority[] VALUES = values();

        ///
        // Determines if this priority is higher than or equal to a given priority.
        ///
        public boolean greaterEqual(RegisterPriority __other)
        {
            return ordinal() >= __other.ordinal();
        }

        ///
        // Determines if this priority is lower than a given priority.
        ///
        public boolean lessThan(RegisterPriority __other)
        {
            return ordinal() < __other.ordinal();
        }
    }

    ///
    // Constants denoting whether an interval is bound to a specific register. This models platform
    // dependencies on register usage for certain instructions.
    ///
    // @enum Interval.RegisterBinding
    enum RegisterBinding
    {
        ///
        // Interval is bound to a specific register as required by the platform.
        ///
        Fixed,

        ///
        // Interval has no specific register requirements.
        ///
        Any,

        ///
        // Interval is bound to a stack slot.
        ///
        Stack;

        // @def
        public static final RegisterBinding[] VALUES = values();
    }

    ///
    // Constants denoting the linear-scan states an interval may be in with respect to the
    // {@linkplain Interval#from() start} {@code position} of the interval being processed.
    ///
    // @enum Interval.State
    enum State
    {
        ///
        // An interval that starts after {@code position}.
        ///
        Unhandled,

        ///
        // An interval that {@linkplain Interval#covers covers} {@code position} and has an assigned register.
        ///
        Active,

        ///
        // An interval that starts before and ends after {@code position} but does not
        // {@linkplain Interval#covers cover} it due to a lifetime hole.
        ///
        Inactive,

        ///
        // An interval that ends before {@code position} or is spilled to memory.
        ///
        Handled;
    }

    ///
    // Constants used in optimization of spilling of an interval.
    ///
    // @enum Interval.SpillState
    public enum SpillState
    {
        ///
        // Starting state of calculation: no definition found yet.
        ///
        NoDefinitionFound,

        ///
        // One definition has already been found. Two consecutive definitions are treated as one
        // (e.g. a consecutive move and add because of two-operand LIR form). The position of this
        // definition is given by {@link Interval#spillDefinitionPos()}.
        ///
        NoSpillStore,

        ///
        // One spill move has already been inserted.
        ///
        OneSpillStore,

        ///
        // The interval is spilled multiple times or is spilled in a loop. Place the store somewhere
        // on the dominator path between the definition and the usages.
        ///
        SpillInDominator,

        ///
        // The interval should be stored immediately after its definition to prevent multiple
        // redundant stores.
        ///
        StoreAtDefinition,

        ///
        // The interval starts in memory (e.g. method parameter), so a store is never necessary.
        ///
        StartInMemory,

        ///
        // The interval has more than one definition (e.g. resulting from phi moves), so stores to
        // memory are not optimized.
        ///
        NoOptimization;

        // @def
        public static final EnumSet<SpillState> ALWAYS_IN_MEMORY = EnumSet.of(SpillInDominator, StoreAtDefinition, StartInMemory);
    }

    ///
    // List of use positions. Each entry in the list records the use position and register priority
    // associated with the use position. The entries in the list are in descending order of use position.
    ///
    // @class Interval.UsePosList
    public static final class UsePosList
    {
        // @field
        private IntList ___list;

        ///
        // Creates a use list.
        //
        // @param initialCapacity the initial capacity of the list in terms of entries
        ///
        // @cons
        public UsePosList(int __initialCapacity)
        {
            super();
            this.___list = new IntList(__initialCapacity * 2);
        }

        // @cons
        private UsePosList(IntList __list)
        {
            super();
            this.___list = __list;
        }

        ///
        // Splits this list around a given position. All entries in this list with a use position
        // greater or equal than {@code splitPos} are removed from this list and added to the
        // returned list.
        //
        // @param splitPos the position for the split
        // @return a use position list containing all entries removed from this list that have a use
        //         position greater or equal than {@code splitPos}
        ///
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
            IntList __childList = this.___list;
            this.___list = IntList.copy(this.___list, __listSplitIndex, __len);
            __childList.setSize(__listSplitIndex);
            return new UsePosList(__childList);
        }

        ///
        // Gets the use position at a specified index in this list.
        //
        // @param index the index of the entry for which the use position is returned
        // @return the use position of entry {@code index} in this list
        ///
        public int usePos(int __index)
        {
            return this.___list.get(__index << 1);
        }

        ///
        // Gets the register priority for the use position at a specified index in this list.
        //
        // @param index the index of the entry for which the register priority is returned
        // @return the register priority of entry {@code index} in this list
        ///
        public RegisterPriority registerPriority(int __index)
        {
            return RegisterPriority.VALUES[this.___list.get((__index << 1) + 1)];
        }

        public void add(int __usePos, RegisterPriority __registerPriority)
        {
            this.___list.add(__usePos);
            this.___list.add(__registerPriority.ordinal());
        }

        public int size()
        {
            return this.___list.size() >> 1;
        }

        public void removeLowestUsePos()
        {
            this.___list.setSize(this.___list.size() - 2);
        }

        public void setRegisterPriority(int __index, RegisterPriority __registerPriority)
        {
            this.___list.set((__index << 1) + 1, __registerPriority.ordinal());
        }
    }

    // @def
    protected static final int END_MARKER_OPERAND_NUMBER = Integer.MIN_VALUE;

    ///
    // The {@linkplain RegisterValue register} or {@linkplain Variable variable} for this interval
    // prior to register allocation.
    ///
    // @field
    public final AllocatableValue ___operand;

    ///
    // The operand number for this interval's {@linkplain #operand operand}.
    ///
    // @field
    public final int ___operandNumber;

    ///
    // The {@linkplain RegisterValue register} or {@linkplain StackSlot spill slot} assigned to this
    // interval. In case of a spilled interval which is re-materialized this is {@link Value#ILLEGAL}.
    ///
    // @field
    private AllocatableValue ___location;

    ///
    // The stack slot to which all splits of this interval are spilled if necessary.
    ///
    // @field
    private AllocatableValue ___spillSlot;

    ///
    // The kind of this interval.
    ///
    // @field
    private ValueKind<?> ___kind;

    ///
    // The head of the list of ranges describing this interval. This list is sorted by
    // {@linkplain LIRInstruction#id instruction ids}.
    ///
    // @field
    private Range ___first;

    ///
    // List of (use-positions, register-priorities) pairs, sorted by use-positions.
    ///
    // @field
    private UsePosList ___usePosList;

    ///
    // Iterator used to traverse the ranges of an interval.
    ///
    // @field
    private Range ___current;

    ///
    // Link to next interval in a sorted list of intervals that ends with LinearScan.intervalEndMarker.
    ///
    // @field
    Interval ___next;

    ///
    // The linear-scan state of this interval.
    ///
    // @field
    State ___state;

    // @field
    private int ___cachedTo; // cached value: to of last range (-1: not cached)

    ///
    // The interval from which this one is derived. If this is a {@linkplain #isSplitParent() split
    // parent}, it points to itself.
    ///
    // @field
    private Interval ___splitParent;

    ///
    // List of all intervals that are split off from this interval. This is only used if this is a
    // {@linkplain #isSplitParent() split parent}.
    ///
    // @field
    private List<Interval> ___splitChildren = Collections.emptyList();

    ///
    // Current split child that has been active or inactive last (always stored in split parents).
    ///
    // @field
    private Interval ___currentSplitChild;

    ///
    // Specifies if move is inserted between currentSplitChild and this interval when interval gets
    // active the first time.
    ///
    // @field
    private boolean ___insertMoveWhenActivated;

    ///
    // For spill move optimization.
    ///
    // @field
    private SpillState ___spillState;

    ///
    // Position where this interval is defined (if defined only once).
    ///
    // @field
    private int ___spillDefinitionPos;

    ///
    // This interval should be assigned the same location as the hint interval.
    ///
    // @field
    private Interval ___locationHint;

    ///
    // The value with which a spilled child interval can be re-materialized. Currently this must be
    // a Constant.
    ///
    // @field
    private Constant ___materializedValue;

    ///
    // The number of times {@link #addMaterializationValue(Constant)} is called.
    ///
    // @field
    private int ___numMaterializationValuesAdded;

    void assignLocation(AllocatableValue __newLocation)
    {
        if (ValueUtil.isRegister(__newLocation))
        {
            if (__newLocation.getValueKind().equals(LIRKind.Illegal) && !this.___kind.equals(LIRKind.Illegal))
            {
                this.___location = ValueUtil.asRegister(__newLocation).asValue(this.___kind);
                return;
            }
        }
        this.___location = __newLocation;
    }

    ///
    // Returns true is this is the sentinel interval that denotes the end of an interval list.
    ///
    public boolean isEndMarker()
    {
        return this.___operandNumber == END_MARKER_OPERAND_NUMBER;
    }

    ///
    // Gets the {@linkplain RegisterValue register} or {@linkplain StackSlot spill slot} assigned to
    // this interval.
    ///
    public AllocatableValue location()
    {
        return this.___location;
    }

    public ValueKind<?> kind()
    {
        return this.___kind;
    }

    public void setKind(ValueKind<?> __kind)
    {
        this.___kind = __kind;
    }

    public Range first()
    {
        return this.___first;
    }

    public int from()
    {
        return this.___first.___from;
    }

    int to()
    {
        if (this.___cachedTo == -1)
        {
            this.___cachedTo = calcTo();
        }
        return this.___cachedTo;
    }

    int numUsePositions()
    {
        return this.___usePosList.size();
    }

    public void setLocationHint(Interval __interval)
    {
        this.___locationHint = __interval;
    }

    public boolean isSplitParent()
    {
        return this.___splitParent == this;
    }

    boolean isSplitChild()
    {
        return this.___splitParent != this;
    }

    ///
    // Gets the split parent for this interval.
    ///
    public Interval splitParent()
    {
        return this.___splitParent;
    }

    ///
    // Gets the canonical spill slot for this interval.
    ///
    public AllocatableValue spillSlot()
    {
        return splitParent().___spillSlot;
    }

    public void setSpillSlot(AllocatableValue __slot)
    {
        splitParent().___spillSlot = __slot;
    }

    Interval currentSplitChild()
    {
        return splitParent().___currentSplitChild;
    }

    void makeCurrentSplitChild()
    {
        splitParent().___currentSplitChild = this;
    }

    boolean insertMoveWhenActivated()
    {
        return this.___insertMoveWhenActivated;
    }

    void setInsertMoveWhenActivated(boolean __b)
    {
        this.___insertMoveWhenActivated = __b;
    }

    // for spill optimization
    public SpillState spillState()
    {
        return splitParent().___spillState;
    }

    public int spillDefinitionPos()
    {
        return splitParent().___spillDefinitionPos;
    }

    public void setSpillState(SpillState __state)
    {
        splitParent().___spillState = __state;
    }

    public void setSpillDefinitionPos(int __pos)
    {
        splitParent().___spillDefinitionPos = __pos;
    }

    // returns true if this interval has a shadow copy on the stack that is always correct
    public boolean alwaysInMemory()
    {
        return SpillState.ALWAYS_IN_MEMORY.contains(spillState()) && !canMaterialize();
    }

    void removeFirstUsePos()
    {
        this.___usePosList.removeLowestUsePos();
    }

    // test intersection
    boolean intersects(Interval __i)
    {
        return this.___first.intersects(__i.___first);
    }

    int intersectsAt(Interval __i)
    {
        return this.___first.intersectsAt(__i.___first);
    }

    // range iteration
    void rewindRange()
    {
        this.___current = this.___first;
    }

    void nextRange()
    {
        this.___current = this.___current.___next;
    }

    int currentFrom()
    {
        return this.___current.___from;
    }

    int currentTo()
    {
        return this.___current.___to;
    }

    boolean currentAtEnd()
    {
        return this.___current.isEndMarker();
    }

    boolean currentIntersects(Interval __it)
    {
        return this.___current.intersects(__it.___current);
    }

    int currentIntersectsAt(Interval __it)
    {
        return this.___current.intersectsAt(__it.___current);
    }

    // @cons
    Interval(AllocatableValue __operand, int __operandNumber, Interval __intervalEndMarker, Range __rangeEndMarker)
    {
        super();
        this.___operand = __operand;
        this.___operandNumber = __operandNumber;
        if (ValueUtil.isRegister(__operand))
        {
            this.___location = __operand;
        }
        this.___kind = LIRKind.Illegal;
        this.___first = __rangeEndMarker;
        this.___usePosList = new UsePosList(4);
        this.___current = __rangeEndMarker;
        this.___next = __intervalEndMarker;
        this.___cachedTo = -1;
        this.___spillState = SpillState.NoDefinitionFound;
        this.___spillDefinitionPos = -1;
        this.___splitParent = this;
        this.___currentSplitChild = this;
    }

    ///
    // Sets the value which is used for re-materialization.
    ///
    public void addMaterializationValue(Constant __value)
    {
        if (this.___numMaterializationValuesAdded == 0)
        {
            this.___materializedValue = __value;
        }
        else
        {
            // Interval is defined on multiple places -> no materialization is possible.
            this.___materializedValue = null;
        }
        this.___numMaterializationValuesAdded++;
    }

    ///
    // Returns true if this interval can be re-materialized when spilled. This means that no
    // spill-moves are needed. Instead of restore-moves the {@link #materializedValue} is restored.
    ///
    public boolean canMaterialize()
    {
        return getMaterializedValue() != null;
    }

    ///
    // Returns a value which can be moved to a register instead of a restore-move from stack.
    ///
    public Constant getMaterializedValue()
    {
        return splitParent().___materializedValue;
    }

    int calcTo()
    {
        Range __r = this.___first;
        while (!__r.___next.isEndMarker())
        {
            __r = __r.___next;
        }
        return __r.___to;
    }

    public Interval locationHint(boolean __searchSplitChild)
    {
        if (!__searchSplitChild)
        {
            return this.___locationHint;
        }

        if (this.___locationHint != null)
        {
            if (this.___locationHint.___location != null && ValueUtil.isRegister(this.___locationHint.___location))
            {
                return this.___locationHint;
            }
            else if (!this.___locationHint.___splitChildren.isEmpty())
            {
                // search the first split child that has a register assigned
                int __len = this.___locationHint.___splitChildren.size();
                for (int __i = 0; __i < __len; __i++)
                {
                    Interval __interval = this.___locationHint.___splitChildren.get(__i);
                    if (__interval.___location != null && ValueUtil.isRegister(__interval.___location))
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
        if (this.___splitChildren.isEmpty())
        {
            return this;
        }
        else
        {
            Interval __result = null;
            int __len = this.___splitChildren.size();

            // in outputMode, the end of the interval (opId == cur.to()) is not valid
            int __toOffset = (__mode == LIRInstruction.OperandMode.DEF ? 0 : 1);

            int __i;
            for (__i = 0; __i < __len; __i++)
            {
                Interval __cur = this.___splitChildren.get(__i);
                if (__cur.from() <= __opId && __opId < __cur.to() + __toOffset)
                {
                    if (__i > 0)
                    {
                        // exchange current split child to start of list (faster access for next call)
                        Util.atPutGrow(this.___splitChildren, __i, this.___splitChildren.get(0), null);
                        Util.atPutGrow(this.___splitChildren, 0, __cur, null);
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

        int __len = __parent.___splitChildren.size();

        for (int __i = __len - 1; __i >= 0; __i--)
        {
            Interval __cur = __parent.___splitChildren.get(__i);
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

        int __len = __parent.___splitChildren.size();

        for (int __i = __len - 1; __i >= 0; __i--)
        {
            Interval __cur = __parent.___splitChildren.get(__i);
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
        if (this.___splitChildren.isEmpty())
        {
            // simple case if interval was not split
            return covers(__opId, __mode);
        }
        else
        {
            // extended case: check all split children
            int __len = this.___splitChildren.size();
            for (int __i = 0; __i < __len; __i++)
            {
                Interval __cur = this.___splitChildren.get(__i);
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
        // In case of re-materialized values we require that use-operands are registers,
        // because we don't have the value in a stack location.
        // (Note that ShouldHaveRegister means that the operand can also be a StackSlot).
        if (__priority == RegisterPriority.ShouldHaveRegister && canMaterialize())
        {
            return RegisterPriority.MustHaveRegister;
        }
        return __priority;
    }

    // note: use positions are sorted descending = first use has highest index
    int firstUsage(RegisterPriority __minRegisterPriority)
    {
        for (int __i = this.___usePosList.size() - 1; __i >= 0; --__i)
        {
            RegisterPriority __registerPriority = adaptPriority(this.___usePosList.registerPriority(__i));
            if (__registerPriority.greaterEqual(__minRegisterPriority))
            {
                return this.___usePosList.usePos(__i);
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsage(RegisterPriority __minRegisterPriority, int __from)
    {
        for (int __i = this.___usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = this.___usePosList.usePos(__i);
            if (__usePos >= __from && adaptPriority(this.___usePosList.registerPriority(__i)).greaterEqual(__minRegisterPriority))
            {
                return __usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int nextUsageExact(RegisterPriority __exactRegisterPriority, int __from)
    {
        for (int __i = this.___usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = this.___usePosList.usePos(__i);
            if (__usePos >= __from && adaptPriority(this.___usePosList.registerPriority(__i)) == __exactRegisterPriority)
            {
                return __usePos;
            }
        }
        return Integer.MAX_VALUE;
    }

    int previousUsage(RegisterPriority __minRegisterPriority, int __from)
    {
        int __prev = -1;
        for (int __i = this.___usePosList.size() - 1; __i >= 0; --__i)
        {
            int __usePos = this.___usePosList.usePos(__i);
            if (__usePos > __from)
            {
                return __prev;
            }
            if (adaptPriority(this.___usePosList.registerPriority(__i)).greaterEqual(__minRegisterPriority))
            {
                __prev = __usePos;
            }
        }
        return __prev;
    }

    public void addUsePos(int __pos, RegisterPriority __registerPriority)
    {
        // do not add use positions for precolored intervals because they are never used
        if (__registerPriority != RegisterPriority.None && LIRValueUtil.isVariable(this.___operand))
        {
            // note: addUse is called in descending order, so list gets sorted automatically by just appending new use positions
            int __len = this.___usePosList.size();
            if (__len == 0 || this.___usePosList.usePos(__len - 1) > __pos)
            {
                this.___usePosList.add(__pos, __registerPriority);
            }
            else if (this.___usePosList.registerPriority(__len - 1).lessThan(__registerPriority))
            {
                this.___usePosList.setRegisterPriority(__len - 1, __registerPriority);
            }
        }
    }

    public void addRange(int __from, int __to)
    {
        if (this.___first.___from <= __to)
        {
            // join intersecting ranges
            this.___first.___from = Math.min(__from, first().___from);
            this.___first.___to = Math.max(__to, first().___to);
        }
        else
        {
            // insert new range
            this.___first = new Range(__from, __to, first());
        }
    }

    Interval newSplitChild(LinearScan __allocator)
    {
        // allocate new interval
        Interval __parent = splitParent();
        Interval __result = __allocator.createDerivedInterval(__parent);
        __result.setKind(kind());

        __result.___splitParent = __parent;
        __result.setLocationHint(__parent);

        // insert new interval in children-list of parent
        if (__parent.___splitChildren.isEmpty())
        {
            // create new non-shared list
            __parent.___splitChildren = new ArrayList<>(4);
            __parent.___splitChildren.add(this);
        }
        __parent.___splitChildren.add(__result);

        return __result;
    }

    ///
    // Splits this interval at a specified position and returns the remainder as a new <i>child</i>
    // interval of this interval's {@linkplain #splitParent() parent} interval.
    //
    // When an interval is split, a bi-directional link is established between the original
    // <i>parent</i> interval and the <i>children</i> intervals that are split off this interval.
    // When a split child is split again, the new created interval is a direct child of the original
    // parent. That is, there is no tree of split children stored, just a flat list. All split
    // children are spilled to the same {@linkplain #spillSlot spill slot}.
    //
    // @param splitPos the position at which to split this interval
    // @param allocator the register allocator context
    // @return the child interval split off from this interval
    ///
    Interval split(int __splitPos, LinearScan __allocator)
    {
        // allocate new interval
        Interval __result = newSplitChild(__allocator);

        // split the ranges
        Range __prev = null;
        Range __cur = this.___first;
        while (!__cur.isEndMarker() && __cur.___to <= __splitPos)
        {
            __prev = __cur;
            __cur = __cur.___next;
        }

        if (__cur.___from < __splitPos)
        {
            __result.___first = new Range(__splitPos, __cur.___to, __cur.___next);
            __cur.___to = __splitPos;
            __cur.___next = __allocator.___rangeEndMarker;
        }
        else
        {
            __result.___first = __cur;
            __prev.___next = __allocator.___rangeEndMarker;
        }
        __result.___current = __result.___first;
        this.___cachedTo = -1; // clear cached value

        // split list of use positions
        __result.___usePosList = this.___usePosList.splitAt(__splitPos);

        return __result;
    }

    ///
    // Splits this interval at a specified position and returns the head as a new interval (this interval is the tail).
    //
    // Currently, only the first range can be split, and the new interval must not have split positions
    ///
    Interval splitFromStart(int __splitPos, LinearScan __allocator)
    {
        // allocate new interval
        Interval __result = newSplitChild(__allocator);

        // the new interval has only one range (checked by assertion above,
        // so the splitting of the ranges is very simple
        __result.addRange(this.___first.___from, __splitPos);

        if (__splitPos == this.___first.___to)
        {
            this.___first = this.___first.___next;
        }
        else
        {
            this.___first.___from = __splitPos;
        }

        return __result;
    }

    // returns true if the opId is inside the interval
    boolean covers(int __opId, LIRInstruction.OperandMode __mode)
    {
        Range __cur = this.___first;

        while (!__cur.isEndMarker() && __cur.___to < __opId)
        {
            __cur = __cur.___next;
        }
        if (!__cur.isEndMarker())
        {
            if (__mode == LIRInstruction.OperandMode.DEF)
            {
                return __cur.___from <= __opId && __opId < __cur.___to;
            }
            else
            {
                return __cur.___from <= __opId && __opId <= __cur.___to;
            }
        }
        return false;
    }

    // returns true if the interval has any hole between holeFrom and holeTo
    // (even if the hole has only the length 1)
    boolean hasHoleBetween(int __holeFrom, int __holeTo)
    {
        Range __cur = this.___first;
        while (!__cur.isEndMarker())
        {
            // hole-range starts before this range . hole
            if (__holeFrom < __cur.___from)
            {
                return true;

                // hole-range completely inside this range . no hole
            }
            else
            {
                if (__holeTo <= __cur.___to)
                {
                    return false;

                    // overlapping of hole-range with this range . hole
                }
                else
                {
                    if (__holeFrom <= __cur.___to)
                    {
                        return true;
                    }
                }
            }

            __cur = __cur.___next;
        }

        return false;
    }

    ///
    // Gets the use position information for this interval.
    ///
    public UsePosList usePosList()
    {
        return this.___usePosList;
    }

    List<Interval> getSplitChildren()
    {
        return Collections.unmodifiableList(this.___splitChildren);
    }
}
