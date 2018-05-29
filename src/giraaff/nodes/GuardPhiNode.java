package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;

/**
 * Guard {@link PhiNode}s merge guard dependencies at control flow merges.
 */
// @NodeInfo.allowedUsageTypes "Guard"
// @class GuardPhiNode
public final class GuardPhiNode extends PhiNode implements GuardingNode
{
    public static final NodeClass<GuardPhiNode> TYPE = NodeClass.create(GuardPhiNode.class);

    @OptionalInput(InputType.Guard) NodeInputList<ValueNode> values;

    // @cons
    public GuardPhiNode(AbstractMergeNode merge)
    {
        super(TYPE, StampFactory.forVoid(), merge);
        this.values = new NodeInputList<>(this);
    }

    // @cons
    public GuardPhiNode(AbstractMergeNode merge, ValueNode[] values)
    {
        super(TYPE, StampFactory.forVoid(), merge);
        this.values = new NodeInputList<>(this, values);
    }

    @Override
    public NodeInputList<ValueNode> values()
    {
        return values;
    }
}
