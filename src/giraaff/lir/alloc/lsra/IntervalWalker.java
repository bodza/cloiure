package giraaff.lir.alloc.lsra;

import giraaff.lir.alloc.lsra.Interval;

// @class IntervalWalker
public class IntervalWalker
{
    // @field
    protected final LinearScan ___allocator;

    ///
    // Sorted list of intervals, not live before the current position.
    ///
    // @field
    protected Interval.RegisterBindingLists ___unhandledLists;

    ///
    // Sorted list of intervals, live at the current position.
    ///
    // @field
    protected Interval.RegisterBindingLists ___activeLists;

    ///
    // Sorted list of intervals in a life time hole at the current position.
    ///
    // @field
    protected Interval.RegisterBindingLists ___inactiveLists;

    ///
    // The current position (intercept point through the intervals).
    ///
    // @field
    protected int ___currentPosition;

    ///
    // The binding of the current interval being processed.
    ///
    // @field
    protected Interval.RegisterBinding ___currentBinding;

    ///
    // Processes the {@code currentInterval} interval in an attempt to allocate a physical register
    // to it and thus allow it to be moved to a list of {@linkplain #activeLists active} intervals.
    //
    // @return {@code true} if a register was allocated to the {@code currentInterval} interval
    ///
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

    ///
    // Creates a new interval walker.
    //
    // @param allocator the register allocator context
    // @param unhandledFixed the list of unhandled {@linkplain Interval.RegisterBinding#Fixed fixed} intervals
    // @param unhandledAny the list of unhandled {@linkplain Interval.RegisterBinding#Any non-fixed} intervals
    ///
    // @cons IntervalWalker
    IntervalWalker(LinearScan __allocator, Interval __unhandledFixed, Interval __unhandledAny)
    {
        super();
        this.___allocator = __allocator;

        this.___unhandledLists = new Interval.RegisterBindingLists(__unhandledFixed, __unhandledAny, __allocator.___intervalEndMarker);
        this.___activeLists = new Interval.RegisterBindingLists(__allocator.___intervalEndMarker, __allocator.___intervalEndMarker, __allocator.___intervalEndMarker);
        this.___inactiveLists = new Interval.RegisterBindingLists(__allocator.___intervalEndMarker, __allocator.___intervalEndMarker, __allocator.___intervalEndMarker);
        this.___currentPosition = -1;
    }

    protected void removeFromList(Interval __interval)
    {
        if (__interval.___state == Interval.IntervalState.Active)
        {
            this.___activeLists.remove(Interval.RegisterBinding.Any, __interval);
        }
        else
        {
            this.___inactiveLists.remove(Interval.RegisterBinding.Any, __interval);
        }
    }

    private void walkTo(Interval.IntervalState __state, int __from)
    {
        for (Interval.RegisterBinding __binding : Interval.RegisterBinding.VALUES)
        {
            walkTo(__state, __from, __binding);
        }
    }

    private void walkTo(Interval.IntervalState __state, int __from, Interval.RegisterBinding __binding)
    {
        Interval __prevprev = null;
        Interval __prev = (__state == Interval.IntervalState.Active) ? this.___activeLists.get(__binding) : this.___inactiveLists.get(__binding);
        Interval __next = __prev;
        while (__next.currentFrom() <= __from)
        {
            Interval __cur = __next;
            __next = __cur.___next;

            boolean __rangeHasChanged = false;
            while (__cur.currentTo() <= __from)
            {
                __cur.nextRange();
                __rangeHasChanged = true;
            }

            // also handle move from inactive list to active list
            __rangeHasChanged = __rangeHasChanged || (__state == Interval.IntervalState.Inactive && __cur.currentFrom() <= __from);

            if (__rangeHasChanged)
            {
                // remove cur from list
                if (__prevprev == null)
                {
                    if (__state == Interval.IntervalState.Active)
                    {
                        this.___activeLists.set(__binding, __next);
                    }
                    else
                    {
                        this.___inactiveLists.set(__binding, __next);
                    }
                }
                else
                {
                    __prevprev.___next = __next;
                }
                __prev = __next;
                Interval.IntervalState __newState;
                if (__cur.currentAtEnd())
                {
                    // move to handled state (not maintained as a list)
                    __newState = Interval.IntervalState.Handled;
                    __cur.___state = __newState;
                }
                else
                {
                    if (__cur.currentFrom() <= __from)
                    {
                        // sort into active list
                        this.___activeLists.addToListSortedByCurrentFromPositions(__binding, __cur);
                        __newState = Interval.IntervalState.Active;
                    }
                    else
                    {
                        // sort into inactive list
                        this.___inactiveLists.addToListSortedByCurrentFromPositions(__binding, __cur);
                        __newState = Interval.IntervalState.Inactive;
                    }
                    __cur.___state = __newState;
                    if (__prev == __cur)
                    {
                        __prevprev = __prev;
                        __prev = __cur.___next;
                    }
                }
                intervalMoved(__cur, __state, __newState);
            }
            else
            {
                __prevprev = __prev;
                __prev = __cur.___next;
            }
        }
    }

    ///
    // Get the next interval from {@linkplain #unhandledLists} which starts before or at
    // {@code toOpId}. The returned interval is removed and {@link #currentBinding} is set.
    //
    // @postcondition all intervals in {@linkplain #unhandledLists} start after {@code toOpId}.
    //
    // @return The next interval or null if there is no {@linkplain #unhandledLists unhandled}
    //         interval at position {@code toOpId}.
    ///
    private Interval nextInterval(int __toOpId)
    {
        Interval.RegisterBinding __binding;
        Interval __any = this.___unhandledLists.___any;
        Interval __fixed = this.___unhandledLists.___fixed;

        if (!__any.isEndMarker())
        {
            // intervals may start at same position . prefer fixed interval
            __binding = !__fixed.isEndMarker() && __fixed.from() <= __any.from() ? Interval.RegisterBinding.Fixed : Interval.RegisterBinding.Any;
        }
        else if (!__fixed.isEndMarker())
        {
            __binding = Interval.RegisterBinding.Fixed;
        }
        else
        {
            return null;
        }
        Interval __currentInterval = this.___unhandledLists.get(__binding);

        if (__toOpId < __currentInterval.from())
        {
            return null;
        }

        this.___currentBinding = __binding;
        this.___unhandledLists.set(__binding, __currentInterval.___next);
        __currentInterval.___next = this.___allocator.___intervalEndMarker;
        __currentInterval.rewindRange();
        return __currentInterval;
    }

    ///
    // Walk up to {@code toOpId}.
    //
    // @postcondition {@link #currentPosition} is set to {@code toOpId}, {@link #activeLists} and
    //                {@link #inactiveLists} are populated and {@link Interval#state}s are up to date.
    ///
    protected void walkTo(int __toOpId)
    {
        for (Interval __currentInterval = nextInterval(__toOpId); __currentInterval != null; __currentInterval = nextInterval(__toOpId))
        {
            int __opId = __currentInterval.from();

            // set currentPosition prior to call of walkTo
            this.___currentPosition = __opId;

            // update unhandled stack intervals
            updateUnhandledStackIntervals(__opId);

            // call walkTo even if currentPosition == id
            walkTo(Interval.IntervalState.Active, __opId);
            walkTo(Interval.IntervalState.Inactive, __opId);

            __currentInterval.___state = Interval.IntervalState.Active;
            if (activateCurrent(__currentInterval))
            {
                this.___activeLists.addToListSortedByCurrentFromPositions(this.___currentBinding, __currentInterval);
                intervalMoved(__currentInterval, Interval.IntervalState.Unhandled, Interval.IntervalState.Active);
            }
        }
        // set currentPosition prior to call of walkTo
        this.___currentPosition = __toOpId;

        if (this.___currentPosition <= this.___allocator.maxOpId())
        {
            // update unhandled stack intervals
            updateUnhandledStackIntervals(__toOpId);

            // call walkTo if still in range
            walkTo(Interval.IntervalState.Active, __toOpId);
            walkTo(Interval.IntervalState.Inactive, __toOpId);
        }
    }

    private void intervalMoved(Interval __interval, Interval.IntervalState __from, Interval.IntervalState __to)
    {
        // intervalMoved() is called whenever an interval moves from one interval list to another.
        // In the implementation of this method it is prohibited to move the interval to any list.
    }

    ///
    // Move {@linkplain #unhandledLists unhandled} stack intervals to
    // {@linkplain IntervalWalker #activeLists active}.
    //
    // Note that for {@linkplain Interval.RegisterBinding#Fixed fixed} and {@linkplain Interval.RegisterBinding#Any
    // any} intervals this is done in {@link #nextInterval(int)}.
    ///
    private void updateUnhandledStackIntervals(int __opId)
    {
        Interval __currentInterval = this.___unhandledLists.get(Interval.RegisterBinding.Stack);
        while (!__currentInterval.isEndMarker() && __currentInterval.from() <= __opId)
        {
            Interval __next = __currentInterval.___next;
            if (__currentInterval.to() > __opId)
            {
                __currentInterval.___state = Interval.IntervalState.Active;
                this.___activeLists.addToListSortedByCurrentFromPositions(Interval.RegisterBinding.Stack, __currentInterval);
                intervalMoved(__currentInterval, Interval.IntervalState.Unhandled, Interval.IntervalState.Active);
            }
            else
            {
                __currentInterval.___state = Interval.IntervalState.Handled;
                intervalMoved(__currentInterval, Interval.IntervalState.Unhandled, Interval.IntervalState.Handled);
            }
            __currentInterval = __next;
        }
        this.___unhandledLists.set(Interval.RegisterBinding.Stack, __currentInterval);
    }
}
