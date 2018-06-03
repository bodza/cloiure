package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.Proxy;

// @NodeInfo.allowedUsageTypes "Guard"
// @class GuardProxyNode
public final class GuardProxyNode extends ProxyNode implements GuardingNode, Proxy, LIRLowerable, Canonicalizable
{
    // @def
    public static final NodeClass<GuardProxyNode> TYPE = NodeClass.create(GuardProxyNode.class);

    @OptionalInput(InputType.Guard)
    // @field
    GuardingNode ___value;

    // @cons
    public GuardProxyNode(GuardingNode __value, LoopExitNode __proxyPoint)
    {
        super(TYPE, StampFactory.forVoid(), __proxyPoint);
        this.___value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    public void setValue(GuardingNode __newValue)
    {
        this.updateUsages(this.___value.asNode(), __newValue.asNode());
        this.___value = __newValue;
    }

    @Override
    public ValueNode value()
    {
        return (this.___value == null ? null : this.___value.asNode());
    }

    @Override
    public Node getOriginalNode()
    {
        return (this.___value == null ? null : this.___value.asNode());
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___value == null)
        {
            return null;
        }
        return this;
    }
}
