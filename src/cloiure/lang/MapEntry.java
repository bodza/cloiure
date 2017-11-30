package cloiure.lang;

import java.util.Iterator;

public class MapEntry extends AMapEntry
{
    final Object _key;
    final Object _val;

    static public MapEntry create(Object key, Object val)
    {
        return new MapEntry(key, val);
    }

    public MapEntry(Object key, Object val)
    {
        this._key = key;
        this._val = val;
    }

    public Object key()
    {
        return _key;
    }

    public Object val()
    {
        return _val;
    }

    public Object getKey()
    {
        return key();
    }

    public Object getValue()
    {
        return val();
    }
}
