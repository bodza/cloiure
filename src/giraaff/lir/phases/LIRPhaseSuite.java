package giraaff.lir.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import jdk.vm.ci.code.TargetDescription;

import giraaff.lir.gen.LIRGenerationResult;

// @class LIRPhaseSuite
public class LIRPhaseSuite<C> extends LIRPhase<C>
{
    // @field
    private List<LIRPhase<C>> phases;
    // @field
    private boolean immutable;

    // @cons
    public LIRPhaseSuite()
    {
        super();
        phases = new ArrayList<>();
    }

    /**
     * Gets an unmodifiable view on the phases in this suite.
     */
    public List<LIRPhase<C>> getPhases()
    {
        return Collections.unmodifiableList(phases);
    }

    /**
     * Add a new phase at the beginning of this suite.
     */
    public final void prependPhase(LIRPhase<C> __phase)
    {
        phases.add(0, __phase);
    }

    /**
     * Add a new phase at the end of this suite.
     */
    public final void appendPhase(LIRPhase<C> __phase)
    {
        phases.add(__phase);
    }

    public final ListIterator<LIRPhase<C>> findPhase(Class<? extends LIRPhase<C>> __phaseClass)
    {
        ListIterator<LIRPhase<C>> __it = phases.listIterator();
        if (findNextPhase(__it, __phaseClass))
        {
            return __it;
        }
        else
        {
            return null;
        }
    }

    public final <T extends LIRPhase<C>> T findPhaseInstance(Class<T> __phaseClass)
    {
        ListIterator<LIRPhase<C>> __it = phases.listIterator();
        while (__it.hasNext())
        {
            LIRPhase<C> __phase = __it.next();
            if (__phaseClass.isInstance(__phase))
            {
                return __phaseClass.cast(__phase);
            }
        }
        return null;
    }

    public static <C> boolean findNextPhase(ListIterator<LIRPhase<C>> __it, Class<? extends LIRPhase<C>> __phaseClass)
    {
        while (__it.hasNext())
        {
            LIRPhase<C> __phase = __it.next();
            if (__phaseClass.isInstance(__phase))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected final void run(TargetDescription __target, LIRGenerationResult __lirGenRes, C __context)
    {
        for (LIRPhase<C> __phase : phases)
        {
            __phase.apply(__target, __lirGenRes, __context);
        }
    }

    public LIRPhaseSuite<C> copy()
    {
        LIRPhaseSuite<C> __suite = new LIRPhaseSuite<>();
        __suite.phases.addAll(phases);
        return __suite;
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
}
