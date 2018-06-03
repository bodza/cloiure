package giraaff.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.ValueProxy;

///
// Proxy node that is used in OSR. This node drops the stamp information from the value, since the
// types we see during OSR may be too precise (if a branch was not parsed for example).
///
// @class EntryProxyNode
public final class EntryProxyNode extends FloatingNode implements ValueProxy
{
    // @def
    public static final NodeClass<EntryProxyNode> TYPE = NodeClass.create(EntryProxyNode.class);

    @Input(InputType.Association)
    // @field
    EntryMarkerNode ___proxyPoint;
    @Input
    // @field
    ValueNode ___value;

    // @cons
    public EntryProxyNode(ValueNode __value, EntryMarkerNode __proxyPoint)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT).unrestricted());
        this.___value = __value;
        this.___proxyPoint = __proxyPoint;
    }

    public ValueNode value()
    {
        return this.___value;
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return value();
    }

    @Override
    public GuardingNode getGuard()
    {
        return this.___proxyPoint;
    }
}
