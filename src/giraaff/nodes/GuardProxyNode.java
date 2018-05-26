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
public final class GuardProxyNode extends ProxyNode implements GuardingNode, Proxy, LIRLowerable, Canonicalizable
{
    public static final NodeClass<GuardProxyNode> TYPE = NodeClass.create(GuardProxyNode.class);
    @OptionalInput(InputType.Guard) GuardingNode value;

    public GuardProxyNode(GuardingNode value, LoopExitNode proxyPoint)
    {
        super(TYPE, StampFactory.forVoid(), proxyPoint);
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
    }

    public void setValue(GuardingNode newValue)
    {
        this.updateUsages(value.asNode(), newValue.asNode());
        this.value = newValue;
    }

    @Override
    public ValueNode value()
    {
        return (value == null ? null : value.asNode());
    }

    @Override
    public Node getOriginalNode()
    {
        return (value == null ? null : value.asNode());
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (value == null)
        {
            return null;
        }
        return this;
    }
}
