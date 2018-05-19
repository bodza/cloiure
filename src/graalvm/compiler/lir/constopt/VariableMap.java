package graalvm.compiler.lir.constopt;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import graalvm.compiler.lir.Variable;

/**
 * Maps variables to a generic type.
 *
 * TODO (je) evaluate data structure
 */
class VariableMap<T>
{
    private final ArrayList<T> content;

    VariableMap()
    {
        content = new ArrayList<>();
    }

    public T get(Variable key)
    {
        if (key == null || key.index >= content.size())
        {
            return null;
        }
        return content.get(key.index);
    }

    public T put(Variable key, T value)
    {
        while (key.index >= content.size())
        {
            content.add(null);
        }
        return content.set(key.index, value);
    }

    public T remove(Variable key)
    {
        if (key.index >= content.size())
        {
            return null;
        }
        return content.set(key.index, null);
    }

    public void forEach(Consumer<T> action)
    {
        for (T e : content)
        {
            if (e != null)
            {
                action.accept(e);
            }
        }
    }

    /**
     * Keeps only keys which match the given predicate.
     */
    public void filter(Predicate<T> predicate)
    {
        for (int i = 0; i < content.size(); i++)
        {
            T e = content.get(i);
            if (e != null && !predicate.test(e))
            {
                content.set(i, null);
            }
        }
    }
}
