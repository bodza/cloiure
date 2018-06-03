package giraaff.nodes.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graalvm.word.LocationIdentity;

// @class LocationSet
public final class LocationSet
{
    // @field
    private LocationIdentity ___firstLocation;
    // @field
    private List<LocationIdentity> ___list;

    // @cons
    public LocationSet()
    {
        super();
        this.___list = null;
    }

    // @cons
    public LocationSet(LocationSet __other)
    {
        super();
        this.___firstLocation = __other.___firstLocation;
        if (__other.___list != null && __other.___list.size() > 0)
        {
            this.___list = new ArrayList<>(__other.___list);
        }
    }

    private void initList()
    {
        if (this.___list == null)
        {
            this.___list = new ArrayList<>(4);
        }
    }

    public boolean isEmpty()
    {
        return this.___firstLocation == null;
    }

    public boolean isAny()
    {
        return this.___firstLocation != null && this.___firstLocation.isAny();
    }

    public void add(LocationIdentity __location)
    {
        if (this.isAny())
        {
            return;
        }
        else if (__location.isAny())
        {
            this.___firstLocation = __location;
            this.___list = null;
        }
        else if (__location.isImmutable())
        {
            return;
        }
        else
        {
            if (this.___firstLocation == null)
            {
                this.___firstLocation = __location;
            }
            else if (__location.equals(this.___firstLocation))
            {
                return;
            }
            else
            {
                initList();
                for (int __i = 0; __i < this.___list.size(); ++__i)
                {
                    LocationIdentity __value = this.___list.get(__i);
                    if (__location.equals(__value))
                    {
                        return;
                    }
                }
                this.___list.add(__location);
            }
        }
    }

    public void addAll(LocationSet __other)
    {
        if (__other.___firstLocation != null)
        {
            add(__other.___firstLocation);
        }
        List<LocationIdentity> __otherList = __other.___list;
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
        if (LocationIdentity.any().equals(this.___firstLocation))
        {
            return true;
        }
        if (__locationIdentity.equals(this.___firstLocation))
        {
            return true;
        }
        if (this.___list != null)
        {
            for (int __i = 0; __i < this.___list.size(); ++__i)
            {
                LocationIdentity __value = this.___list.get(__i);
                if (__locationIdentity.equals(__value))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
