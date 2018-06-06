package giraaff.nodes;

import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;

// @NodeInfo.allowedUsageTypes "InputType.Association"
// @class LoopExitNode
public final class LoopExitNode extends BeginStateSplitNode implements IterableNodeType, Simplifiable
{
    // @def
    public static final NodeClass<LoopExitNode> TYPE = NodeClass.create(LoopExitNode.class);

    // The declared type of the field cannot be LoopBeginNode, because loop explosion during partial
    // evaluation can temporarily assign a non-loop begin. This node will then be deleted shortly
    // after - but we still must not have type system violations for that short amount of time.
    @Node.Input(InputType.Association)
    // @field
    AbstractBeginNode ___loopBegin;

    // @cons LoopExitNode
    public LoopExitNode(LoopBeginNode __loop)
    {
        super(TYPE);
        this.___loopBegin = __loop;
    }

    public LoopBeginNode loopBegin()
    {
        return (LoopBeginNode) this.___loopBegin;
    }

    @Override
    public NodeIterable<Node> anchored()
    {
        return super.anchored().filter(__n ->
        {
            if (__n instanceof ProxyNode)
            {
                ProxyNode __proxyNode = (ProxyNode) __n;
                return __proxyNode.proxyPoint() != this;
            }
            return true;
        });
    }

    @Override
    public void prepareDelete(FixedNode __evacuateFrom)
    {
        removeProxies();
        super.prepareDelete(__evacuateFrom);
    }

    public void removeProxies()
    {
        if (this.hasUsages())
        {
            outer: while (true)
            {
                for (ProxyNode __vpn : proxies().snapshot())
                {
                    ValueNode __value = __vpn.value();
                    __vpn.replaceAtUsagesAndDelete(__value);
                    if (__value == this)
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
        return (NodeIterable) usages().filter(__n ->
        {
            if (__n instanceof ProxyNode)
            {
                ProxyNode __proxyNode = (ProxyNode) __n;
                return __proxyNode.proxyPoint() == this;
            }
            return false;
        });
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        Node __prev = this.predecessor();
        while (__tool.allUsagesAvailable() && __prev instanceof BeginNode && __prev.hasNoUsages())
        {
            AbstractBeginNode __begin = (AbstractBeginNode) __prev;
            __prev = __prev.predecessor();
            graph().removeFixed(__begin);
        }
    }
}
