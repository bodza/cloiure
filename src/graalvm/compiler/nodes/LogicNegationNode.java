package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Condition;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;

/**
 * Logic node that negates its argument.
 */
public final class LogicNegationNode extends LogicNode implements Canonicalizable.Unary<LogicNode>
{
    public static final NodeClass<LogicNegationNode> TYPE = NodeClass.create(LogicNegationNode.class);
    @Input(Condition) LogicNode value;

    public LogicNegationNode(LogicNode value)
    {
        super(TYPE);
        this.value = value;
    }

    public static LogicNode create(LogicNode value)
    {
        LogicNode synonym = findSynonym(value);
        if (synonym != null)
        {
            return synonym;
        }
        return new LogicNegationNode(value);
    }

    private static LogicNode findSynonym(LogicNode value)
    {
        if (value instanceof LogicConstantNode)
        {
            LogicConstantNode logicConstantNode = (LogicConstantNode) value;
            return LogicConstantNode.forBoolean(!logicConstantNode.getValue());
        }
        else if (value instanceof LogicNegationNode)
        {
            return ((LogicNegationNode) value).getValue();
        }
        return null;
    }

    @Override
    public LogicNode getValue()
    {
        return value;
    }

    @Override
    public LogicNode canonical(CanonicalizerTool tool, LogicNode forValue)
    {
        LogicNode synonym = findSynonym(forValue);
        if (synonym != null)
        {
            return synonym;
        }
        return this;
    }
}
