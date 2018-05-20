package graalvm.compiler.virtual.phases.ea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.virtual.AllocatedObjectNode;
import graalvm.compiler.nodes.virtual.CommitAllocationNode;
import graalvm.compiler.nodes.virtual.LockState;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.virtual.phases.ea.EffectList.Effect;

public abstract class PartialEscapeBlockState<T extends PartialEscapeBlockState<T>> extends EffectsBlockState<T>
{
    private static final ObjectState[] EMPTY_ARRAY = new ObjectState[0];

    /**
     * This array contains the state of all virtual objects, indexed by
     * {@link VirtualObjectNode#getObjectId()}. Entries in this array may be null if the
     * corresponding virtual object is not alive or reachable currently.
     */
    private ObjectState[] objectStates;

    public boolean contains(VirtualObjectNode value)
    {
        for (ObjectState state : objectStates)
        {
            if (state != null && state.isVirtual() && state.getEntries() != null)
            {
                for (ValueNode entry : state.getEntries())
                {
                    if (entry == value)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static class RefCount
    {
        private int refCount = 1;
    }

    /**
     * Usage count for the objectStates array, to avoid unneessary copying.
     */
    private RefCount arrayRefCount;

    private final OptionValues options;

    /**
     * Final subclass of PartialEscapeBlockState, for performance and to make everything behave
     * nicely with generics.
     */
    public static final class Final extends PartialEscapeBlockState<Final>
    {
        public Final(OptionValues options)
        {
            super(options);
        }

        public Final(Final other)
        {
            super(other);
        }
    }

    protected PartialEscapeBlockState(OptionValues options)
    {
        objectStates = EMPTY_ARRAY;
        arrayRefCount = new RefCount();
        this.options = options;
    }

    protected PartialEscapeBlockState(PartialEscapeBlockState<T> other)
    {
        super(other);
        adoptAddObjectStates(other);
        options = other.options;
    }

    public ObjectState getObjectState(int object)
    {
        ObjectState state = objectStates[object];
        return state;
    }

    public ObjectState getObjectStateOptional(int object)
    {
        return object >= objectStates.length ? null : objectStates[object];
    }

    /**
     * Asserts that the given virtual object is available/reachable in the current state.
     */
    public ObjectState getObjectState(VirtualObjectNode object)
    {
        ObjectState state = objectStates[object.getObjectId()];
        return state;
    }

    public ObjectState getObjectStateOptional(VirtualObjectNode object)
    {
        int id = object.getObjectId();
        return id >= objectStates.length ? null : objectStates[id];
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

    private ObjectState getObjectStateForModification(int object)
    {
        ObjectState[] array = getObjectStateArrayForModification();
        ObjectState objectState = array[object];
        if (objectState.copyOnWrite)
        {
            array[object] = objectState = objectState.cloneState();
        }
        return objectState;
    }

    public void setEntry(int object, int entryIndex, ValueNode value)
    {
        if (objectStates[object].getEntry(entryIndex) != value)
        {
            getObjectStateForModification(object).setEntry(entryIndex, value);
        }
    }

    public void escape(int object, ValueNode materialized)
    {
        getObjectStateForModification(object).escape(materialized);
    }

    public void addLock(int object, MonitorIdNode monitorId)
    {
        getObjectStateForModification(object).addLock(monitorId);
    }

    public MonitorIdNode removeLock(int object)
    {
        return getObjectStateForModification(object).removeLock();
    }

    public void setEnsureVirtualized(int object, boolean ensureVirtualized)
    {
        if (objectStates[object].getEnsureVirtualized() != ensureVirtualized)
        {
            getObjectStateForModification(object).setEnsureVirtualized(ensureVirtualized);
        }
    }

    public void updateMaterializedValue(int object, ValueNode value)
    {
        if (objectStates[object].getMaterializedValue() != value)
        {
            getObjectStateForModification(object).updateMaterializedValue(value);
        }
    }

    /**
     * Materializes the given virtual object and produces the necessary effects in the effects list.
     * This transitively also materializes all other virtual objects that are reachable from the
     * entries.
     */
    public void materializeBefore(FixedNode fixed, VirtualObjectNode virtual, GraphEffectList materializeEffects)
    {
        List<AllocatedObjectNode> objects = new ArrayList<>(2);
        List<ValueNode> values = new ArrayList<>(8);
        List<List<MonitorIdNode>> locks = new ArrayList<>();
        List<ValueNode> otherAllocations = new ArrayList<>(2);
        List<Boolean> ensureVirtual = new ArrayList<>(2);
        materializeWithCommit(fixed, virtual, objects, locks, values, ensureVirtual, otherAllocations);

        materializeEffects.addVirtualizationDelta(-(objects.size() + otherAllocations.size()));
        materializeEffects.add("materializeBefore", new Effect()
        {
            @Override
            public void apply(StructuredGraph graph, ArrayList<Node> obsoleteNodes)
            {
                for (ValueNode alloc : otherAllocations)
                {
                    ValueNode otherAllocation = graph.addOrUniqueWithInputs(alloc);
                    if (otherAllocation instanceof FixedWithNextNode)
                    {
                        graph.addBeforeFixed(fixed, (FixedWithNextNode) otherAllocation);
                    }
                }
                if (!objects.isEmpty())
                {
                    CommitAllocationNode commit;
                    if (fixed.predecessor() instanceof CommitAllocationNode)
                    {
                        commit = (CommitAllocationNode) fixed.predecessor();
                    }
                    else
                    {
                        commit = graph.add(new CommitAllocationNode());
                        graph.addBeforeFixed(fixed, commit);
                    }
                    for (AllocatedObjectNode obj : objects)
                    {
                        graph.addWithoutUnique(obj);
                        commit.getVirtualObjects().add(obj.getVirtualObject());
                        obj.setCommit(commit);
                    }
                    for (ValueNode value : values)
                    {
                        commit.getValues().add(graph.addOrUniqueWithInputs(value));
                    }
                    for (List<MonitorIdNode> monitorIds : locks)
                    {
                        commit.addLocks(monitorIds);
                    }
                    commit.getEnsureVirtual().addAll(ensureVirtual);

                    List<AllocatedObjectNode> materializedValues = commit.usages().filter(AllocatedObjectNode.class).snapshot();
                    for (int i = 0; i < commit.getValues().size(); i++)
                    {
                        if (materializedValues.contains(commit.getValues().get(i)))
                        {
                            commit.getValues().set(i, ((AllocatedObjectNode) commit.getValues().get(i)).getVirtualObject());
                        }
                    }
                }
            }
        });
    }

    private void materializeWithCommit(FixedNode fixed, VirtualObjectNode virtual, List<AllocatedObjectNode> objects, List<List<MonitorIdNode>> locks, List<ValueNode> values, List<Boolean> ensureVirtual, List<ValueNode> otherAllocations)
    {
        ObjectState obj = getObjectState(virtual);

        ValueNode[] entries = obj.getEntries();
        ValueNode representation = virtual.getMaterializedRepresentation(fixed, entries, obj.getLocks());
        escape(virtual.getObjectId(), representation);
        obj = getObjectState(virtual);
        PartialEscapeClosure.updateStatesForMaterialized(this, virtual, obj.getMaterializedValue());
        if (representation instanceof AllocatedObjectNode)
        {
            objects.add((AllocatedObjectNode) representation);
            locks.add(LockState.asList(obj.getLocks()));
            ensureVirtual.add(obj.getEnsureVirtualized());
            int pos = values.size();
            while (values.size() < pos + entries.length)
            {
                values.add(null);
            }
            for (int i = 0; i < entries.length; i++)
            {
                if (entries[i] instanceof VirtualObjectNode)
                {
                    VirtualObjectNode entryVirtual = (VirtualObjectNode) entries[i];
                    ObjectState entryObj = getObjectState(entryVirtual);
                    if (entryObj.isVirtual())
                    {
                        materializeWithCommit(fixed, entryVirtual, objects, locks, values, ensureVirtual, otherAllocations);
                        entryObj = getObjectState(entryVirtual);
                    }
                    values.set(pos + i, entryObj.getMaterializedValue());
                }
                else
                {
                    values.set(pos + i, entries[i]);
                }
            }
            objectMaterialized(virtual, (AllocatedObjectNode) representation, values.subList(pos, pos + entries.length));
        }
        else
        {
            otherAllocations.add(representation);
        }
    }

    protected void objectMaterialized(VirtualObjectNode virtual, AllocatedObjectNode representation, List<ValueNode> values)
    {
    }

    public void addObject(int virtual, ObjectState state)
    {
        ensureSize(virtual)[virtual] = state;
    }

    private ObjectState[] ensureSize(int objectId)
    {
        if (objectStates.length <= objectId)
        {
            objectStates = Arrays.copyOf(objectStates, Math.max(objectId * 2, 4));
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
    public String toString()
    {
        return super.toString() + ", Object States: " + Arrays.toString(objectStates);
    }

    @Override
    public boolean equivalentTo(T other)
    {
        int length = Math.max(objectStates.length, other.getStateCount());
        for (int i = 0; i < length; i++)
        {
            ObjectState left = getObjectStateOptional(i);
            ObjectState right = other.getObjectStateOptional(i);
            if (left != right)
            {
                if (left == null || right == null)
                {
                    return false;
                }
                if (!left.equals(right))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public void resetObjectStates(int size)
    {
        objectStates = new ObjectState[size];
    }

    public static boolean identicalObjectStates(PartialEscapeBlockState<?>[] states)
    {
        for (int i = 1; i < states.length; i++)
        {
            if (states[0].objectStates != states[i].objectStates)
            {
                return false;
            }
        }
        return true;
    }

    public static boolean identicalObjectStates(PartialEscapeBlockState<?>[] states, int object)
    {
        for (int i = 1; i < states.length; i++)
        {
            if (states[0].objectStates[object] != states[i].objectStates[object])
            {
                return false;
            }
        }
        return true;
    }

    public void adoptAddObjectStates(PartialEscapeBlockState<?> other)
    {
        if (objectStates != null)
        {
            arrayRefCount.refCount--;
        }
        objectStates = other.objectStates;
        arrayRefCount = other.arrayRefCount;

        if (arrayRefCount.refCount == 1)
        {
            for (ObjectState state : objectStates)
            {
                if (state != null)
                {
                    state.share();
                }
            }
        }
        arrayRefCount.refCount++;
    }
}
