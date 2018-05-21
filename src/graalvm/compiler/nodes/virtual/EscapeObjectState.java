package graalvm.compiler.nodes.virtual;

import graalvm.compiler.graph.Node.ValueNumberable;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.VirtualState;

public abstract class EscapeObjectState extends VirtualState implements ValueNumberable
{
    public static final NodeClass<EscapeObjectState> TYPE = NodeClass.create(EscapeObjectState.class);

    @Input protected VirtualObjectNode object;

    public VirtualObjectNode object()
    {
        return object;
    }

    public EscapeObjectState(NodeClass<? extends EscapeObjectState> c, VirtualObjectNode object)
    {
        super(c);
        this.object = object;
    }

    @Override
    public abstract EscapeObjectState duplicateWithVirtualState();

    @Override
    public boolean isPartOfThisState(VirtualState state)
    {
        return this == state;
    }

    @Override
    public void applyToVirtual(VirtualClosure closure)
    {
        closure.apply(this);
    }
}
