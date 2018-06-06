package giraaff.nodes.virtual;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.VirtualState;

// @class EscapeObjectState
public abstract class EscapeObjectState extends VirtualState implements Node.ValueNumberable
{
    // @def
    public static final NodeClass<EscapeObjectState> TYPE = NodeClass.create(EscapeObjectState.class);

    @Node.Input
    // @field
    protected VirtualObjectNode ___object;

    public VirtualObjectNode object()
    {
        return this.___object;
    }

    // @cons EscapeObjectState
    public EscapeObjectState(NodeClass<? extends EscapeObjectState> __c, VirtualObjectNode __object)
    {
        super(__c);
        this.___object = __object;
    }

    @Override
    public abstract EscapeObjectState duplicateWithVirtualState();

    @Override
    public boolean isPartOfThisState(VirtualState __state)
    {
        return this == __state;
    }

    @Override
    public void applyToVirtual(VirtualState.VirtualClosure __closure)
    {
        __closure.apply(this);
    }
}
