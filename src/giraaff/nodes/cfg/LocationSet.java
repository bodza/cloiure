package giraaff.nodes.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graalvm.word.LocationIdentity;

// @class LocationSet
public final class LocationSet
{
    // @field
    private LocationIdentity firstLocation;
    // @field
    private List<LocationIdentity> list;

    // @cons
    public LocationSet()
    {
        super();
        list = null;
    }

    // @cons
    public LocationSet(LocationSet __other)
    {
        super();
        this.firstLocation = __other.firstLocation;
        if (__other.list != null && __other.list.size() > 0)
        {
            list = new ArrayList<>(__other.list);
        }
    }

    private void initList()
    {
        if (list == null)
        {
            list = new ArrayList<>(4);
        }
    }

    public boolean isEmpty()
    {
        return firstLocation == null;
    }

    public boolean isAny()
    {
        return firstLocation != null && firstLocation.isAny();
    }

    public void add(LocationIdentity __location)
    {
        if (this.isAny())
        {
            return;
        }
        else if (__location.isAny())
        {
            firstLocation = __location;
            list = null;
        }
        else if (__location.isImmutable())
        {
            return;
        }
        else
        {
            if (firstLocation == null)
            {
                firstLocation = __location;
            }
            else if (__location.equals(firstLocation))
            {
                return;
            }
            else
            {
                initList();
                for (int __i = 0; __i < list.size(); ++__i)
                {
                    LocationIdentity __value = list.get(__i);
                    if (__location.equals(__value))
                    {
                        return;
                    }
                }
                list.add(__location);
            }
        }
    }

    public void addAll(LocationSet __other)
    {
        if (__other.firstLocation != null)
        {
            add(__other.firstLocation);
        }
        List<LocationIdentity> __otherList = __other.list;
        if (__otherList != null)
        {
            for (LocationIdentity __l : __otherList)
            {
                add(__l);
            }
        }
    }

    public boolean contains(LocationIdentity __locationIdentity)
    {
        if (LocationIdentity.any().equals(firstLocation))
        {
            return true;
        }
        if (__locationIdentity.equals(firstLocation))
        {
            return true;
        }
        if (list != null)
        {
            for (int __i = 0; __i < list.size(); ++__i)
            {
                LocationIdentity __value = list.get(__i);
                if (__locationIdentity.equals(__value))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
