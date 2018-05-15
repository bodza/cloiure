package graalvm.compiler.debug;

/**
 * A counter for some value of interest.
 */
public interface CounterKey extends MetricKey {

    /**
     * Adds 1 to this counter.
     */
    void increment(DebugContext debug);

    /**
     * Adds {@code value} to this counter.
     */
    void add(DebugContext debug, long value);

    /**
     * Gets the current value of this counter.
     */
    long getCurrentValue(DebugContext debug);

    /**
     * Determines if this counter is enabled.
     */
    boolean isEnabled(DebugContext debug);

    @Override
    CounterKey doc(String string);
}
