package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.extended.GuardingNode;

/**
 * Guard {@link PhiNode}s merge guard dependencies at control flow merges.
 */
public final class GuardPhiNode extends PhiNode implements GuardingNode
{
    public static final NodeClass<GuardPhiNode> TYPE = NodeClass.create(GuardPhiNode.class);
    @OptionalInput(InputType.Guard) NodeInputList<ValueNode> values;

    public GuardPhiNode(AbstractMergeNode merge)
    {
        super(TYPE, StampFactory.forVoid(), merge);
        this.values = new NodeInputList<>(this);
    }

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
