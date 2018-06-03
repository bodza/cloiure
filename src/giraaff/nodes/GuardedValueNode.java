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
    // @def
    public static final NodeClass<GuardedValueNode> TYPE = NodeClass.create(GuardedValueNode.class);

    @Input
    // @field
    ValueNode object;

    // @cons
    public GuardedValueNode(ValueNode __object, GuardingNode __guard)
    {
        super(TYPE, __object.stamp(NodeView.DEFAULT), __guard);
        this.object = __object;
    }

    public ValueNode object()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        if (object.getStackKind() != JavaKind.Void && object.getStackKind() != JavaKind.Illegal)
        {
            __gen.setResult(this, __gen.operand(object));
        }
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(object().stamp(NodeView.DEFAULT));
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            __tool.replaceWithVirtual((VirtualObjectNode) __alias);
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
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
