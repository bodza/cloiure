package cloiure.lang;

public class Binding<T>
{
    public T val;
    public final Binding rest;

    public Binding(T val)
    {
        this.val = val;
        this.rest = null;
    }

    public Binding(T val, Binding rest)
    {
        this.val = val;
        this.rest = rest;
    }
}
