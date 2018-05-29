package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node.ValueNumberable;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GuardingNode;

/**
 * A proxy is inserted at loop exits for any value that is created inside the loop (i.e. was not
 * live on entry to the loop) and is (potentially) used after the loop.
 */
// @class ProxyNode
public abstract class ProxyNode extends FloatingNode implements ValueNumberable
{
    public static final NodeClass<ProxyNode> TYPE = NodeClass.create(ProxyNode.class);

    @Input(InputType.Association) LoopExitNode loopExit;

    // @cons
    protected ProxyNode(NodeClass<? extends ProxyNode> c, Stamp stamp, LoopExitNode proxyPoint)
    {
        super(c, stamp);
        this.loopExit = proxyPoint;
    }

    public abstract ValueNode value();

    public LoopExitNode proxyPoint()
    {
        return loopExit;
    }

    public static ValueProxyNode forValue(ValueNode value, LoopExitNode exit, StructuredGraph graph)
    {
        return graph.unique(new ValueProxyNode(value, exit));
    }

    public static GuardProxyNode forGuard(GuardingNode value, LoopExitNode exit, StructuredGraph graph)
    {
        return graph.unique(new GuardProxyNode(value, exit));
    }
}
