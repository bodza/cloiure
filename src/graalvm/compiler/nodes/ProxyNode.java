package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node.ValueNumberable;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.GuardingNode;

/**
 * A proxy is inserted at loop exits for any value that is created inside the loop (i.e. was not
 * live on entry to the loop) and is (potentially) used after the loop.
 */
@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public abstract class ProxyNode extends FloatingNode implements ValueNumberable {

    public static final NodeClass<ProxyNode> TYPE = NodeClass.create(ProxyNode.class);
    @Input(Association) LoopExitNode loopExit;

    protected ProxyNode(NodeClass<? extends ProxyNode> c, Stamp stamp, LoopExitNode proxyPoint) {
        super(c, stamp);
        assert proxyPoint != null;
        this.loopExit = proxyPoint;
    }

    public abstract ValueNode value();

    public LoopExitNode proxyPoint() {
        return loopExit;
    }

    @Override
    public boolean verify() {
        assert !(value() instanceof ProxyNode) || ((ProxyNode) value()).loopExit != loopExit;
        return super.verify();
    }

    public static ValueProxyNode forValue(ValueNode value, LoopExitNode exit, StructuredGraph graph) {
        return graph.unique(new ValueProxyNode(value, exit));
    }

    public static GuardProxyNode forGuard(GuardingNode value, LoopExitNode exit, StructuredGraph graph) {
        return graph.unique(new GuardProxyNode(value, exit));
    }
}
