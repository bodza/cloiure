package graalvm.compiler.nodes;

import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * The {@code LogicConstantNode} represents a boolean constant.
 */
public final class LogicConstantNode extends LogicNode implements LIRLowerable
{
    public static final NodeClass<LogicConstantNode> TYPE = NodeClass.create(LogicConstantNode.class);
    protected final boolean value;

    public LogicConstantNode(boolean value)
    {
        super(TYPE);
        this.value = value;
    }

    /**
     * Returns a node for a boolean constant.
     *
     * @param v the boolean value for which to create the instruction
     * @return a node representing the boolean
     */
    public static LogicConstantNode forBoolean(boolean v, Graph graph)
    {
        return graph.unique(new LogicConstantNode(v));
    }

    /**
     * Returns a node for a boolean constant.
     *
     * @param v the boolean value for which to create the instruction
     * @return a node representing the boolean
     */
    public static LogicConstantNode forBoolean(boolean v)
    {
        return new LogicConstantNode(v);
    }

    /**
     * Gets a constant for {@code true}.
     */
    public static LogicConstantNode tautology(Graph graph)
    {
        return forBoolean(true, graph);
    }

    /**
     * Gets a constant for {@code false}.
     */
    public static LogicConstantNode contradiction(Graph graph)
    {
        return forBoolean(false, graph);
    }

    /**
     * Gets a constant for {@code true}.
     */
    public static LogicConstantNode tautology()
    {
        return forBoolean(true);
    }

    /**
     * Gets a constant for {@code false}.
     */
    public static LogicConstantNode contradiction()
    {
        return forBoolean(false);
    }

    public boolean getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        // nothing to do
    }
}
