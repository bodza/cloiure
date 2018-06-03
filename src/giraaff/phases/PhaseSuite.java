package giraaff.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import giraaff.nodes.StructuredGraph;

/**
 * A compiler phase that can apply an ordered collection of phases to a graph.
 */
// @class PhaseSuite
public class PhaseSuite<C> extends BasePhase<C>
{
    // @field
    private List<BasePhase<? super C>> phases;
    // @field
    private boolean immutable;

    // @cons
    public PhaseSuite()
    {
        super();
        this.phases = new ArrayList<>();
    }

    public boolean isImmutable()
    {
        return immutable;
    }

    public synchronized void setImmutable()
    {
        if (!immutable)
        {
            phases = Collections.unmodifiableList(phases);
            immutable = true;
        }
    }

    /**
     * Add a new phase at the beginning of this suite.
     */
    public final void prependPhase(BasePhase<? super C> __phase)
    {
        phases.add(0, __phase);
    }

    /**
     * Add a new phase at the end of this suite.
     */
    public final void appendPhase(BasePhase<? super C> __phase)
    {
        phases.add(__phase);
    }

    /**
     * Inserts a phase before the last phase in the suite. If the suite contains no phases the new
     * phase will be inserted as the first phase.
     */
    public final void addBeforeLast(BasePhase<? super C> __phase)
    {
        ListIterator<BasePhase<? super C>> __last = findLastPhase();
        if (__last.hasPrevious())
        {
            __last.previous();
        }
        __last.add(__phase);
    }

    /**
     * Returns a {@link ListIterator} at the position of the last phase in the suite. If the suite
     * has no phases then it will return an empty iterator.
     */
    private ListIterator<BasePhase<? super C>> findLastPhase()
    {
        ListIterator<BasePhase<? super C>> __it = phases.listIterator();
        while (__it.hasNext())
        {
            __it.next();
        }
        return __it;
    }

    /**
     * Gets an unmodifiable view on the phases in this suite.
     */
    public List<BasePhase<? super C>> getPhases()
    {
        return Collections.unmodifiableList(phases);
    }

    /**
     * Returns a {@link ListIterator} at the position of the first phase which is an instance of
     * {@code phaseClass} or null if no such phase can be found.
     *
     * Calling {@link ListIterator#previous()} would return the phase that was found.
     *
     * @param phaseClass the type of phase to look for.
     */
    public final ListIterator<BasePhase<? super C>> findPhase(Class<? extends BasePhase<? super C>> __phaseClass)
    {
        return findPhase(__phaseClass, false);
    }

    /**
     * Returns a {@link ListIterator} at the position of the first phase which is an instance of
     * {@code phaseClass} or, if {@code recursive} is true, is a {@link PhaseSuite} containing a
     * phase which is an instance of {@code phaseClass}. This method returns null if no such phase
     * can be found.
     *
     * Calling {@link ListIterator#previous()} would return the phase or phase suite that was found.
     *
     * @param phaseClass the type of phase to look for
     * @param recursive whether to recursively look into phase suites.
     */
    public final ListIterator<BasePhase<? super C>> findPhase(Class<? extends BasePhase<? super C>> __phaseClass, boolean __recursive)
    {
        ListIterator<BasePhase<? super C>> __it = phases.listIterator();
        if (findNextPhase(__it, __phaseClass, __recursive))
        {
            return __it;
        }
        else
        {
            return null;
        }
    }

    public static <C> boolean findNextPhase(ListIterator<BasePhase<? super C>> __it, Class<? extends BasePhase<? super C>> __phaseClass)
    {
        return findNextPhase(__it, __phaseClass, false);
    }

    @SuppressWarnings("unchecked")
    public static <C> boolean findNextPhase(ListIterator<BasePhase<? super C>> __it, Class<? extends BasePhase<? super C>> __phaseClass, boolean __recursive)
    {
        while (__it.hasNext())
        {
            BasePhase<? super C> __phase = __it.next();
            if (__phaseClass.isInstance(__phase))
            {
                return true;
            }
            else if (__recursive && __phase instanceof PhaseSuite)
            {
                PhaseSuite<C> __suite = (PhaseSuite<C>) __phase;
                if (__suite.findPhase(__phaseClass, true) != null)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the first instance of the given phase class, looking recursively into inner phase suites.
     */
    @SuppressWarnings("unchecked")
    public boolean removePhase(Class<? extends BasePhase<? super C>> __phaseClass)
    {
        ListIterator<BasePhase<? super C>> __it = phases.listIterator();
        while (__it.hasNext())
        {
            BasePhase<? super C> __phase = __it.next();
            if (__phaseClass.isInstance(__phase))
            {
                __it.remove();
                return true;
            }
            else if (__phase instanceof PhaseSuite)
            {
                PhaseSuite<C> __innerSuite = (PhaseSuite<C>) __phase;
                if (__innerSuite.removePhase(__phaseClass))
                {
                    if (__innerSuite.phases.isEmpty())
                    {
                        __it.remove();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the first instance of the given phase class, looking recursively into inner phase suites.
     */
    @SuppressWarnings("unchecked")
    public boolean replacePhase(Class<? extends BasePhase<? super C>> __phaseClass, BasePhase<? super C> __newPhase)
    {
        ListIterator<BasePhase<? super C>> __it = phases.listIterator();
        while (__it.hasNext())
        {
            BasePhase<? super C> __phase = __it.next();
            if (__phaseClass.isInstance(__phase))
            {
                __it.set(__newPhase);
                return true;
            }
            else if (__phase instanceof PhaseSuite)
            {
                PhaseSuite<C> __innerSuite = (PhaseSuite<C>) __phase;
                if (__innerSuite.removePhase(__phaseClass))
                {
                    if (__innerSuite.phases.isEmpty())
                    {
                        __it.set(__newPhase);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void run(StructuredGraph __graph, C __context)
    {
        for (BasePhase<? super C> __phase : phases)
        {
            __phase.apply(__graph, __context);
        }
    }

    public PhaseSuite<C> copy()
    {
        PhaseSuite<C> __suite = new PhaseSuite<>();
        __suite.phases.addAll(phases);
        return __suite;
    }
}
