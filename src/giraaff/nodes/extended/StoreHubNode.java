package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
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

    @Input
    // @field
    ValueNode value;
    @Input
    // @field
    ValueNode object;

    public ValueNode getValue()
    {
        return value;
    }

    public ValueNode getObject()
    {
        return object;
    }

    // @cons
    public StoreHubNode(ValueNode __object, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = __value;
        this.object = __object;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @NodeIntrinsic
    public static native void write(Object object, Object value);
}
