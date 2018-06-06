package giraaff.lir.constopt;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import giraaff.lir.Variable;

///
// Maps variables to a generic type.
//
// TODO evaluate data structure
///
// @class VariableMap
final class VariableMap<T>
{
    // @field
    private final ArrayList<T> ___content;

    // @cons VariableMap
    VariableMap()
    {
        super();
        this.___content = new ArrayList<>();
    }

    public T get(Variable __key)
    {
        if (__key == null || __key.___index >= this.___content.size())
        {
            return null;
        }
        return this.___content.get(__key.___index);
    }

    public T put(Variable __key, T __value)
    {
        while (__key.___index >= this.___content.size())
        {
            this.___content.add(null);
        }
        return this.___content.set(__key.___index, __value);
    }

    public T remove(Variable __key)
    {
        if (__key.___index >= this.___content.size())
        {
            return null;
        }
        return this.___content.set(__key.___index, null);
    }

    public void forEach(Consumer<T> __action)
    {
        for (T __e : this.___content)
        {
            if (__e != null)
            {
                __action.accept(__e);
            }
        }
    }

    ///
    // Keeps only keys which match the given predicate.
    ///
    public void filter(Predicate<T> __predicate)
    {
        for (int __i = 0; __i < this.___content.size(); __i++)
        {
            T __e = this.___content.get(__i);
            if (__e != null && !__predicate.test(__e))
            {
                this.___content.set(__i, null);
            }
        }
    }
}
