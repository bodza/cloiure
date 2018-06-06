package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;

///
// Guard {@link PhiNode}s merge guard dependencies at control flow merges.
///
// @NodeInfo.allowedUsageTypes "InputType.Guard"
// @class GuardPhiNode
public final class GuardPhiNode extends PhiNode implements GuardingNode
{
    // @def
    public static final NodeClass<GuardPhiNode> TYPE = NodeClass.create(GuardPhiNode.class);

    @Node.OptionalInput(InputType.Guard)
    // @field
    NodeInputList<ValueNode> ___values;

    // @cons GuardPhiNode
    public GuardPhiNode(AbstractMergeNode __merge)
    {
        super(TYPE, StampFactory.forVoid(), __merge);
        this.___values = new NodeInputList<>(this);
    }

    // @cons GuardPhiNode
    public GuardPhiNode(AbstractMergeNode __merge, ValueNode[] __values)
    {
        super(TYPE, StampFactory.forVoid(), __merge);
        this.___values = new NodeInputList<>(this, __values);
    }

    @Override
    public NodeInputList<ValueNode> values()
    {
        return this.___values;
    }
}
