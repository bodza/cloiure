package giraaff.nodes;

import giraaff.graph.Graph;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// The {@code LogicConstantNode} represents a boolean constant.
///
// @class LogicConstantNode
public final class LogicConstantNode extends LogicNode implements LIRLowerable
{
    // @def
    public static final NodeClass<LogicConstantNode> TYPE = NodeClass.create(LogicConstantNode.class);

    // @field
    protected final boolean ___value;

    // @cons LogicConstantNode
    public LogicConstantNode(boolean __value)
    {
        super(TYPE);
        this.___value = __value;
    }

    ///
    // Returns a node for a boolean constant.
    //
    // @param v the boolean value for which to create the instruction
    // @return a node representing the boolean
    ///
    public static LogicConstantNode forBoolean(boolean __v, Graph __graph)
    {
        return __graph.unique(new LogicConstantNode(__v));
    }

    ///
    // Returns a node for a boolean constant.
    //
    // @param v the boolean value for which to create the instruction
    // @return a node representing the boolean
    ///
    public static LogicConstantNode forBoolean(boolean __v)
    {
        return new LogicConstantNode(__v);
    }

    ///
    // Gets a constant for {@code true}.
    ///
    public static LogicConstantNode tautology(Graph __graph)
    {
        return forBoolean(true, __graph);
    }

    ///
    // Gets a constant for {@code false}.
    ///
    public static LogicConstantNode contradiction(Graph __graph)
    {
        return forBoolean(false, __graph);
    }

    ///
    // Gets a constant for {@code true}.
    ///
    public static LogicConstantNode tautology()
    {
        return forBoolean(true);
    }

    ///
    // Gets a constant for {@code false}.
    ///
    public static LogicConstantNode contradiction()
    {
        return forBoolean(false);
    }

    public boolean getValue()
    {
        return this.___value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nothing to do
    }
}
