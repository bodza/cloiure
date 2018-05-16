package graalvm.compiler.lir.util;

import jdk.vm.ci.meta.Value;

public abstract class ValueSet<S extends ValueSet<S>>
{
    public abstract void put(Value v);

    public abstract void remove(Value v);

    public abstract void putAll(S s);

    public abstract S copy();
}
