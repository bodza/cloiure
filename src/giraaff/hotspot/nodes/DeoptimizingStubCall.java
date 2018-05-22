package giraaff.hotspot.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;

public abstract class DeoptimizingStubCall extends DeoptimizingFixedWithNextNode
{
    public static final NodeClass<DeoptimizingStubCall> TYPE = NodeClass.create(DeoptimizingStubCall.class);

    public DeoptimizingStubCall(NodeClass<? extends DeoptimizingStubCall> c, Stamp stamp)
    {
        super(c, stamp);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
