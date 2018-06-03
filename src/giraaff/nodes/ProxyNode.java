package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node.ValueNumberable;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GuardingNode;

///
// A proxy is inserted at loop exits for any value that is created inside the loop (i.e. was not
// live on entry to the loop) and is (potentially) used after the loop.
///
// @class ProxyNode
public abstract class ProxyNode extends FloatingNode implements ValueNumberable
{
    // @def
    public static final NodeClass<ProxyNode> TYPE = NodeClass.create(ProxyNode.class);

    @Input(InputType.Association)
    // @field
    LoopExitNode ___loopExit;

    // @cons
    protected ProxyNode(NodeClass<? extends ProxyNode> __c, Stamp __stamp, LoopExitNode __proxyPoint)
    {
        super(__c, __stamp);
        this.___loopExit = __proxyPoint;
    }

    public abstract ValueNode value();

    public LoopExitNode proxyPoint()
    {
        return this.___loopExit;
    }

    public static ValueProxyNode forValue(ValueNode __value, LoopExitNode __exit, StructuredGraph __graph)
    {
        return __graph.unique(new ValueProxyNode(__value, __exit));
    }

    public static GuardProxyNode forGuard(GuardingNode __value, LoopExitNode __exit, StructuredGraph __graph)
    {
        return __graph.unique(new GuardProxyNode(__value, __exit));
    }
}
