package graalvm.compiler.core.common.util;

import graalvm.compiler.debug.Assertions;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

/**
 * Utility class that allows the compiler to monitor compilations that take a very long time.
 */
public final class CompilationAlarm implements AutoCloseable
{
    public static class Options
    {
        @Option(help = "Time limit in seconds before a compilation expires (0 to disable the limit). " +
                       "The compilation alarm will be implicitly disabled if assertions are enabled.", type = OptionType.Debug)
        public static final OptionKey<Integer> CompilationExpirationPeriod = new OptionKey<>(300);
    }

    private CompilationAlarm(long expiration)
    {
        this.expiration = expiration;
    }

    /**
     * Thread local storage for the active compilation alarm.
     */
    private static final ThreadLocal<CompilationAlarm> currentAlarm = new ThreadLocal<>();

    private static final CompilationAlarm NEVER_EXPIRES = new CompilationAlarm(0);

    /**
     * Gets the current compilation alarm. If there is no current alarm, a non-null value is
     * returned that will always return {@code false} for {@link #hasExpired()}.
     */
    public static CompilationAlarm current()
    {
        CompilationAlarm alarm = currentAlarm.get();
        return alarm == null ? NEVER_EXPIRES : alarm;
    }

    /**
     * Determines if this alarm has expired. A compilation expires if it takes longer than
     * {@linkplain CompilationAlarm.Options#CompilationExpirationPeriod}.
     *
     * @return {@code true} if the current compilation already takes longer than
     *         {@linkplain CompilationAlarm.Options#CompilationExpirationPeriod}, {@code false}
     *         otherwise
     */
    public boolean hasExpired()
    {
        return this != NEVER_EXPIRES && System.currentTimeMillis() > expiration;
    }

    @Override
    public void close()
    {
        if (this != NEVER_EXPIRES)
        {
            currentAlarm.set(null);
        }
    }

    /**
     * The time at which this alarm expires.
     */
    private final long expiration;

    /**
     * Starts an alarm for setting a time limit on a compilation if there isn't already an active
     * alarm, if assertions are disabled and
     * {@link CompilationAlarm.Options#CompilationExpirationPeriod}{@code > 0}. The returned value
     * can be used in a try-with-resource statement to disable the alarm once the compilation is
     * finished.
     *
     * @return a {@link CompilationAlarm} if there was no current alarm for the calling thread
     *         before this call otherwise {@code null}
     */
    public static CompilationAlarm trackCompilationPeriod(OptionValues options)
    {
        int period = Assertions.assertionsEnabled() ? 0 : Options.CompilationExpirationPeriod.getValue(options);
        if (period > 0)
        {
            CompilationAlarm current = currentAlarm.get();
            if (current == null)
            {
                long expiration = System.currentTimeMillis() + period * 1000;
                current = new CompilationAlarm(expiration);
                currentAlarm.set(current);
                return current;
            }
        }
        return null;
    }
}
