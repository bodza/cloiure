package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;

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
