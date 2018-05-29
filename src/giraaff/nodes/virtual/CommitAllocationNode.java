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
    public static final NodeClass<CommitAllocationNode> TYPE = NodeClass.create(CommitAllocationNode.class);

    @Input NodeInputList<VirtualObjectNode> virtualObjects = new NodeInputList<>(this);
    @Input NodeInputList<ValueNode> values = new NodeInputList<>(this);
    @Input(InputType.Association) NodeInputList<MonitorIdNode> locks = new NodeInputList<>(this);
    protected ArrayList<Integer> lockIndexes = new ArrayList<>(Arrays.asList(0));
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

    public List<MonitorIdNode> getLocks(int objIndex)
    {
        return locks.subList(lockIndexes.get(objIndex), lockIndexes.get(objIndex + 1));
    }

    public List<Boolean> getEnsureVirtual()
    {
        return ensureVirtual;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        for (int i = 0; i < virtualObjects.size(); i++)
        {
            if (ensureVirtual.get(i))
            {
                EnsureVirtualizedNode.ensureVirtualFailure(this, virtualObjects.get(i).stamp(NodeView.DEFAULT));
            }
        }
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locks.isEmpty() ? LocationIdentity.init() : LocationIdentity.any();
    }

    @Override
    public void afterClone(Node other)
    {
        lockIndexes = new ArrayList<>(lockIndexes);
    }

    public void addLocks(List<MonitorIdNode> monitorIds)
    {
        locks.addAll(monitorIds);
        lockIndexes.add(locks.size());
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        int pos = 0;
        for (int i = 0; i < virtualObjects.size(); i++)
        {
            VirtualObjectNode virtualObject = virtualObjects.get(i);
            int entryCount = virtualObject.entryCount();
            tool.createVirtualObject(virtualObject, values.subList(pos, pos + entryCount).toArray(new ValueNode[entryCount]), getLocks(i), ensureVirtual.get(i));
            pos += entryCount;
        }
        tool.delete();
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        boolean[] used = new boolean[virtualObjects.size()];
        int usedCount = 0;
        for (AllocatedObjectNode addObject : usages().filter(AllocatedObjectNode.class))
        {
            int index = virtualObjects.indexOf(addObject.getVirtualObject());
            used[index] = true;
            usedCount++;
        }
        if (usedCount == 0)
        {
            List<Node> inputSnapshot = inputs().snapshot();
            graph().removeFixed(this);
            for (Node input : inputSnapshot)
            {
                tool.removeIfUnused(input);
            }
            return;
        }
        boolean progress;
        do
        {
            progress = false;
            int valuePos = 0;
            for (int objIndex = 0; objIndex < virtualObjects.size(); objIndex++)
            {
                VirtualObjectNode virtualObject = virtualObjects.get(objIndex);
                if (used[objIndex])
                {
                    for (int i = 0; i < virtualObject.entryCount(); i++)
                    {
                        int index = virtualObjects.indexOf(values.get(valuePos + i));
                        if (index != -1 && !used[index])
                        {
                            progress = true;
                            used[index] = true;
                            usedCount++;
                        }
                    }
                }
                valuePos += virtualObject.entryCount();
            }
        } while (progress);

        if (usedCount < virtualObjects.size())
        {
            List<VirtualObjectNode> newVirtualObjects = new ArrayList<>(usedCount);
            List<MonitorIdNode> newLocks = new ArrayList<>(usedCount);
            ArrayList<Integer> newLockIndexes = new ArrayList<>(usedCount + 1);
            ArrayList<Boolean> newEnsureVirtual = new ArrayList<>(usedCount);
            newLockIndexes.add(0);
            List<ValueNode> newValues = new ArrayList<>();
            int valuePos = 0;
            for (int objIndex = 0; objIndex < virtualObjects.size(); objIndex++)
            {
                VirtualObjectNode virtualObject = virtualObjects.get(objIndex);
                if (used[objIndex])
                {
                    newVirtualObjects.add(virtualObject);
                    newLocks.addAll(getLocks(objIndex));
                    newLockIndexes.add(newLocks.size());
                    newValues.addAll(values.subList(valuePos, valuePos + virtualObject.entryCount()));
                    newEnsureVirtual.add(ensureVirtual.get(objIndex));
                }
                valuePos += virtualObject.entryCount();
            }
            virtualObjects.clear();
            virtualObjects.addAll(newVirtualObjects);
            locks.clear();
            locks.addAll(newLocks);
            values.clear();
            values.addAll(newValues);
            lockIndexes = newLockIndexes;
            ensureVirtual = newEnsureVirtual;
        }
    }
}
