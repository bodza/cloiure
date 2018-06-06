package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class StoreHubNode
public final class StoreHubNode extends FixedWithNextNode implements Lowerable
{
    // @def
    public static final NodeClass<StoreHubNode> TYPE = NodeClass.create(StoreHubNode.class);

    @Node.Input
    // @field
    ValueNode ___value;
    @Node.Input
    // @field
    ValueNode ___object;

    public ValueNode getValue()
    {
        return this.___value;
    }

    public ValueNode getObject()
    {
        return this.___object;
    }

    // @cons StoreHubNode
    public StoreHubNode(ValueNode __object, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.___value = __value;
        this.___object = __object;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Node.NodeIntrinsic
    public static native void write(Object __object, Object __value);
}
