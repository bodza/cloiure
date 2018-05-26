package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.ValueProxy;

public class FixedValueAnchorNode extends FixedWithNextNode implements LIRLowerable, ValueProxy, GuardingNode
{
    public static final NodeClass<FixedValueAnchorNode> TYPE = NodeClass.create(FixedValueAnchorNode.class);

    @Input ValueNode object;
    private Stamp predefinedStamp;

    public ValueNode object()
    {
        return object;
    }

    protected FixedValueAnchorNode(NodeClass<? extends FixedValueAnchorNode> c, ValueNode object)
    {
        super(c, object.stamp(NodeView.DEFAULT));
        this.object = object;
    }

    public FixedValueAnchorNode(ValueNode object)
    {
        this(TYPE, object);
    }

    public FixedValueAnchorNode(ValueNode object, Stamp predefinedStamp)
    {
        super(TYPE, predefinedStamp);
        this.object = object;
        this.predefinedStamp = predefinedStamp;
    }

    @Override
    public boolean inferStamp()
    {
        if (predefinedStamp == null)
        {
            return updateStamp(object.stamp(NodeView.DEFAULT));
        }
        else
        {
            return false;
        }
    }

    @NodeIntrinsic
    public static native Object getObject(Object object);

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.operand(object));
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return object;
    }

    @Override
    public GuardingNode getGuard()
    {
        return this;
    }
}
