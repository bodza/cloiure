package graalvm.compiler.lir.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.lir.gen.LIRGenerationResult;

public class LIRPhaseSuite<C> extends LIRPhase<C>
{
    private List<LIRPhase<C>> phases;
    private boolean immutable;

    public LIRPhaseSuite()
    {
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
    public final void prependPhase(LIRPhase<C> phase)
    {
        phases.add(0, phase);
    }

    /**
     * Add a new phase at the end of this suite.
     */
    public final void appendPhase(LIRPhase<C> phase)
    {
        phases.add(phase);
    }

    public final ListIterator<LIRPhase<C>> findPhase(Class<? extends LIRPhase<C>> phaseClass)
    {
        ListIterator<LIRPhase<C>> it = phases.listIterator();
        if (findNextPhase(it, phaseClass))
        {
            return it;
        }
        else
        {
            return null;
        }
    }

    public final <T extends LIRPhase<C>> T findPhaseInstance(Class<T> phaseClass)
    {
        ListIterator<LIRPhase<C>> it = phases.listIterator();
        while (it.hasNext())
        {
            LIRPhase<C> phase = it.next();
            if (phaseClass.isInstance(phase))
            {
                return phaseClass.cast(phase);
            }
        }
        return null;
    }

    public static <C> boolean findNextPhase(ListIterator<LIRPhase<C>> it, Class<? extends LIRPhase<C>> phaseClass)
    {
        while (it.hasNext())
        {
            LIRPhase<C> phase = it.next();
            if (phaseClass.isInstance(phase))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected final void run(TargetDescription target, LIRGenerationResult lirGenRes, C context)
    {
        for (LIRPhase<C> phase : phases)
        {
            phase.apply(target, lirGenRes, context);
        }
    }

    public LIRPhaseSuite<C> copy()
    {
        LIRPhaseSuite<C> suite = new LIRPhaseSuite<>();
        suite.phases.addAll(phases);
        return suite;
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
