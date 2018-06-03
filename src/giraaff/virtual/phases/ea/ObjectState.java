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

/**
 * This class describes the state of a virtual object while iterating over the graph. It describes
 * the fields or array elements (called "entries") and the lock count if the object is still
 * virtual. If the object was materialized, it contains the current materialized value.
 */
// @class ObjectState
public final class ObjectState
{
    // @field
    private ValueNode[] entries;
    // @field
    private ValueNode materializedValue;
    // @field
    private LockState locks;
    // @field
    private boolean ensureVirtualized;

    // @field
    private EscapeObjectState cachedState;

    /**
     * ObjectStates are duplicated lazily, if this field is true then the state needs to be copied
     * before it is modified.
     */
    // @field
    boolean copyOnWrite;

    // @cons
    public ObjectState(ValueNode[] __entries, List<MonitorIdNode> __locks, boolean __ensureVirtualized)
    {
        this(__entries, (LockState) null, __ensureVirtualized);
        for (int __i = __locks.size() - 1; __i >= 0; __i--)
        {
            this.locks = new LockState(__locks.get(__i), this.locks);
        }
    }

    // @cons
    public ObjectState(ValueNode[] __entries, LockState __locks, boolean __ensureVirtualized)
    {
        super();
        this.entries = __entries;
        this.locks = __locks;
        this.ensureVirtualized = __ensureVirtualized;
    }

    // @cons
    public ObjectState(ValueNode __materializedValue, LockState __locks, boolean __ensureVirtualized)
    {
        super();
        this.materializedValue = __materializedValue;
        this.locks = __locks;
        this.ensureVirtualized = __ensureVirtualized;
    }

    // @cons
    private ObjectState(ObjectState __other)
    {
        super();
        entries = __other.entries == null ? null : __other.entries.clone();
        materializedValue = __other.materializedValue;
        locks = __other.locks;
        cachedState = __other.cachedState;
        ensureVirtualized = __other.ensureVirtualized;
    }

    public ObjectState cloneState()
    {
        return new ObjectState(this);
    }

    public EscapeObjectState createEscapeObjectState(VirtualObjectNode __virtual)
    {
        if (cachedState == null)
        {
            if (isVirtual())
            {
                /*
                 * Clear out entries that are default values anyway.
                 *
                 * TODO this should be propagated into ObjectState.entries, but that will take some more refactoring
                 */
                ValueNode[] __newEntries = entries.clone();
                for (int __i = 0; __i < __newEntries.length; __i++)
                {
                    if (__newEntries[__i].asJavaConstant() == JavaConstant.defaultForKind(__virtual.entryKind(__i).getStackKind()))
                    {
                        __newEntries[__i] = null;
                    }
                }
                cachedState = new VirtualObjectState(__virtual, __newEntries);
            }
            else
            {
                cachedState = new MaterializedObjectState(__virtual, materializedValue);
            }
        }
        return cachedState;
    }

    public boolean isVirtual()
    {
        return materializedValue == null;
    }

    /**
     * Users of this method are not allowed to change the entries of the returned array.
     */
    public ValueNode[] getEntries()
    {
        return entries;
    }

    public ValueNode getEntry(int __index)
    {
        return entries[__index];
    }

    public ValueNode getMaterializedValue()
    {
        return materializedValue;
    }

    public void setEntry(int __index, ValueNode __value)
    {
        cachedState = null;
        entries[__index] = __value;
    }

    public void escape(ValueNode __materialized)
    {
        materializedValue = __materialized;
        entries = null;
        cachedState = null;
    }

    public void updateMaterializedValue(ValueNode __value)
    {
        cachedState = null;
        materializedValue = __value;
    }

    public void addLock(MonitorIdNode __monitorId)
    {
        locks = new LockState(__monitorId, locks);
    }

    public MonitorIdNode removeLock()
    {
        try
        {
            return locks.monitorId;
        }
        finally
        {
            locks = locks.next;
        }
    }

    public LockState getLocks()
    {
        return locks;
    }

    public boolean hasLocks()
    {
        return locks != null;
    }

    public boolean locksEqual(ObjectState __other)
    {
        LockState __a = locks;
        LockState __b = __other.locks;
        while (__a != null && __b != null && __a.monitorId == __b.monitorId)
        {
            __a = __a.next;
            __b = __b.next;
        }
        return __a == null && __b == null;
    }

    public void setEnsureVirtualized(boolean __ensureVirtualized)
    {
        this.ensureVirtualized = __ensureVirtualized;
    }

    public boolean getEnsureVirtualized()
    {
        return ensureVirtualized;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + Arrays.hashCode(entries);
        __result = __prime * __result + (locks != null ? locks.monitorId.getLockDepth() : 0);
        __result = __prime * __result + ((materializedValue == null) ? 0 : materializedValue.hashCode());
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
        if (!Arrays.equals(entries, __other.entries))
        {
            return false;
        }
        if (!locksEqual(__other))
        {
            return false;
        }
        if (materializedValue == null)
        {
            if (__other.materializedValue != null)
            {
                return false;
            }
        }
        else if (!materializedValue.equals(__other.materializedValue))
        {
            return false;
        }
        return true;
    }

    public ObjectState share()
    {
        copyOnWrite = true;
        return this;
    }
}
