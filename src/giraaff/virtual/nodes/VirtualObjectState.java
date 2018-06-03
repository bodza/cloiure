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
// @class VirtualObjectState
public final class VirtualObjectState extends EscapeObjectState implements Node.ValueNumberable
{
    // @def
    public static final NodeClass<VirtualObjectState> TYPE = NodeClass.create(VirtualObjectState.class);

    @OptionalInput
    // @field
    NodeInputList<ValueNode> values;

    public NodeInputList<ValueNode> values()
    {
        return values;
    }

    // @cons
    public VirtualObjectState(VirtualObjectNode __object, ValueNode[] __values)
    {
        super(TYPE, __object);
        this.values = new NodeInputList<>(this, __values);
    }

    // @cons
    public VirtualObjectState(VirtualObjectNode __object, List<ValueNode> __values)
    {
        super(TYPE, __object);
        this.values = new NodeInputList<>(this, __values);
    }

    @Override
    public VirtualObjectState duplicateWithVirtualState()
    {
        return graph().addWithoutUnique(new VirtualObjectState(object(), values));
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> __closure)
    {
        for (ValueNode __value : values)
        {
            if (__value != null)
            {
                __closure.apply(this, __value);
            }
        }
    }
}
