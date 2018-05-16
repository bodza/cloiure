package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_4;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_16;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.nodes.type.MethodCountersPointerStamp;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo(cycles = CYCLES_4, size = SIZE_16)
public class ResolveMethodAndLoadCountersNode extends DeoptimizingFixedWithNextNode implements Lowerable
{
    public static final NodeClass<ResolveMethodAndLoadCountersNode> TYPE = NodeClass.create(ResolveMethodAndLoadCountersNode.class);

    ResolvedJavaMethod method;
    @Input ValueNode hub;

    public ResolveMethodAndLoadCountersNode(ResolvedJavaMethod method, ValueNode hub)
    {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.method = method;
        this.hub = hub;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    public ValueNode getHub()
    {
        return hub;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
