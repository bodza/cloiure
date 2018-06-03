package giraaff.virtual.phases.ea;

import java.util.Arrays;
import java.util.List;

import jdk.vm.ci.meta.JavaConstant;

import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.nodes.virtual.LockState;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.virtual.nodes.MaterializedObjectState;
import giraaff.virtual.nodes.VirtualObjectState;

///
// This class describes the state of a virtual object while iterating over the graph. It describes
// the fields or array elements (called "entries") and the lock count if the object is still
// virtual. If the object was materialized, it contains the current materialized value.
///
// @class ObjectState
public final class ObjectState
{
    // @field
    private ValueNode[] ___entries;
    // @field
    private ValueNode ___materializedValue;
    // @field
    private LockState ___locks;
    // @field
    private boolean ___ensureVirtualized;

    // @field
    private EscapeObjectState ___cachedState;

    ///
    // ObjectStates are duplicated lazily, if this field is true then the state needs to be copied
    // before it is modified.
    ///
    // @field
    boolean ___copyOnWrite;

    // @cons
    public ObjectState(ValueNode[] __entries, List<MonitorIdNode> __locks, boolean __ensureVirtualized)
    {
        this(__entries, (LockState) null, __ensureVirtualized);
        for (int __i = __locks.size() - 1; __i >= 0; __i--)
        {
            this.___locks = new LockState(__locks.get(__i), this.___locks);
        }
    }

    // @cons
    public ObjectState(ValueNode[] __entries, LockState __locks, boolean __ensureVirtualized)
    {
        super();
        this.___entries = __entries;
        this.___locks = __locks;
        this.___ensureVirtualized = __ensureVirtualized;
    }

    // @cons
    public ObjectState(ValueNode __materializedValue, LockState __locks, boolean __ensureVirtualized)
    {
        super();
        this.___materializedValue = __materializedValue;
        this.___locks = __locks;
        this.___ensureVirtualized = __ensureVirtualized;
    }

    // @cons
    private ObjectState(ObjectState __other)
    {
        super();
        this.___entries = __other.___entries == null ? null : __other.___entries.clone();
        this.___materializedValue = __other.___materializedValue;
        this.___locks = __other.___locks;
        this.___cachedState = __other.___cachedState;
        this.___ensureVirtualized = __other.___ensureVirtualized;
    }

    public ObjectState cloneState()
    {
        return new ObjectState(this);
    }

    public EscapeObjectState createEscapeObjectState(VirtualObjectNode __virtual)
    {
        if (this.___cachedState == null)
        {
            if (isVirtual())
            {
                // Clear out entries that are default values anyway.
                //
                // TODO this should be propagated into ObjectState.entries, but that will take some more refactoring
                ValueNode[] __newEntries = this.___entries.clone();
                for (int __i = 0; __i < __newEntries.length; __i++)
                {
                    if (__newEntries[__i].asJavaConstant() == JavaConstant.defaultForKind(__virtual.entryKind(__i).getStackKind()))
                    {
                        __newEntries[__i] = null;
                    }
                }
                this.___cachedState = new VirtualObjectState(__virtual, __newEntries);
            }
            else
            {
                this.___cachedState = new MaterializedObjectState(__virtual, this.___materializedValue);
            }
        }
        return this.___cachedState;
    }

    public boolean isVirtual()
    {
        return this.___materializedValue == null;
    }

    ///
    // Users of this method are not allowed to change the entries of the returned array.
    ///
    public ValueNode[] getEntries()
    {
        return this.___entries;
    }

    public ValueNode getEntry(int __index)
    {
        return this.___entries[__index];
    }

    public ValueNode getMaterializedValue()
    {
        return this.___materializedValue;
    }

    public void setEntry(int __index, ValueNode __value)
    {
        this.___cachedState = null;
        this.___entries[__index] = __value;
    }

    public void escape(ValueNode __materialized)
    {
        this.___materializedValue = __materialized;
        this.___entries = null;
        this.___cachedState = null;
    }

    public void updateMaterializedValue(ValueNode __value)
    {
        this.___cachedState = null;
        this.___materializedValue = __value;
    }

    public void addLock(MonitorIdNode __monitorId)
    {
        this.___locks = new LockState(__monitorId, this.___locks);
    }

    public MonitorIdNode removeLock()
    {
        try
        {
            return this.___locks.___monitorId;
        }
        finally
        {
            this.___locks = this.___locks.___next;
        }
    }

    public LockState getLocks()
    {
        return this.___locks;
    }

    public boolean hasLocks()
    {
        return this.___locks != null;
    }

    public boolean locksEqual(ObjectState __other)
    {
        LockState __a = this.___locks;
        LockState __b = __other.___locks;
        while (__a != null && __b != null && __a.___monitorId == __b.___monitorId)
        {
            __a = __a.___next;
            __b = __b.___next;
        }
        return __a == null && __b == null;
    }

    public void setEnsureVirtualized(boolean __ensureVirtualized)
    {
        this.___ensureVirtualized = __ensureVirtualized;
    }

    public boolean getEnsureVirtualized()
    {
        return this.___ensureVirtualized;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + Arrays.hashCode(this.___entries);
        __result = __prime * __result + (this.___locks != null ? this.___locks.___monitorId.getLockDepth() : 0);
        __result = __prime * __result + ((this.___materializedValue == null) ? 0 : this.___materializedValue.hashCode());
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null || getClass() != __obj.getClass())
        {
            return false;
        }
        ObjectState __other = (ObjectState) __obj;
        if (!Arrays.equals(this.___entries, __other.___entries))
        {
            return false;
        }
        if (!locksEqual(__other))
        {
            return false;
        }
        if (this.___materializedValue == null)
        {
            if (__other.___materializedValue != null)
            {
                return false;
            }
        }
        else if (!this.___materializedValue.equals(__other.___materializedValue))
        {
            return false;
        }
        return true;
    }

    public ObjectState share()
    {
        this.___copyOnWrite = true;
        return this;
    }
}
