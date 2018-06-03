package giraaff.nodes.virtual;

import giraaff.graph.Node.ValueNumberable;
import giraaff.graph.NodeClass;
import giraaff.nodes.VirtualState;

// @class EscapeObjectState
public abstract class EscapeObjectState extends VirtualState implements ValueNumberable
{
    // @def
    public static final NodeClass<EscapeObjectState> TYPE = NodeClass.create(EscapeObjectState.class);

    @Input
    // @field
    protected VirtualObjectNode ___object;

    public VirtualObjectNode object()
    {
        return this.___object;
    }

    // @cons
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
    public void applyToVirtual(VirtualClosure __closure)
    {
        __closure.apply(this);
    }
}
