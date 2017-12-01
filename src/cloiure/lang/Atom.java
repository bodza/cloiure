package cloiure.lang;

import java.util.concurrent.atomic.AtomicReference;

final public class Atom extends ARef implements IAtom2
{
    final AtomicReference state;

    public Atom(Object state)
    {
        this.state = new AtomicReference(state);
    }

    public Atom(Object state, IPersistentMap meta)
    {
        super(meta);
        this.state = new AtomicReference(state);
    }

    public Object deref()
    {
        return state.get();
    }

    public Object swap(IFn f)
    {
        while (true)
        {
            Object v = deref();
            Object newv = f.invoke(v);
            validate(newv);
            if (state.compareAndSet(v, newv))
            {
                notifyWatches(v, newv);
                return newv;
            }
        }
    }

    public Object swap(IFn f, Object arg)
    {
        while (true)
        {
            Object v = deref();
            Object newv = f.invoke(v, arg);
            validate(newv);
            if (state.compareAndSet(v, newv))
            {
                notifyWatches(v, newv);
                return newv;
            }
        }
    }

    public Object swap(IFn f, Object arg1, Object arg2)
    {
        while (true)
        {
            Object v = deref();
            Object newv = f.invoke(v, arg1, arg2);
            validate(newv);
            if (state.compareAndSet(v, newv))
            {
                notifyWatches(v, newv);
                return newv;
            }
        }
    }

    public Object swap(IFn f, Object x, Object y, ISeq args)
    {
        while (true)
        {
            Object v = deref();
            Object newv = f.applyTo(RT.listStar(v, x, y, args));
            validate(newv);
            if (state.compareAndSet(v, newv))
            {
                notifyWatches(v, newv);
                return newv;
            }
        }
    }

    public IPersistentVector swapVals(IFn f)
    {
        while (true)
        {
            Object oldv = deref();
            Object newv = f.invoke(oldv);
            validate(newv);
            if (state.compareAndSet(oldv, newv))
            {
                notifyWatches(oldv, newv);
                return LazilyPersistentVector.createOwning(oldv, newv);
            }
        }
    }

    public IPersistentVector swapVals(IFn f, Object arg)
    {
        while (true)
        {
            Object oldv = deref();
            Object newv = f.invoke(oldv, arg);
            validate(newv);
            if (state.compareAndSet(oldv, newv))
            {
                notifyWatches(oldv, newv);
                return LazilyPersistentVector.createOwning(oldv, newv);
            }
        }
    }

    public IPersistentVector swapVals(IFn f, Object arg1, Object arg2)
    {
        while (true)
        {
            Object oldv = deref();
            Object newv = f.invoke(oldv, arg1, arg2);
            validate(newv);
            if (state.compareAndSet(oldv, newv))
            {
                notifyWatches(oldv, newv);
                return LazilyPersistentVector.createOwning(oldv, newv);
            }
        }
    }

    public IPersistentVector swapVals(IFn f, Object x, Object y, ISeq args)
    {
        while (true)
        {
            Object oldv = deref();
            Object newv = f.applyTo(RT.listStar(oldv, x, y, args));
            validate(newv);
            if (state.compareAndSet(oldv, newv))
            {
                notifyWatches(oldv, newv);
                return LazilyPersistentVector.createOwning(oldv, newv);
            }
        }
    }

    public boolean compareAndSet(Object oldv, Object newv)
    {
        validate(newv);
        boolean ret = state.compareAndSet(oldv, newv);
        if (ret)
        {
            notifyWatches(oldv, newv);
        }
        return ret;
    }

    public Object reset(Object newval)
    {
        Object oldval = state.get();
        validate(newval);
        state.set(newval);
        notifyWatches(oldval, newval);
        return newval;
    }

    public IPersistentVector resetVals(Object newv)
    {
        validate(newv);
        while (true)
        {
            Object oldv = deref();
            if (state.compareAndSet(oldv, newv))
            {
                notifyWatches(oldv, newv);
                return LazilyPersistentVector.createOwning(oldv, newv);
            }
        }
    }
}
