package giraaff.nodes.virtual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;

// @NodeInfo.allowedUsageTypes "Extension, Memory"
// @class CommitAllocationNode
public final class CommitAllocationNode extends FixedWithNextNode implements VirtualizableAllocation, Lowerable, Simplifiable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<CommitAllocationNode> TYPE = NodeClass.create(CommitAllocationNode.class);

    @Input
    // @field
    NodeInputList<VirtualObjectNode> virtualObjects = new NodeInputList<>(this);
    @Input
    // @field
    NodeInputList<ValueNode> values = new NodeInputList<>(this);
    @Input(InputType.Association)
    // @field
    NodeInputList<MonitorIdNode> locks = new NodeInputList<>(this);
    // @field
    protected ArrayList<Integer> lockIndexes = new ArrayList<>(Arrays.asList(0));
    // @field
    protected ArrayList<Boolean> ensureVirtual = new ArrayList<>();

    // @cons
    public CommitAllocationNode()
    {
        super(TYPE, StampFactory.forVoid());
    }

    public List<VirtualObjectNode> getVirtualObjects()
    {
        return virtualObjects;
    }

    public List<ValueNode> getValues()
    {
        return values;
    }

    public List<MonitorIdNode> getLocks(int __objIndex)
    {
        return locks.subList(lockIndexes.get(__objIndex), lockIndexes.get(__objIndex + 1));
    }

    public List<Boolean> getEnsureVirtual()
    {
        return ensureVirtual;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        for (int __i = 0; __i < virtualObjects.size(); __i++)
        {
            if (ensureVirtual.get(__i))
            {
                EnsureVirtualizedNode.ensureVirtualFailure(this, virtualObjects.get(__i).stamp(NodeView.DEFAULT));
            }
        }
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locks.isEmpty() ? LocationIdentity.init() : LocationIdentity.any();
    }

    @Override
    public void afterClone(Node __other)
    {
        lockIndexes = new ArrayList<>(lockIndexes);
    }

    public void addLocks(List<MonitorIdNode> __monitorIds)
    {
        locks.addAll(__monitorIds);
        lockIndexes.add(locks.size());
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        int __pos = 0;
        for (int __i = 0; __i < virtualObjects.size(); __i++)
        {
            VirtualObjectNode __virtualObject = virtualObjects.get(__i);
            int __entryCount = __virtualObject.entryCount();
            __tool.createVirtualObject(__virtualObject, values.subList(__pos, __pos + __entryCount).toArray(new ValueNode[__entryCount]), getLocks(__i), ensureVirtual.get(__i));
            __pos += __entryCount;
        }
        __tool.delete();
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        boolean[] __used = new boolean[virtualObjects.size()];
        int __usedCount = 0;
        for (AllocatedObjectNode __addObject : usages().filter(AllocatedObjectNode.class))
        {
            int __index = virtualObjects.indexOf(__addObject.getVirtualObject());
            __used[__index] = true;
            __usedCount++;
        }
        if (__usedCount == 0)
        {
            List<Node> __inputSnapshot = inputs().snapshot();
            graph().removeFixed(this);
            for (Node __input : __inputSnapshot)
            {
                __tool.removeIfUnused(__input);
            }
            return;
        }
        boolean __progress;
        do
        {
            __progress = false;
            int __valuePos = 0;
            for (int __objIndex = 0; __objIndex < virtualObjects.size(); __objIndex++)
            {
                VirtualObjectNode __virtualObject = virtualObjects.get(__objIndex);
                if (__used[__objIndex])
                {
                    for (int __i = 0; __i < __virtualObject.entryCount(); __i++)
                    {
                        int __index = virtualObjects.indexOf(values.get(__valuePos + __i));
                        if (__index != -1 && !__used[__index])
                        {
                            __progress = true;
                            __used[__index] = true;
                            __usedCount++;
                        }
                    }
                }
                __valuePos += __virtualObject.entryCount();
            }
        } while (__progress);

        if (__usedCount < virtualObjects.size())
        {
            List<VirtualObjectNode> __newVirtualObjects = new ArrayList<>(__usedCount);
            List<MonitorIdNode> __newLocks = new ArrayList<>(__usedCount);
            ArrayList<Integer> __newLockIndexes = new ArrayList<>(__usedCount + 1);
            ArrayList<Boolean> __newEnsureVirtual = new ArrayList<>(__usedCount);
            __newLockIndexes.add(0);
            List<ValueNode> __newValues = new ArrayList<>();
            int __valuePos = 0;
            for (int __objIndex = 0; __objIndex < virtualObjects.size(); __objIndex++)
            {
                VirtualObjectNode __virtualObject = virtualObjects.get(__objIndex);
                if (__used[__objIndex])
                {
                    __newVirtualObjects.add(__virtualObject);
                    __newLocks.addAll(getLocks(__objIndex));
                    __newLockIndexes.add(__newLocks.size());
                    __newValues.addAll(values.subList(__valuePos, __valuePos + __virtualObject.entryCount()));
                    __newEnsureVirtual.add(ensureVirtual.get(__objIndex));
                }
                __valuePos += __virtualObject.entryCount();
            }
            virtualObjects.clear();
            virtualObjects.addAll(__newVirtualObjects);
            locks.clear();
            locks.addAll(__newLocks);
            values.clear();
            values.addAll(__newValues);
            lockIndexes = __newLockIndexes;
            ensureVirtual = __newEnsureVirtual;
        }
    }
}
