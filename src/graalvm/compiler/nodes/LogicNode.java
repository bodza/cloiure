package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Condition;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node.IndirectCanonicalization;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;

@NodeInfo(allowedUsageTypes = {Condition}, size = SIZE_1)
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
