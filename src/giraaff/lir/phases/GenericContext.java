package giraaff.lir.phases;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Allows storing of arbitrary data.
 */
// @class GenericContext
public class GenericContext
{
    // @field
    private ArrayList<Object> context;

    // @cons
    public GenericContext()
    {
        super();
        context = null;
    }

    public <T> void contextAdd(T __obj)
    {
        if (context == null)
        {
            context = new ArrayList<>();
        }
        context.add(__obj);
    }

    @SuppressWarnings("unchecked")
    public <T> T contextLookup(Class<T> __clazz)
    {
        if (context != null)
        {
            for (Object __e : context)
            {
                if (__clazz.isInstance(__e))
                {
                    return (T) __e;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T contextRemove(Class<T> __clazz)
    {
        if (context != null)
        {
            ListIterator<Object> __it = context.listIterator();
            while (__it.hasNext())
            {
                Object __e = __it.next();
                if (__clazz.isInstance(__e))
                {
                    // remove entry
                    __it.remove();
                    if (context.isEmpty())
                    {
                        context = null;
                    }
                    return (T) __e;
                }
            }
        }
        return null;
    }
}
