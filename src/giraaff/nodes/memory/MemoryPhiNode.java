package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ValueNode;

/**
 * Memory {@code PhiNode}s merge memory dependencies at control flow merges.
 */
// NodeInfo.allowedUsageTypes = Memory
public final class MemoryPhiNode extends PhiNode implements MemoryNode
{
    public static final NodeClass<MemoryPhiNode> TYPE = NodeClass.create(MemoryPhiNode.class);
    @Input(InputType.Memory) NodeInputList<ValueNode> values;
    protected final LocationIdentity locationIdentity;

    public MemoryPhiNode(AbstractMergeNode merge, LocationIdentity locationIdentity)
    {
        super(TYPE, StampFactory.forVoid(), merge);
        this.locationIdentity = locationIdentity;
        this.values = new NodeInputList<>(this);
    }

    public MemoryPhiNode(AbstractMergeNode merge, LocationIdentity locationIdentity, ValueNode[] values)
    {
        super(TYPE, StampFactory.forVoid(), merge);
        this.locationIdentity = locationIdentity;
        this.values = new NodeInputList<>(this, values);
    }

    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    @Override
    public NodeInputList<ValueNode> values()
    {
        return values;
    }

    @Override
    protected String valueDescription()
    {
        return locationIdentity.toString();
    }
}
