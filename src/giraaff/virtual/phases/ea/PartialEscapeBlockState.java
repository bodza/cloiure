package giraaff.virtual.phases.ea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import giraaff.graph.Node;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.CommitAllocationNode;
import giraaff.nodes.virtual.LockState;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.virtual.phases.ea.EffectList.Effect;

// @class PartialEscapeBlockState
public abstract class PartialEscapeBlockState<T extends PartialEscapeBlockState<T>> extends EffectsBlockState<T>
{
    // @def
    private static final ObjectState[] EMPTY_ARRAY = new ObjectState[0];

    /**
     * This array contains the state of all virtual objects, indexed by
     * {@link VirtualObjectNode#getObjectId()}. Entries in this array may be null if the
     * corresponding virtual object is not alive or reachable currently.
     */
    // @field
    private ObjectState[] objectStates;

    public boolean contains(VirtualObjectNode __value)
    {
        for (ObjectState __state : objectStates)
        {
            if (__state != null && __state.isVirtual() && __state.getEntries() != null)
            {
                for (ValueNode __entry : __state.getEntries())
                {
                    if (__entry == __value)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // @class PartialEscapeBlockState.RefCount
    private static final class RefCount
    {
        // @field
        private int refCount = 1;
    }

    /**
     * Usage count for the objectStates array, to avoid unneessary copying.
     */
    // @field
    private RefCount arrayRefCount;

    /**
     * Final subclass of PartialEscapeBlockState, for performance and to make everything behave
     * nicely with generics.
     */
    // @class PartialEscapeBlockState.Final
    public static final class Final extends PartialEscapeBlockState<Final>
    {
        // @cons
        public Final()
        {
            super();
        }

        // @cons
        public Final(Final __other)
        {
            super(__other);
        }
    }

    // @cons
    protected PartialEscapeBlockState()
    {
        super();
        objectStates = EMPTY_ARRAY;
        arrayRefCount = new RefCount();
    }

    // @cons
    protected PartialEscapeBlockState(PartialEscapeBlockState<T> __other)
    {
        super(__other);
        adoptAddObjectStates(__other);
    }

    public ObjectState getObjectState(int __object)
    {
        return objectStates[__object];
    }

    public ObjectState getObjectStateOptional(int __object)
    {
        return __object >= objectStates.length ? null : objectStates[__object];
    }

    /**
     * Asserts that the given virtual object is available/reachable in the current state.
     */
    public ObjectState getObjectState(VirtualObjectNode __object)
    {
        return objectStates[__object.getObjectId()];
    }

    public ObjectState getObjectStateOptional(VirtualObjectNode __object)
    {
        int __id = __object.getObjectId();
        return __id >= objectStates.length ? null : objectStates[__id];
    }

    private ObjectState[] getObjectStateArrayForModification()
    {
        if (arrayRefCount.refCount > 1)
        {
            objectStates = objectStates.clone();
            arrayRefCount.refCount--;
            arrayRefCount = new RefCount();
        }
        return objectStates;
    }

    private ObjectState getObjectStateForModification(int __object)
    {
        ObjectState[] __array = getObjectStateArrayForModification();
        ObjectState __objectState = __array[__object];
        if (__objectState.copyOnWrite)
        {
            __array[__object] = __objectState = __objectState.cloneState();
        }
        return __objectState;
    }

    public void setEntry(int __object, int __entryIndex, ValueNode __value)
    {
        if (objectStates[__object].getEntry(__entryIndex) != __value)
        {
            getObjectStateForModification(__object).setEntry(__entryIndex, __value);
        }
    }

    public void escape(int __object, ValueNode __materialized)
    {
        getObjectStateForModification(__object).escape(__materialized);
    }

    public void addLock(int __object, MonitorIdNode __monitorId)
    {
        getObjectStateForModification(__object).addLock(__monitorId);
    }

    public MonitorIdNode removeLock(int __object)
    {
        return getObjectStateForModification(__object).removeLock();
    }

    public void setEnsureVirtualized(int __object, boolean __ensureVirtualized)
    {
        if (objectStates[__object].getEnsureVirtualized() != __ensureVirtualized)
        {
            getObjectStateForModification(__object).setEnsureVirtualized(__ensureVirtualized);
        }
    }

    public void updateMaterializedValue(int __object, ValueNode __value)
    {
        if (objectStates[__object].getMaterializedValue() != __value)
        {
            getObjectStateForModification(__object).updateMaterializedValue(__value);
        }
    }

    /**
     * Materializes the given virtual object and produces the necessary effects in the effects list.
     * This transitively also materializes all other virtual objects that are reachable from the entries.
     */
    public void materializeBefore(FixedNode __fixed, VirtualObjectNode __virtual, GraphEffectList __materializeEffects)
    {
        List<AllocatedObjectNode> __objects = new ArrayList<>(2);
        List<ValueNode> __values = new ArrayList<>(8);
        List<List<MonitorIdNode>> __locks = new ArrayList<>();
        List<ValueNode> __otherAllocations = new ArrayList<>(2);
        List<Boolean> __ensureVirtual = new ArrayList<>(2);
        materializeWithCommit(__fixed, __virtual, __objects, __locks, __values, __ensureVirtual, __otherAllocations);

        __materializeEffects.addVirtualizationDelta(-(__objects.size() + __otherAllocations.size()));
        // @closure
        __materializeEffects.add("materializeBefore", new Effect()
        {
            @Override
            public void apply(StructuredGraph __graph, ArrayList<Node> __obsoleteNodes)
            {
                for (ValueNode __alloc : __otherAllocations)
                {
                    ValueNode __otherAllocation = __graph.addOrUniqueWithInputs(__alloc);
                    if (__otherAllocation instanceof FixedWithNextNode)
                    {
                        __graph.addBeforeFixed(__fixed, (FixedWithNextNode) __otherAllocation);
                    }
                }
                if (!__objects.isEmpty())
                {
                    CommitAllocationNode __commit;
                    if (__fixed.predecessor() instanceof CommitAllocationNode)
                    {
                        __commit = (CommitAllocationNode) __fixed.predecessor();
                    }
                    else
                    {
                        __commit = __graph.add(new CommitAllocationNode());
                        __graph.addBeforeFixed(__fixed, __commit);
                    }
                    for (AllocatedObjectNode __obj : __objects)
                    {
                        __graph.addWithoutUnique(__obj);
                        __commit.getVirtualObjects().add(__obj.getVirtualObject());
                        __obj.setCommit(__commit);
                    }
                    for (ValueNode __value : __values)
                    {
                        __commit.getValues().add(__graph.addOrUniqueWithInputs(__value));
                    }
                    for (List<MonitorIdNode> __monitorIds : __locks)
                    {
                        __commit.addLocks(__monitorIds);
                    }
                    __commit.getEnsureVirtual().addAll(__ensureVirtual);

                    List<AllocatedObjectNode> __materializedValues = __commit.usages().filter(AllocatedObjectNode.class).snapshot();
                    for (int __i = 0; __i < __commit.getValues().size(); __i++)
                    {
                        if (__materializedValues.contains(__commit.getValues().get(__i)))
                        {
                            __commit.getValues().set(__i, ((AllocatedObjectNode) __commit.getValues().get(__i)).getVirtualObject());
                        }
                    }
                }
            }
        });
    }

    private void materializeWithCommit(FixedNode __fixed, VirtualObjectNode __virtual, List<AllocatedObjectNode> __objects, List<List<MonitorIdNode>> __locks, List<ValueNode> __values, List<Boolean> __ensureVirtual, List<ValueNode> __otherAllocations)
    {
        ObjectState __obj = getObjectState(__virtual);

        ValueNode[] __entries = __obj.getEntries();
        ValueNode __representation = __virtual.getMaterializedRepresentation(__fixed, __entries, __obj.getLocks());
        escape(__virtual.getObjectId(), __representation);
        __obj = getObjectState(__virtual);
        PartialEscapeClosure.updateStatesForMaterialized(this, __virtual, __obj.getMaterializedValue());
        if (__representation instanceof AllocatedObjectNode)
        {
            __objects.add((AllocatedObjectNode) __representation);
            __locks.add(LockState.asList(__obj.getLocks()));
            __ensureVirtual.add(__obj.getEnsureVirtualized());
            int __pos = __values.size();
            while (__values.size() < __pos + __entries.length)
            {
                __values.add(null);
            }
            for (int __i = 0; __i < __entries.length; __i++)
            {
                if (__entries[__i] instanceof VirtualObjectNode)
                {
                    VirtualObjectNode __entryVirtual = (VirtualObjectNode) __entries[__i];
                    ObjectState __entryObj = getObjectState(__entryVirtual);
                    if (__entryObj.isVirtual())
                    {
                        materializeWithCommit(__fixed, __entryVirtual, __objects, __locks, __values, __ensureVirtual, __otherAllocations);
                        __entryObj = getObjectState(__entryVirtual);
                    }
                    __values.set(__pos + __i, __entryObj.getMaterializedValue());
                }
                else
                {
                    __values.set(__pos + __i, __entries[__i]);
                }
            }
            objectMaterialized(__virtual, (AllocatedObjectNode) __representation, __values.subList(__pos, __pos + __entries.length));
        }
        else
        {
            __otherAllocations.add(__representation);
        }
    }

    protected void objectMaterialized(VirtualObjectNode __virtual, AllocatedObjectNode __representation, List<ValueNode> __values)
    {
    }

    public void addObject(int __virtual, ObjectState __state)
    {
        ensureSize(__virtual)[__virtual] = __state;
    }

    private ObjectState[] ensureSize(int __objectId)
    {
        if (objectStates.length <= __objectId)
        {
            objectStates = Arrays.copyOf(objectStates, Math.max(__objectId * 2, 4));
            arrayRefCount.refCount--;
            arrayRefCount = new RefCount();
            return objectStates;
        }
        else
        {
            return getObjectStateArrayForModification();
        }
    }

    public int getStateCount()
    {
        return objectStates.length;
    }

    @Override
    public boolean equivalentTo(T __other)
    {
        int __length = Math.max(objectStates.length, __other.getStateCount());
        for (int __i = 0; __i < __length; __i++)
        {
            ObjectState __left = getObjectStateOptional(__i);
            ObjectState __right = __other.getObjectStateOptional(__i);
            if (__left != __right)
            {
                if (__left == null || __right == null)
                {
                    return false;
                }
                if (!__left.equals(__right))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public void resetObjectStates(int __size)
    {
        objectStates = new ObjectState[__size];
    }

    public static boolean identicalObjectStates(PartialEscapeBlockState<?>[] __states)
    {
        for (int __i = 1; __i < __states.length; __i++)
        {
            if (__states[0].objectStates != __states[__i].objectStates)
            {
                return false;
            }
        }
        return true;
    }

    public static boolean identicalObjectStates(PartialEscapeBlockState<?>[] __states, int __object)
    {
        for (int __i = 1; __i < __states.length; __i++)
        {
            if (__states[0].objectStates[__object] != __states[__i].objectStates[__object])
            {
                return false;
            }
        }
        return true;
    }

    public void adoptAddObjectStates(PartialEscapeBlockState<?> __other)
    {
        if (objectStates != null)
        {
            arrayRefCount.refCount--;
        }
        objectStates = __other.objectStates;
        arrayRefCount = __other.arrayRefCount;

        if (arrayRefCount.refCount == 1)
        {
            for (ObjectState __state : objectStates)
            {
                if (__state != null)
                {
                    __state.share();
                }
            }
        }
        arrayRefCount.refCount++;
    }
}
