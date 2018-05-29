package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.graph.NodeClass;
import giraaff.hotspot.nodes.type.MethodCountersPointerStamp;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class ResolveMethodAndLoadCountersNode
public final class ResolveMethodAndLoadCountersNode extends DeoptimizingFixedWithNextNode implements Lowerable
{
    public static final NodeClass<ResolveMethodAndLoadCountersNode> TYPE = NodeClass.create(ResolveMethodAndLoadCountersNode.class);

    ResolvedJavaMethod method;
    @Input ValueNode hub;

    // @cons
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
