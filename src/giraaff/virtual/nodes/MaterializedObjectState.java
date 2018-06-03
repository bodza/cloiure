package giraaff.virtual.nodes;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * This class encapsulated the materialized state of an escape analyzed object.
 */
// @class MaterializedObjectState
public final class MaterializedObjectState extends EscapeObjectState implements Node.ValueNumberable
{
    // @def
    public static final NodeClass<MaterializedObjectState> TYPE = NodeClass.create(MaterializedObjectState.class);

    @Input
    // @field
    ValueNode materializedValue;

    public ValueNode materializedValue()
    {
        return materializedValue;
    }

    // @cons
    public MaterializedObjectState(VirtualObjectNode __object, ValueNode __materializedValue)
    {
        super(TYPE, __object);
        this.materializedValue = __materializedValue;
    }

    @Override
    public MaterializedObjectState duplicateWithVirtualState()
    {
        return graph().addWithoutUnique(new MaterializedObjectState(object(), materializedValue));
    }

    @Override
    public void applyToNonVirtual(NodeClosure<? super ValueNode> __closure)
    {
        __closure.apply(this, materializedValue);
    }
}
