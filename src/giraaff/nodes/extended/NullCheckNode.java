package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class NullCheckNode extends DeoptimizingFixedWithNextNode implements LIRLowerable, GuardingNode
{
    public static final NodeClass<NullCheckNode> TYPE = NodeClass.create(NullCheckNode.class);
    @Input ValueNode object;

    public NullCheckNode(ValueNode object)
    {
        super(TYPE, StampFactory.forVoid());
        this.object = object;
    }

    public ValueNode getObject()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.getLIRGeneratorTool().emitNullCheck(generator.operand(object), generator.state(this));
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @NodeIntrinsic
    public static native void nullCheck(Object object);
}
