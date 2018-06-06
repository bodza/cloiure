package giraaff.nodes;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class UnaryOpLogicNode
public abstract class UnaryOpLogicNode extends LogicNode implements LIRLowerable, Canonicalizable.Unary<ValueNode>
{
    // @def
    public static final NodeClass<UnaryOpLogicNode> TYPE = NodeClass.create(UnaryOpLogicNode.class);

    @Node.Input
    // @field
    protected ValueNode ___value;

    @Override
    public ValueNode getValue()
    {
        return this.___value;
    }

    // @cons UnaryOpLogicNode
    public UnaryOpLogicNode(NodeClass<? extends UnaryOpLogicNode> __c, ValueNode __value)
    {
        super(__c);
        this.___value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
    }

    public Stamp getSucceedingStampForValue(boolean __negated, Stamp __valueStamp)
    {
        Stamp __succStamp = getSucceedingStampForValue(__negated);
        if (__succStamp != null)
        {
            __succStamp = __succStamp.join(__valueStamp);
        }
        return __succStamp;
    }

    public abstract Stamp getSucceedingStampForValue(boolean __negated);

    public abstract TriState tryFold(Stamp __valueStamp);
}
