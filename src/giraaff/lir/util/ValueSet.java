package giraaff.lir.util;

import jdk.vm.ci.meta.Value;

// @class ValueSet
public abstract class ValueSet<S extends ValueSet<S>>
{
    public abstract void put(Value __v);

    public abstract void remove(Value __v);

    public abstract void putAll(S __s);

    public abstract S copy();
}
