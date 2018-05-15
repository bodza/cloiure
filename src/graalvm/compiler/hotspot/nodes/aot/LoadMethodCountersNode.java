package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_4;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_4;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.hotspot.nodes.type.MethodCountersPointerStamp;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo(cycles = CYCLES_4, size = SIZE_4)
public class LoadMethodCountersNode extends FloatingNode implements LIRLowerable {
    public static final NodeClass<LoadMethodCountersNode> TYPE = NodeClass.create(LoadMethodCountersNode.class);

    ResolvedJavaMethod method;

    public LoadMethodCountersNode(ResolvedJavaMethod method) {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.method = method;
    }

    public ResolvedJavaMethod getMethod() {
        return method;
    }

    public static NodeIterable<LoadMethodCountersNode> getLoadMethodCountersNodes(StructuredGraph graph) {
        return graph.getNodes().filter(LoadMethodCountersNode.class);
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // TODO: With AOT we don't need this, as this node will be replaced.
        // Implement later when profiling is needed in the JIT mode.
        throw GraalError.unimplemented();
    }
}
