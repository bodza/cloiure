package graalvm.compiler.lir.util;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import jdk.vm.ci.meta.Value;

public final class GenericValueMap<T> extends ValueMap<Value, T> {

    private final EconomicMap<Value, T> data;

    public GenericValueMap() {
        data = EconomicMap.create(Equivalence.DEFAULT);
    }

    @Override
    public T get(Value value) {
        return data.get(value);
    }

    @Override
    public void remove(Value value) {
        data.removeKey(value);
    }

    @Override
    public void put(Value value, T object) {
        data.put(value, object);
    }

}
