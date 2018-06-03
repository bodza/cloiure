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
    // @field
    private final SpeculationLog log;
    // @field
    private final EconomicMap<SpeculationReason, JavaConstant> speculations;

    // @cons
    public GraphSpeculationLog(SpeculationLog __log)
    {
        super();
        this.log = __log;
        this.speculations = EconomicMap.create();
    }

    /**
     * Unwraps {@code log} if it is a {@link GraphSpeculationLog}.
     */
    public static SpeculationLog unwrap(SpeculationLog __log)
    {
        if (__log instanceof GraphSpeculationLog)
        {
            return ((GraphSpeculationLog) __log).log;
        }
        return __log;
    }

    /**
     * Determines if the compiler is allowed to speculate with {@code reason}. Note that a
     * {@code true} return value guarantees that a subsequent call to
     * {@link #speculate(SpeculationReason)} with an argument {@linkplain Object#equals(Object)
     * equal} to {@code reason} will succeed.
     */
    @Override
    public boolean maySpeculate(SpeculationReason __reason)
    {
        JavaConstant __speculation = speculations.get(__reason);
        if (__speculation == null)
        {
            if (log.maySpeculate(__reason))
            {
                try
                {
                    __speculation = log.speculate(__reason);
                    speculations.put(__reason, __speculation);
                }
                catch (IllegalArgumentException __e)
                {
                    // the speculation was disabled by another thread in between the call to log.maySpeculate and log.speculate
                    __speculation = null;
                }
            }
        }
        return __speculation != null;
    }

    @Override
    public JavaConstant speculate(SpeculationReason __reason)
    {
        if (maySpeculate(__reason))
        {
            return speculations.get(__reason);
        }
        throw new IllegalArgumentException("Cannot make speculation with reason " + __reason + " as it is known to fail");
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof GraphSpeculationLog)
        {
            GraphSpeculationLog __that = (GraphSpeculationLog) __obj;
            return this.log == __that.log;
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
