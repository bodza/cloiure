package giraaff.virtual.nodes;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.VirtualState;
import giraaff.nodes.virtual.EscapeObjectState;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// This class encapsulated the materialized state of an escape analyzed object.
///
// @class MaterializedObjectState
public final class MaterializedObjectState extends EscapeObjectState implements Node.ValueNumberable
{
    // @def
    public static final NodeClass<MaterializedObjectState> TYPE = NodeClass.create(MaterializedObjectState.class);

    @Node.Input
    // @field
    ValueNode ___materializedValue;

    public ValueNode materializedValue()
    {
        return this.___materializedValue;
    }

    // @cons MaterializedObjectState
    public MaterializedObjectState(VirtualObjectNode __object, ValueNode __materializedValue)
    {
        super(TYPE, __object);
        this.___materializedValue = __materializedValue;
    }

    @Override
    public MaterializedObjectState duplicateWithVirtualState()
    {
        return graph().addWithoutUnique(new MaterializedObjectState(object(), this.___materializedValue));
    }

    @Override
    public void applyToNonVirtual(VirtualState.NodeClosure<? super ValueNode> __closure)
    {
        __closure.apply(this, this.___materializedValue);
    }
}
