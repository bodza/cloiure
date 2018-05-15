package graalvm.compiler.nodes;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.spi.ValueProxy;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

@NodeInfo(nameTemplate = "Proxy({i#value})")
public final class ValueProxyNode extends ProxyNode implements Canonicalizable, Virtualizable, ValueProxy {

    public static final NodeClass<ValueProxyNode> TYPE = NodeClass.create(ValueProxyNode.class);
    @Input ValueNode value;
    private final boolean loopPhiProxy;

    public ValueProxyNode(ValueNode value, LoopExitNode loopExit) {
        super(TYPE, value.stamp(NodeView.DEFAULT), loopExit);
        this.value = value;
        loopPhiProxy = loopExit.loopBegin().isPhiAtMerge(value);
    }

    @Override
    public ValueNode value() {
        return value;
    }

    @Override
    public boolean inferStamp() {
        return updateStamp(value.stamp(NodeView.DEFAULT));
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        ValueNode curValue = value;
        if (curValue.isConstant()) {
            return curValue;
        }
        if (loopPhiProxy && !loopExit.loopBegin().isPhiAtMerge(curValue)) {
            return curValue;
        }
        return this;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(value);
        if (alias instanceof VirtualObjectNode) {
            tool.replaceWithVirtual((VirtualObjectNode) alias);
        }
    }

    @Override
    public ValueNode getOriginalNode() {
        return value();
    }

    @Override
    public GuardingNode getGuard() {
        return this.proxyPoint();
    }
}
