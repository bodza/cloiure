package cloiure.lang;

public class FnLoaderThunk extends RestFn
{
    final Var v;
    final ClassLoader loader;
    final String fnClassName;
    IFn fn;

    public FnLoaderThunk(Var v, String fnClassName)
    {
        this.v = v;
        this.loader = (ClassLoader) RT.FN_LOADER_VAR.get();
        this.fnClassName = fnClassName;
        fn = null;
    }

    public Object invoke(Object arg1)
    {
        load();
        return fn.invoke(arg1);
    }

    public Object invoke(Object arg1, Object arg2)
    {
        load();
        return fn.invoke(arg1, arg2);
    }

    public Object invoke(Object arg1, Object arg2, Object arg3)
    {
        load();
        return fn.invoke(arg1, arg2, arg3);
    }

    protected Object doInvoke(Object args)
    {
        load();
        return fn.applyTo((ISeq) args);
    }

    private void load()
    {
        if (fn == null)
        {
            try
            {
                fn = (IFn) Class.forName(fnClassName, true, loader).newInstance();
            }
            catch (Exception e)
            {
                throw Util.sneakyThrow(e);
            }
            v.root = fn;
        }
    }

    public int getRequiredArity()
    {
        return 0;
    }

    public IObj withMeta(IPersistentMap meta)
    {
        return this;
    }

    public IPersistentMap meta()
    {
        return null;
    }
}
