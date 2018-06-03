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
    // @def
    public static final NodeClass<ResolveMethodAndLoadCountersNode> TYPE = NodeClass.create(ResolveMethodAndLoadCountersNode.class);

    // @field
    ResolvedJavaMethod method;
    @Input
    // @field
    ValueNode hub;

    // @cons
    public ResolveMethodAndLoadCountersNode(ResolvedJavaMethod __method, ValueNode __hub)
    {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.method = __method;
        this.hub = __hub;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
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
