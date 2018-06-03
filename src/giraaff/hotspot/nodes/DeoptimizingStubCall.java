package giraaff.hotspot.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;

// @class DeoptimizingStubCall
public abstract class DeoptimizingStubCall extends DeoptimizingFixedWithNextNode
{
    // @def
    public static final NodeClass<DeoptimizingStubCall> TYPE = NodeClass.create(DeoptimizingStubCall.class);

    // @cons
    public DeoptimizingStubCall(NodeClass<? extends DeoptimizingStubCall> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
