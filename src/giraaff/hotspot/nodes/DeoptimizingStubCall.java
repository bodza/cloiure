package giraaff.hotspot.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;

// @class DeoptimizingStubCall
public abstract class DeoptimizingStubCall extends DeoptimizingFixedWithNextNode
{
    public static final NodeClass<DeoptimizingStubCall> TYPE = NodeClass.create(DeoptimizingStubCall.class);

    // @cons
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
