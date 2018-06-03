package giraaff.nodes;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.ValueProxy;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class ValueProxyNode
public final class ValueProxyNode extends ProxyNode implements Canonicalizable, Virtualizable, ValueProxy
{
    // @def
    public static final NodeClass<ValueProxyNode> TYPE = NodeClass.create(ValueProxyNode.class);

    @Input
    // @field
    ValueNode value;
    // @field
    private final boolean loopPhiProxy;

    // @cons
    public ValueProxyNode(ValueNode __value, LoopExitNode __loopExit)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT), __loopExit);
        this.value = __value;
        loopPhiProxy = __loopExit.loopBegin().isPhiAtMerge(__value);
    }

    @Override
    public ValueNode value()
    {
        return value;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(value.stamp(NodeView.DEFAULT));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        ValueNode __curValue = value;
        if (__curValue.isConstant())
        {
            return __curValue;
        }
        if (loopPhiProxy && !loopExit.loopBegin().isPhiAtMerge(__curValue))
        {
            return __curValue;
        }
        return this;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(value);
        if (__alias instanceof VirtualObjectNode)
        {
            __tool.replaceWithVirtual((VirtualObjectNode) __alias);
        }
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return value();
    }

    @Override
    public GuardingNode getGuard()
    {
        return this.proxyPoint();
    }
}
