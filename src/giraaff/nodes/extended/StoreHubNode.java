package giraaff.nodes.extended;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public final class StoreHubNode extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<StoreHubNode> TYPE = NodeClass.create(StoreHubNode.class);
    @Input ValueNode value;
    @Input ValueNode object;

    public ValueNode getValue()
    {
        return value;
    }

    public ValueNode getObject()
    {
        return object;
    }

    public StoreHubNode(ValueNode object, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
        this.object = object;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @NodeIntrinsic
    public static native void write(Object object, Object value);
}
