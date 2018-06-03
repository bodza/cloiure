package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ValueNode;

///
// Memory {@code PhiNode}s merge memory dependencies at control flow merges.
///
// @NodeInfo.allowedUsageTypes "Memory"
// @class MemoryPhiNode
public final class MemoryPhiNode extends PhiNode implements MemoryNode
{
    // @def
    public static final NodeClass<MemoryPhiNode> TYPE = NodeClass.create(MemoryPhiNode.class);

    @Input(InputType.Memory)
    // @field
    NodeInputList<ValueNode> ___values;
    // @field
    protected final LocationIdentity ___locationIdentity;

    // @cons
    public MemoryPhiNode(AbstractMergeNode __merge, LocationIdentity __locationIdentity)
    {
        super(TYPE, StampFactory.forVoid(), __merge);
        this.___locationIdentity = __locationIdentity;
        this.___values = new NodeInputList<>(this);
    }

    // @cons
    public MemoryPhiNode(AbstractMergeNode __merge, LocationIdentity __locationIdentity, ValueNode[] __values)
    {
        super(TYPE, StampFactory.forVoid(), __merge);
        this.___locationIdentity = __locationIdentity;
        this.___values = new NodeInputList<>(this, __values);
    }

    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    @Override
    public NodeInputList<ValueNode> values()
    {
        return this.___values;
    }
}
