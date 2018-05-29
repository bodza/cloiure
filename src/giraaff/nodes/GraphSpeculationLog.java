package giraaff.nodes;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SpeculationLog;

import org.graalvm.collections.EconomicMap;

/**
 * A wrapper around a {@link SpeculationLog} instance.
 *
 * This should be used when the wrapped instance may be accessed by multiple threads. Due to races,
 * such an instance can return true for a call to {@link SpeculationLog#maySpeculate} but still fail
 * (i.e. raise an {@link IllegalArgumentException}) when {@link SpeculationLog#speculate} is called.
 *
 * A {@link GraphSpeculationLog} must only be used by a single thread and is typically closely
 * coupled with a {@link StructuredGraph} (hence the name).
 */
// @class GraphSpeculationLog
public final class GraphSpeculationLog implements SpeculationLog
{
    private final SpeculationLog log;
    private final EconomicMap<SpeculationReason, JavaConstant> speculations;

    // @cons
    public GraphSpeculationLog(SpeculationLog log)
    {
        super();
        this.log = log;
        this.speculations = EconomicMap.create();
    }

    /**
     * Unwraps {@code log} if it is a {@link GraphSpeculationLog}.
     */
    public static SpeculationLog unwrap(SpeculationLog log)
    {
        if (log instanceof GraphSpeculationLog)
        {
            return ((GraphSpeculationLog) log).log;
        }
        return log;
    }

    /**
     * Determines if the compiler is allowed to speculate with {@code reason}. Note that a
     * {@code true} return value guarantees that a subsequent call to
     * {@link #speculate(SpeculationReason)} with an argument {@linkplain Object#equals(Object)
     * equal} to {@code reason} will succeed.
     */
    @Override
    public boolean maySpeculate(SpeculationReason reason)
    {
        JavaConstant speculation = speculations.get(reason);
        if (speculation == null)
        {
            if (log.maySpeculate(reason))
            {
                try
                {
                    speculation = log.speculate(reason);
                    speculations.put(reason, speculation);
                }
                catch (IllegalArgumentException e)
                {
                    // the speculation was disabled by another thread in between the call to log.maySpeculate and log.speculate
                    speculation = null;
                }
            }
        }
        return speculation != null;
    }

    @Override
    public JavaConstant speculate(SpeculationReason reason)
    {
        if (maySpeculate(reason))
        {
            return speculations.get(reason);
        }
        throw new IllegalArgumentException("Cannot make speculation with reason " + reason + " as it is known to fail");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof GraphSpeculationLog)
        {
            GraphSpeculationLog that = (GraphSpeculationLog) obj;
            return this.log == that.log;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return log.hashCode();
    }

    @Override
    public void collectFailedSpeculations()
    {
        log.collectFailedSpeculations();
    }

    /**
     * Returns if this log has speculations.
     *
     * @return true if {@link #maySpeculate(SpeculationReason)} has ever returned {@code true} for
     *         this object
     */
    @Override
    public boolean hasSpeculations()
    {
        return !speculations.isEmpty();
    }
}
