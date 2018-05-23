package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.IndirectCanonicalization;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.FloatingNode;

// NodeInfo.allowedUsageTypes = Condition
public abstract class LogicNode extends FloatingNode implements IndirectCanonicalization
{
    public static final NodeClass<LogicNode> TYPE = NodeClass.create(LogicNode.class);

    public LogicNode(NodeClass<? extends LogicNode> c)
    {
        super(c, StampFactory.forVoid());
    }

    public static LogicNode and(LogicNode a, LogicNode b, double shortCircuitProbability)
    {
        return and(a, false, b, false, shortCircuitProbability);
    }

    public static LogicNode and(LogicNode a, boolean negateA, LogicNode b, boolean negateB, double shortCircuitProbability)
    {
        StructuredGraph graph = a.graph();
        ShortCircuitOrNode notAorNotB = graph.unique(new ShortCircuitOrNode(a, !negateA, b, !negateB, shortCircuitProbability));
        return graph.unique(new LogicNegationNode(notAorNotB));
    }

    public static LogicNode or(LogicNode a, LogicNode b, double shortCircuitProbability)
    {
        return or(a, false, b, false, shortCircuitProbability);
    }

    public static LogicNode or(LogicNode a, boolean negateA, LogicNode b, boolean negateB, double shortCircuitProbability)
    {
        return a.graph().unique(new ShortCircuitOrNode(a, negateA, b, negateB, shortCircuitProbability));
    }

    public final boolean isTautology()
    {
        if (this instanceof LogicConstantNode)
        {
            LogicConstantNode logicConstantNode = (LogicConstantNode) this;
            return logicConstantNode.getValue();
        }

        return false;
    }

    public final boolean isContradiction()
    {
        if (this instanceof LogicConstantNode)
        {
            LogicConstantNode logicConstantNode = (LogicConstantNode) this;
            return !logicConstantNode.getValue();
        }

        return false;
    }
}
