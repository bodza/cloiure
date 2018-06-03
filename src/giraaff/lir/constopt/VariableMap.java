package giraaff.lir.constopt;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import giraaff.lir.Variable;

/**
 * Maps variables to a generic type.
 *
 * TODO evaluate data structure
 */
// @class VariableMap
final class VariableMap<T>
{
    // @field
    private final ArrayList<T> content;

    // @cons
    VariableMap()
    {
        super();
        content = new ArrayList<>();
    }

    public T get(Variable __key)
    {
        if (__key == null || __key.index >= content.size())
        {
            return null;
        }
        return content.get(__key.index);
    }

    public T put(Variable __key, T __value)
    {
        while (__key.index >= content.size())
        {
            content.add(null);
        }
        return content.set(__key.index, __value);
    }

    public T remove(Variable __key)
    {
        if (__key.index >= content.size())
        {
            return null;
        }
        return content.set(__key.index, null);
    }

    public void forEach(Consumer<T> __action)
    {
        for (T __e : content)
        {
            if (__e != null)
            {
                __action.accept(__e);
            }
        }
    }

    /**
     * Keeps only keys which match the given predicate.
     */
    public void filter(Predicate<T> __predicate)
    {
        for (int __i = 0; __i < content.size(); __i++)
        {
            T __e = content.get(__i);
            if (__e != null && !__predicate.test(__e))
            {
                content.set(__i, null);
            }
        }
    }
}
