package giraaff.core.common.alloc;

public class TraceMap<T>
{
    private final T[] data;

    @SuppressWarnings("unchecked")
    public TraceMap(TraceBuilderResult traceBuilderResult)
    {
        data = (T[]) new Object[traceBuilderResult.getTraces().size()];
    }

    public T get(Trace trace)
    {
        return data[trace.getId()];
    }

    public void put(Trace trace, T value)
    {
        data[trace.getId()] = value;
    }
}
