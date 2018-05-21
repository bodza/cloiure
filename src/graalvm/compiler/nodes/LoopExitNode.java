package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;

public final class LoopExitNode extends BeginStateSplitNode implements IterableNodeType, Simplifiable
{
    public static final NodeClass<LoopExitNode> TYPE = NodeClass.create(LoopExitNode.class);

    /*
     * The declared type of the field cannot be LoopBeginNode, because loop explosion during partial
     * evaluation can temporarily assign a non-loop begin. This node will then be deleted shortly
     * after - but we still must not have type system violations for that short amount of time.
     */
    @Input(Association) AbstractBeginNode loopBegin;

    public LoopExitNode(LoopBeginNode loop)
    {
        super(TYPE);
        loopBegin = loop;
    }

    public LoopBeginNode loopBegin()
    {
        return (LoopBeginNode) loopBegin;
    }

    @Override
    public NodeIterable<Node> anchored()
    {
        return super.anchored().filter(n ->
        {
            if (n instanceof ProxyNode)
            {
                ProxyNode proxyNode = (ProxyNode) n;
                return proxyNode.proxyPoint() != this;
            }
            return true;
        });
    }

    @Override
    public void prepareDelete(FixedNode evacuateFrom)
    {
        removeProxies();
        super.prepareDelete(evacuateFrom);
    }

    public void removeProxies()
    {
        if (this.hasUsages())
        {
            outer: while (true)
            {
                for (ProxyNode vpn : proxies().snapshot())
                {
                    ValueNode value = vpn.value();
                    vpn.replaceAtUsagesAndDelete(value);
                    if (value == this)
                    {
                        // Guard proxy could have this input as value.
                        continue outer;
                    }
                }
                break;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public NodeIterable<ProxyNode> proxies()
    {
        return (NodeIterable) usages().filter(n ->
        {
            if (n instanceof ProxyNode)
            {
                ProxyNode proxyNode = (ProxyNode) n;
                return proxyNode.proxyPoint() == this;
            }
            return false;
        });
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        Node prev = this.predecessor();
        while (tool.allUsagesAvailable() && prev instanceof BeginNode && prev.hasNoUsages())
        {
            AbstractBeginNode begin = (AbstractBeginNode) prev;
            prev = prev.predecessor();
            graph().removeFixed(begin);
        }
    }
}
