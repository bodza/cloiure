package giraaff.virtual.nodes;

import java.util.List;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.ValueNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * This class encapsulated the virtual state of an escape analyzed object.
 */
public final class VirtualObjectState extends EscapeObjectState implements Node.ValueNumberable
{
    public static final NodeClass<VirtualObjectState> TYPE = NodeClass.create(VirtualObjectState.class);
    @OptionalInput NodeInputList<ValueNode> values;

    public NodeInputList<ValueNode> values()
    {
        return values;
    }

    public VirtualObjectState(VirtualObjectNode object, ValueNode[] values)
    {
        super(TYPE, object);
        this.values = new NodeInputList<>(this, values);
    }

    public VirtualObjectState(VirtualObjectNode object, List<ValueNode> values)
    {
        super(TYPE, object);
        this.values = new NodeInputList<>(this, values);
    }

    @Override
    public VirtualObjectState duplicateWithVirtualState()
    {
        return graph().addWithoutUnique(new VirtualObjectState(object(), values));
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> closure)
    {
        for (ValueNode value : values)
        {
            if (value != null)
            {
                closure.apply(this, value);
            }
        }
    }
}
