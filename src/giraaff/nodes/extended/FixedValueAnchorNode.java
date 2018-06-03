package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.ValueProxy;

// @class FixedValueAnchorNode
public class FixedValueAnchorNode extends FixedWithNextNode implements LIRLowerable, ValueProxy, GuardingNode
{
    // @def
    public static final NodeClass<FixedValueAnchorNode> TYPE = NodeClass.create(FixedValueAnchorNode.class);

    @Input
    // @field
    ValueNode object;
    // @field
    private Stamp predefinedStamp;

    public ValueNode object()
    {
        return object;
    }

    // @cons
    protected FixedValueAnchorNode(NodeClass<? extends FixedValueAnchorNode> __c, ValueNode __object)
    {
        super(__c, __object.stamp(NodeView.DEFAULT));
        this.object = __object;
    }

    // @cons
    public FixedValueAnchorNode(ValueNode __object)
    {
        this(TYPE, __object);
    }

    // @cons
    public FixedValueAnchorNode(ValueNode __object, Stamp __predefinedStamp)
    {
        super(TYPE, __predefinedStamp);
        this.object = __object;
        this.predefinedStamp = __predefinedStamp;
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
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.operand(object));
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
