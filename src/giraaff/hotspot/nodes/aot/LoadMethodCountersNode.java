package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.hotspot.nodes.type.MethodCountersPointerStamp;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public class LoadMethodCountersNode extends FloatingNode implements LIRLowerable
{
    public static final NodeClass<LoadMethodCountersNode> TYPE = NodeClass.create(LoadMethodCountersNode.class);

    ResolvedJavaMethod method;

    public LoadMethodCountersNode(ResolvedJavaMethod method)
    {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.method = method;
    }

    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    public static NodeIterable<LoadMethodCountersNode> getLoadMethodCountersNodes(StructuredGraph graph)
    {
        return graph.getNodes().filter(LoadMethodCountersNode.class);
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // TODO: With AOT we don't need this, as this node will be replaced.
        // Implement later when profiling is needed in the JIT mode.
        throw GraalError.unimplemented();
    }
}
