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
public class ObjectState
{
    private ValueNode[] entries;
    private ValueNode materializedValue;
    private LockState locks;
    private boolean ensureVirtualized;

    private EscapeObjectState cachedState;

    /**
     * ObjectStates are duplicated lazily, if this field is true then the state needs to be copied
     * before it is modified.
     */
    boolean copyOnWrite;

    public ObjectState(ValueNode[] entries, List<MonitorIdNode> locks, boolean ensureVirtualized)
    {
        this(entries, (LockState) null, ensureVirtualized);
        for (int i = locks.size() - 1; i >= 0; i--)
        {
            this.locks = new LockState(locks.get(i), this.locks);
        }
    }

    public ObjectState(ValueNode[] entries, LockState locks, boolean ensureVirtualized)
    {
        this.entries = entries;
        this.locks = locks;
        this.ensureVirtualized = ensureVirtualized;
    }

    public ObjectState(ValueNode materializedValue, LockState locks, boolean ensureVirtualized)
    {
        this.materializedValue = materializedValue;
        this.locks = locks;
        this.ensureVirtualized = ensureVirtualized;
    }

    private ObjectState(ObjectState other)
    {
        entries = other.entries == null ? null : other.entries.clone();
        materializedValue = other.materializedValue;
        locks = other.locks;
        cachedState = other.cachedState;
        ensureVirtualized = other.ensureVirtualized;
    }

    public ObjectState cloneState()
    {
        return new ObjectState(this);
    }

    public EscapeObjectState createEscapeObjectState(VirtualObjectNode virtual)
    {
        if (cachedState == null)
        {
            if (isVirtual())
            {
                /*
                 * Clear out entries that are default values anyway.
                 *
                 * TODO: this should be propagated into ObjectState.entries, but that will take some
                 * more refactoring.
                 */
                ValueNode[] newEntries = entries.clone();
                for (int i = 0; i < newEntries.length; i++)
                {
                    if (newEntries[i].asJavaConstant() == JavaConstant.defaultForKind(virtual.entryKind(i).getStackKind()))
                    {
                        newEntries[i] = null;
                    }
                }
                cachedState = new VirtualObjectState(virtual, newEntries);
            }
            else
            {
                cachedState = new MaterializedObjectState(virtual, materializedValue);
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

    public ValueNode getEntry(int index)
    {
        return entries[index];
    }

    public ValueNode getMaterializedValue()
    {
        return materializedValue;
    }

    public void setEntry(int index, ValueNode value)
    {
        cachedState = null;
        entries[index] = value;
    }

    public void escape(ValueNode materialized)
    {
        materializedValue = materialized;
        entries = null;
        cachedState = null;
    }

    public void updateMaterializedValue(ValueNode value)
    {
        cachedState = null;
        materializedValue = value;
    }

    public void addLock(MonitorIdNode monitorId)
    {
        locks = new LockState(monitorId, locks);
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

    public boolean locksEqual(ObjectState other)
    {
        LockState a = locks;
        LockState b = other.locks;
        while (a != null && b != null && a.monitorId == b.monitorId)
        {
            a = a.next;
            b = b.next;
        }
        return a == null && b == null;
    }

    public void setEnsureVirtualized(boolean ensureVirtualized)
    {
        this.ensureVirtualized = ensureVirtualized;
    }

    public boolean getEnsureVirtualized()
    {
        return ensureVirtualized;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder().append('{');
        if (locks != null)
        {
            str.append('l').append(locks).append(' ');
        }
        if (entries != null)
        {
            for (int i = 0; i < entries.length; i++)
            {
                str.append("entry").append(i).append('=').append(entries[i]).append(' ');
            }
        }
        if (materializedValue != null)
        {
            str.append("mat=").append(materializedValue);
        }

        return str.append('}').toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(entries);
        result = prime * result + (locks != null ? locks.monitorId.getLockDepth() : 0);
        result = prime * result + ((materializedValue == null) ? 0 : materializedValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        ObjectState other = (ObjectState) obj;
        if (!Arrays.equals(entries, other.entries))
        {
            return false;
        }
        if (!locksEqual(other))
        {
            return false;
        }
        if (materializedValue == null)
        {
            if (other.materializedValue != null)
            {
                return false;
            }
        }
        else if (!materializedValue.equals(other.materializedValue))
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
