package giraaff.lir.phases;

import java.util.ArrayList;
import java.util.ListIterator;

///
// Allows storing of arbitrary data.
///
// @class GenericContext
public class GenericContext
{
    // @field
    private ArrayList<Object> ___context;

    // @cons GenericContext
    public GenericContext()
    {
        super();
        this.___context = null;
    }

    public <T> void contextAdd(T __obj)
    {
        if (this.___context == null)
        {
            this.___context = new ArrayList<>();
        }
        this.___context.add(__obj);
    }

    @SuppressWarnings("unchecked")
    public <T> T contextLookup(Class<T> __clazz)
    {
        if (this.___context != null)
        {
            for (Object __e : this.___context)
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
        if (this.___context != null)
        {
            ListIterator<Object> __it = this.___context.listIterator();
            while (__it.hasNext())
            {
                Object __e = __it.next();
                if (__clazz.isInstance(__e))
                {
                    // remove entry
                    __it.remove();
                    if (this.___context.isEmpty())
                    {
                        this.___context = null;
                    }
                    return (T) __e;
                }
            }
        }
        return null;
    }
}
