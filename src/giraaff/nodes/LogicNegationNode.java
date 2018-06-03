package giraaff.nodes;

import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;

///
// Logic node that negates its argument.
///
// @class LogicNegationNode
public final class LogicNegationNode extends LogicNode implements Canonicalizable.Unary<LogicNode>
{
    // @def
    public static final NodeClass<LogicNegationNode> TYPE = NodeClass.create(LogicNegationNode.class);

    @Input(InputType.Condition)
    // @field
    LogicNode ___value;

    // @cons
    public LogicNegationNode(LogicNode __value)
    {
        super(TYPE);
        this.___value = __value;
    }

    public static LogicNode create(LogicNode __value)
    {
        LogicNode __synonym = findSynonym(__value);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new LogicNegationNode(__value);
    }

    private static LogicNode findSynonym(LogicNode __value)
    {
        if (__value instanceof LogicConstantNode)
        {
            LogicConstantNode __logicConstantNode = (LogicConstantNode) __value;
            return LogicConstantNode.forBoolean(!__logicConstantNode.getValue());
        }
        else if (__value instanceof LogicNegationNode)
        {
            return ((LogicNegationNode) __value).getValue();
        }
        return null;
    }

    @Override
    public LogicNode getValue()
    {
        return this.___value;
    }

    @Override
    public LogicNode canonical(CanonicalizerTool __tool, LogicNode __forValue)
    {
        LogicNode __synonym = findSynonym(__forValue);
        if (__synonym != null)
        {
            return __synonym;
        }
        return this;
    }
}
