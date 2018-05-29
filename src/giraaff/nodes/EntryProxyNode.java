package giraaff.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.ValueProxy;

/**
 * Proxy node that is used in OSR. This node drops the stamp information from the value, since the
 * types we see during OSR may be too precise (if a branch was not parsed for example).
 */
// @class EntryProxyNode
public final class EntryProxyNode extends FloatingNode implements ValueProxy
{
    public static final NodeClass<EntryProxyNode> TYPE = NodeClass.create(EntryProxyNode.class);

    @Input(InputType.Association) EntryMarkerNode proxyPoint;
    @Input ValueNode value;

    // @cons
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
