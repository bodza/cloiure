package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.ValueProxy;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * A node that changes the type of its input, usually narrowing it. For example, a GuardedValueNode
 * is used to keep the nodes depending on guards inside a loop during speculative guard movement.
 *
 * A GuardedValueNode will only go away if its guard is null or {@link StructuredGraph#start()}.
 */
// @class GuardedValueNode
public final class GuardedValueNode extends FloatingGuardedNode implements LIRLowerable, Virtualizable, Canonicalizable, ValueProxy
{
    public static final NodeClass<GuardedValueNode> TYPE = NodeClass.create(GuardedValueNode.class);

    @Input ValueNode object;

    // @cons
    public GuardedValueNode(ValueNode object, GuardingNode guard)
    {
        super(TYPE, object.stamp(NodeView.DEFAULT), guard);
        this.object = object;
    }

    public ValueNode object()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        if (object.getStackKind() != JavaKind.Void && object.getStackKind() != JavaKind.Illegal)
        {
            gen.setResult(this, gen.operand(object));
        }
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(object().stamp(NodeView.DEFAULT));
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode)
        {
            tool.replaceWithVirtual((VirtualObjectNode) alias);
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (getGuard() == null)
        {
            if (stamp(NodeView.DEFAULT).equals(object().stamp(NodeView.DEFAULT)))
            {
                return object();
            }
            else
            {
                return PiNode.create(object(), stamp(NodeView.DEFAULT));
            }
        }
        return this;
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return object;
    }
}
