package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.spi.ValueProxy;

/**
 * Proxy node that is used in OSR. This node drops the stamp information from the value, since the
 * types we see during OSR may be too precise (if a branch was not parsed for example).
 */
public final class EntryProxyNode extends FloatingNode implements ValueProxy
{
    public static final NodeClass<EntryProxyNode> TYPE = NodeClass.create(EntryProxyNode.class);
    @Input(Association) EntryMarkerNode proxyPoint;
    @Input ValueNode value;

    public EntryProxyNode(ValueNode value, EntryMarkerNode proxyPoint)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT).unrestricted());
        this.value = value;
        this.proxyPoint = proxyPoint;
    }

    public ValueNode value()
    {
        return value;
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return value();
    }

    @Override
    public GuardingNode getGuard()
    {
        return proxyPoint;
    }
}
