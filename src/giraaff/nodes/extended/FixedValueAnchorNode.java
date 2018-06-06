package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
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

    @Node.Input
    // @field
    ValueNode ___object;
    // @field
    private Stamp ___predefinedStamp;

    public ValueNode object()
    {
        return this.___object;
    }

    // @cons FixedValueAnchorNode
    protected FixedValueAnchorNode(NodeClass<? extends FixedValueAnchorNode> __c, ValueNode __object)
    {
        super(__c, __object.stamp(NodeView.DEFAULT));
        this.___object = __object;
    }

    // @cons FixedValueAnchorNode
    public FixedValueAnchorNode(ValueNode __object)
    {
        this(TYPE, __object);
    }

    // @cons FixedValueAnchorNode
    public FixedValueAnchorNode(ValueNode __object, Stamp __predefinedStamp)
    {
        super(TYPE, __predefinedStamp);
        this.___object = __object;
        this.___predefinedStamp = __predefinedStamp;
    }

    @Override
    public boolean inferStamp()
    {
        if (this.___predefinedStamp == null)
        {
            return updateStamp(this.___object.stamp(NodeView.DEFAULT));
        }
        else
        {
            return false;
        }
    }

    @Node.NodeIntrinsic
    public static native Object getObject(Object __object);

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.operand(this.___object));
    }

    @Override
    public ValueNode getOriginalNode()
    {
        return this.___object;
    }

    @Override
    public GuardingNode getGuard()
    {
        return this;
    }
}
