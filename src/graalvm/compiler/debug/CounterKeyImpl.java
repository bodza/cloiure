package graalvm.compiler.debug;

import org.graalvm.collections.Pair;

class CounterKeyImpl extends AbstractKey implements CounterKey
{
    CounterKeyImpl(String format, Object arg1, Object arg2)
    {
        super(format, arg1, arg2);
    }

    @Override
    public void increment(DebugContext debug)
    {
        add(debug, 1);
    }

    @Override
    public Pair<String, String> toCSVFormat(long value)
    {
        return Pair.create(String.valueOf(value), "");
    }

    @Override
    public String toHumanReadableFormat(long value)
    {
        return Long.toString(value);
    }

    @Override
    public void add(DebugContext debug, long value)
    {
        if (debug.isCounterEnabled(this))
        {
            addToCurrentValue(debug, value);
        }
    }

    @Override
    public boolean isEnabled(DebugContext debug)
    {
        return debug.isCounterEnabled(this);
    }

    @Override
    public CounterKey doc(String doc)
    {
        setDoc(doc);
        return this;
    }
}
