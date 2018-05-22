package graalvm.compiler.nodes;

import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.ValueProxy;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

/**
 * A node that changes the type of its input, usually narrowing it. For example, a GuardedValueNode
 * is used to keep the nodes depending on guards inside a loop during speculative guard movement.
 *
 * A GuardedValueNode will only go away if its guard is null or {@link StructuredGraph#start()}.
 */
public final class GuardedValueNode extends FloatingGuardedNode implements LIRLowerable, Virtualizable, Canonicalizable, ValueProxy
{
    public static final NodeClass<GuardedValueNode> TYPE = NodeClass.create(GuardedValueNode.class);
    @Input ValueNode object;

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
    public void generate(NodeLIRBuilderTool generator)
    {
        if (object.getStackKind() != JavaKind.Void && object.getStackKind() != JavaKind.Illegal)
        {
            generator.setResult(this, generator.operand(object));
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
