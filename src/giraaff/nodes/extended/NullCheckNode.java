package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "InputType.Guard"
// @class NullCheckNode
public final class NullCheckNode extends DeoptimizingFixedWithNextNode implements LIRLowerable, GuardingNode
{
    // @def
    public static final NodeClass<NullCheckNode> TYPE = NodeClass.create(NullCheckNode.class);

    @Node.Input
    // @field
    ValueNode ___object;

    // @cons NullCheckNode
    public NullCheckNode(ValueNode __object)
    {
        super(TYPE, StampFactory.forVoid());
        this.___object = __object;
    }

    public ValueNode getObject()
    {
        return this.___object;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitNullCheck(__gen.operand(this.___object), __gen.state(this));
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Node.NodeIntrinsic
    public static native void nullCheck(Object __object);
}
