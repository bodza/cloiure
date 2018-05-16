package graalvm.compiler.nodes.virtual;

import static graalvm.compiler.nodeinfo.InputType.Association;
import static graalvm.compiler.nodeinfo.InputType.Extension;
import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeCycles;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.NodeSize;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.AbstractNewObjectNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.WriteNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.VirtualizableAllocation;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import org.graalvm.word.LocationIdentity;

@NodeInfo(nameTemplate = "Alloc {i#virtualObjects}",
          allowedUsageTypes = {Extension, Memory},
          cycles = CYCLES_UNKNOWN,
          cyclesRationale = "We don't know statically how many, and which, allocations are done.",
          size = SIZE_UNKNOWN,
          sizeRationale = "We don't know statically how much code for which allocations has to be generated."
)
public final class CommitAllocationNode extends FixedWithNextNode implements VirtualizableAllocation, Lowerable, Simplifiable, MemoryCheckpoint.Single
{
    public static final NodeClass<CommitAllocationNode> TYPE = NodeClass.create(CommitAllocationNode.class);

    @Input NodeInputList<VirtualObjectNode> virtualObjects = new NodeInputList<>(this);
    @Input NodeInputList<ValueNode> values = new NodeInputList<>(this);
    @Input(Association) NodeInputList<MonitorIdNode> locks = new NodeInputList<>(this);
    protected ArrayList<Integer> lockIndexes = new ArrayList<>(Arrays.asList(0));
    protected ArrayList<Boolean> ensureVirtual = new ArrayList<>();

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
    public boolean verify()
    {
        assertTrue(virtualObjects.size() + 1 == lockIndexes.size(), "lockIndexes size doesn't match %s, %s", virtualObjects, lockIndexes);
        assertTrue(lockIndexes.get(lockIndexes.size() - 1) == locks.size(), "locks size doesn't match %s,%s", lockIndexes, locks);
        int valueCount = 0;
        for (VirtualObjectNode virtual : virtualObjects)
        {
            valueCount += virtual.entryCount();
        }
        assertTrue(values.size() == valueCount, "values size doesn't match");
        assertTrue(virtualObjects.size() == ensureVirtual.size(), "ensureVirtual size doesn't match");
        return super.verify();
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
    public Map<Object, Object> getDebugProperties(Map<Object, Object> map)
    {
        Map<Object, Object> properties = super.getDebugProperties(map);
        int valuePos = 0;
        for (int objIndex = 0; objIndex < virtualObjects.size(); objIndex++)
        {
            VirtualObjectNode virtual = virtualObjects.get(objIndex);
            if (virtual == null)
            {
                // Could occur in invalid graphs
                properties.put("object(" + objIndex + ")", "null");
                continue;
            }
            StringBuilder s = new StringBuilder();
            s.append(virtual.type().toJavaName(false)).append("[");
            for (int i = 0; i < virtual.entryCount(); i++)
            {
                ValueNode value = values.get(valuePos++);
                s.append(i == 0 ? "" : ",").append(value == null ? "_" : value.toString(Verbosity.Id));
            }
            s.append("]");
            if (!getLocks(objIndex).isEmpty())
            {
                s.append(" locked(").append(getLocks(objIndex)).append(")");
            }
            properties.put("object(" + virtual.toString(Verbosity.Id) + ")", s.toString());
        }
        return properties;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        boolean[] used = new boolean[virtualObjects.size()];
        int usedCount = 0;
        for (AllocatedObjectNode addObject : usages().filter(AllocatedObjectNode.class))
        {
            int index = virtualObjects.indexOf(addObject.getVirtualObject());
            assert !used[index];
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

    @Override
    public NodeCycles estimatedNodeCycles()
    {
        List<VirtualObjectNode> v = getVirtualObjects();
        int fieldWriteCount = 0;
        for (int i = 0; i < v.size(); i++)
        {
            fieldWriteCount += v.get(i).entryCount();
        }
        int rawValueWrites = NodeCycles.compute(WriteNode.TYPE.cycles(), fieldWriteCount).value;
        int rawValuesTlabBumps = AbstractNewObjectNode.TYPE.cycles().value;
        return NodeCycles.compute(rawValueWrites + rawValuesTlabBumps);
    }

    @Override
    public NodeSize estimatedNodeSize()
    {
        List<VirtualObjectNode> v = getVirtualObjects();
        int fieldWriteCount = 0;
        for (int i = 0; i < v.size(); i++)
        {
            fieldWriteCount += v.get(i).entryCount();
        }
        int rawValueWrites = NodeSize.compute(WriteNode.TYPE.size(), fieldWriteCount).value;
        int rawValuesTlabBumps = AbstractNewObjectNode.TYPE.size().value;
        return NodeSize.compute(rawValueWrites + rawValuesTlabBumps);
    }
}
