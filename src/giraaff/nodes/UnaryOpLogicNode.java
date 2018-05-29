package giraaff.nodes;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class UnaryOpLogicNode
public abstract class UnaryOpLogicNode extends LogicNode implements LIRLowerable, Canonicalizable.Unary<ValueNode>
{
    public static final NodeClass<UnaryOpLogicNode> TYPE = NodeClass.create(UnaryOpLogicNode.class);

    @Input protected ValueNode value;

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    // @cons
    public UnaryOpLogicNode(NodeClass<? extends UnaryOpLogicNode> c, ValueNode value)
    {
        super(c);
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
    }

    public Stamp getSucceedingStampForValue(boolean negated, Stamp valueStamp)
    {
        Stamp succStamp = getSucceedingStampForValue(negated);
        if (succStamp != null)
        {
            succStamp = succStamp.join(valueStamp);
        }
        return succStamp;
    }

    public abstract Stamp getSucceedingStampForValue(boolean negated);

    public abstract TriState tryFold(Stamp valueStamp);
}
