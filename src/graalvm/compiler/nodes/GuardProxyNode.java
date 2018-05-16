package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.Proxy;

@NodeInfo(allowedUsageTypes = {InputType.Guard}, nameTemplate = "Proxy({i#value})")
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
    public void generate(NodeLIRBuilderTool generator)
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
