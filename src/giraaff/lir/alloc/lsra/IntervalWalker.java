package giraaff.lir.alloc.lsra;

import giraaff.lir.alloc.lsra.Interval.RegisterBinding;
import giraaff.lir.alloc.lsra.Interval.RegisterBindingLists;
import giraaff.lir.alloc.lsra.Interval.State;

// @class IntervalWalker
public class IntervalWalker
{
    // @field
    protected final LinearScan allocator;

    /**
     * Sorted list of intervals, not live before the current position.
     */
    // @field
    protected RegisterBindingLists unhandledLists;

    /**
     * Sorted list of intervals, live at the current position.
     */
    // @field
    protected RegisterBindingLists activeLists;

    /**
     * Sorted list of intervals in a life time hole at the current position.
     */
    // @field
    protected RegisterBindingLists inactiveLists;

    /**
     * The current position (intercept point through the intervals).
     */
    // @field
    protected int currentPosition;

    /**
     * The binding of the current interval being processed.
     */
    // @field
    protected RegisterBinding currentBinding;

    /**
     * Processes the {@code currentInterval} interval in an attempt to allocate a physical register
     * to it and thus allow it to be moved to a list of {@linkplain #activeLists active} intervals.
     *
     * @return {@code true} if a register was allocated to the {@code currentInterval} interval
     */
    protected boolean activateCurrent(@SuppressWarnings({"unused"}) Interval __currentInterval)
    {
        return true;
    }

    void walkBefore(int __lirOpId)
    {
        walkTo(__lirOpId - 1);
    }

    void walk()
    {
        walkTo(Integer.MAX_VALUE);
    }

    /**
     * Creates a new interval walker.
     *
     * @param allocator the register allocator context
     * @param unhandledFixed the list of unhandled {@linkplain RegisterBinding#Fixed fixed} intervals
     * @param unhandledAny the list of unhandled {@linkplain RegisterBinding#Any non-fixed} intervals
     */
    // @cons
    IntervalWalker(LinearScan __allocator, Interval __unhandledFixed, Interval __unhandledAny)
    {
        super();
        this.allocator = __allocator;

        unhandledLists = new RegisterBindingLists(__unhandledFixed, __unhandledAny, __allocator.intervalEndMarker);
        activeLists = new RegisterBindingLists(__allocator.intervalEndMarker, __allocator.intervalEndMarker, __allocator.intervalEndMarker);
        inactiveLists = new RegisterBindingLists(__allocator.intervalEndMarker, __allocator.intervalEndMarker, __allocator.intervalEndMarker);
        currentPosition = -1;
    }

    protected void removeFromList(Interval __interval)
    {
        if (__interval.state == State.Active)
        {
            activeLists.remove(RegisterBinding.Any, __interval);
        }
        else
        {
            inactiveLists.remove(RegisterBinding.Any, __interval);
        }
    }

    private void walkTo(State __state, int __from)
    {
        for (RegisterBinding __binding : RegisterBinding.VALUES)
        {
            walkTo(__state, __from, __binding);
        }
    }

    private void walkTo(State __state, int __from, RegisterBinding __binding)
    {
        Interval __prevprev = null;
        Interval __prev = (__state == State.Active) ? activeLists.get(__binding) : inactiveLists.get(__binding);
        Interval __next = __prev;
        while (__next.currentFrom() <= __from)
        {
            Interval __cur = __next;
            __next = __cur.next;

            boolean __rangeHasChanged = false;
            while (__cur.currentTo() <= __from)
            {
                __cur.nextRange();
                __rangeHasChanged = true;
            }

            // also handle move from inactive list to active list
            __rangeHasChanged = __rangeHasChanged || (__state == State.Inactive && __cur.currentFrom() <= __from);

            if (__rangeHasChanged)
            {
                // remove cur from list
                if (__prevprev == null)
                {
                    if (__state == State.Active)
                    {
                        activeLists.set(__binding, __next);
                    }
                    else
                    {
                        inactiveLists.set(__binding, __next);
                    }
                }
                else
                {
                    __prevprev.next = __next;
                }
                __prev = __next;
                Interval.State __newState;
                if (__cur.currentAtEnd())
                {
                    // move to handled state (not maintained as a list)
                    __newState = State.Handled;
                    __cur.state = __newState;
                }
                else
                {
                    if (__cur.currentFrom() <= __from)
                    {
                        // sort into active list
                        activeLists.addToListSortedByCurrentFromPositions(__binding, __cur);
                        __newState = State.Active;
                    }
                    else
                    {
                        // sort into inactive list
                        inactiveLists.addToListSortedByCurrentFromPositions(__binding, __cur);
                        __newState = State.Inactive;
                    }
                    __cur.state = __newState;
                    if (__prev == __cur)
                    {
                        __prevprev = __prev;
                        __prev = __cur.next;
                    }
                }
                intervalMoved(__cur, __state, __newState);
            }
            else
            {
                __prevprev = __prev;
                __prev = __cur.next;
            }
        }
    }

    /**
     * Get the next interval from {@linkplain #unhandledLists} which starts before or at
     * {@code toOpId}. The returned interval is removed and {@link #currentBinding} is set.
     *
     * @postcondition all intervals in {@linkplain #unhandledLists} start after {@code toOpId}.
     *
     * @return The next interval or null if there is no {@linkplain #unhandledLists unhandled}
     *         interval at position {@code toOpId}.
     */
    private Interval nextInterval(int __toOpId)
    {
        RegisterBinding __binding;
        Interval __any = unhandledLists.any;
        Interval __fixed = unhandledLists.fixed;

        if (!__any.isEndMarker())
        {
            // intervals may start at same position . prefer fixed interval
            __binding = !__fixed.isEndMarker() && __fixed.from() <= __any.from() ? RegisterBinding.Fixed : RegisterBinding.Any;
        }
        else if (!__fixed.isEndMarker())
        {
            __binding = RegisterBinding.Fixed;
        }
        else
        {
            return null;
        }
        Interval __currentInterval = unhandledLists.get(__binding);

        if (__toOpId < __currentInterval.from())
        {
            return null;
        }

        currentBinding = __binding;
        unhandledLists.set(__binding, __currentInterval.next);
        __currentInterval.next = allocator.intervalEndMarker;
        __currentInterval.rewindRange();
        return __currentInterval;
    }

    /**
     * Walk up to {@code toOpId}.
     *
     * @postcondition {@link #currentPosition} is set to {@code toOpId}, {@link #activeLists} and
     *                {@link #inactiveLists} are populated and {@link Interval#state}s are up to date.
     */
    protected void walkTo(int __toOpId)
    {
        for (Interval __currentInterval = nextInterval(__toOpId); __currentInterval != null; __currentInterval = nextInterval(__toOpId))
        {
            int __opId = __currentInterval.from();

            // set currentPosition prior to call of walkTo
            currentPosition = __opId;

            // update unhandled stack intervals
            updateUnhandledStackIntervals(__opId);

            // call walkTo even if currentPosition == id
            walkTo(State.Active, __opId);
            walkTo(State.Inactive, __opId);

            __currentInterval.state = State.Active;
            if (activateCurrent(__currentInterval))
            {
                activeLists.addToListSortedByCurrentFromPositions(currentBinding, __currentInterval);
                intervalMoved(__currentInterval, State.Unhandled, State.Active);
            }
        }
        // set currentPosition prior to call of walkTo
        currentPosition = __toOpId;

        if (currentPosition <= allocator.maxOpId())
        {
            // update unhandled stack intervals
            updateUnhandledStackIntervals(__toOpId);

            // call walkTo if still in range
            walkTo(State.Active, __toOpId);
            walkTo(State.Inactive, __toOpId);
        }
    }

    private void intervalMoved(Interval __interval, State __from, State __to)
    {
        // intervalMoved() is called whenever an interval moves from one interval list to another.
        // In the implementation of this method it is prohibited to move the interval to any list.
    }

    /**
     * Move {@linkplain #unhandledLists unhandled} stack intervals to
     * {@linkplain IntervalWalker #activeLists active}.
     *
     * Note that for {@linkplain RegisterBinding#Fixed fixed} and {@linkplain RegisterBinding#Any
     * any} intervals this is done in {@link #nextInterval(int)}.
     */
    private void updateUnhandledStackIntervals(int __opId)
    {
        Interval __currentInterval = unhandledLists.get(RegisterBinding.Stack);
        while (!__currentInterval.isEndMarker() && __currentInterval.from() <= __opId)
        {
            Interval __next = __currentInterval.next;
            if (__currentInterval.to() > __opId)
            {
                __currentInterval.state = State.Active;
                activeLists.addToListSortedByCurrentFromPositions(RegisterBinding.Stack, __currentInterval);
                intervalMoved(__currentInterval, State.Unhandled, State.Active);
            }
            else
            {
                __currentInterval.state = State.Handled;
                intervalMoved(__currentInterval, State.Unhandled, State.Handled);
            }
            __currentInterval = __next;
        }
        unhandledLists.set(RegisterBinding.Stack, __currentInterval);
    }
}
