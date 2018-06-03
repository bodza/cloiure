package giraaff.lir.util;

import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

// @class GenericValueMap
public final class GenericValueMap<T> extends ValueMap<Value, T>
{
    // @field
    private final EconomicMap<Value, T> data;

    // @cons
    public GenericValueMap()
    {
        super();
        data = EconomicMap.create(Equivalence.DEFAULT);
    }

    @Override
    public T get(Value __value)
    {
        return data.get(__value);
    }

    @Override
    public void remove(Value __value)
    {
        data.removeKey(__value);
    }

    @Override
    public void put(Value __value, T __object)
    {
        data.put(__value, __object);
    }
}
