package giraaff.virtual.nodes;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * This class encapsulated the materialized state of an escape analyzed object.
 */
public final class MaterializedObjectState extends EscapeObjectState implements Node.ValueNumberable
{
    public static final NodeClass<MaterializedObjectState> TYPE = NodeClass.create(MaterializedObjectState.class);
    @Input ValueNode materializedValue;

    public ValueNode materializedValue()
    {
        return materializedValue;
    }

    public MaterializedObjectState(VirtualObjectNode object, ValueNode materializedValue)
    {
        super(TYPE, object);
        this.materializedValue = materializedValue;
    }

    @Override
    public MaterializedObjectState duplicateWithVirtualState()
    {
        return graph().addWithoutUnique(new MaterializedObjectState(object(), materializedValue));
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> closure)
    {
        closure.apply(this, materializedValue);
    }
}
