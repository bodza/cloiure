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
    GuardingNode value;

    // @cons
    public GuardProxyNode(GuardingNode __value, LoopExitNode __proxyPoint)
    {
        super(TYPE, StampFactory.forVoid(), __proxyPoint);
        this.value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    public void setValue(GuardingNode __newValue)
    {
        this.updateUsages(value.asNode(), __newValue.asNode());
        this.value = __newValue;
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
    public Node canonical(CanonicalizerTool __tool)
    {
        if (value == null)
        {
            return null;
        }
        return this;
    }
}
