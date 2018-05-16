package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;

@NodeInfo(cycles = CYCLES_UNKNOWN, size = SIZE_UNKNOWN)
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
