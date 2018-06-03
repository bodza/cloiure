package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "Guard"
// @class NullCheckNode
public final class NullCheckNode extends DeoptimizingFixedWithNextNode implements LIRLowerable, GuardingNode
{
    // @def
    public static final NodeClass<NullCheckNode> TYPE = NodeClass.create(NullCheckNode.class);

    @Input
    // @field
    ValueNode object;

    // @cons
    public NullCheckNode(ValueNode __object)
    {
        super(TYPE, StampFactory.forVoid());
        this.object = __object;
    }

    public ValueNode getObject()
    {
        return object;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitNullCheck(__gen.operand(object), __gen.state(this));
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @NodeIntrinsic
    public static native void nullCheck(Object object);
}
