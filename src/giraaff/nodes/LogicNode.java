package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.IndirectCanonicalization;
import giraaff.graph.NodeClass;
import giraaff.nodes.calc.FloatingNode;

// @NodeInfo.allowedUsageTypes "Condition"
// @class LogicNode
public abstract class LogicNode extends FloatingNode implements IndirectCanonicalization
{
    // @def
    public static final NodeClass<LogicNode> TYPE = NodeClass.create(LogicNode.class);

    // @cons
    public LogicNode(NodeClass<? extends LogicNode> __c)
    {
        super(__c, StampFactory.forVoid());
    }

    public static LogicNode and(LogicNode __a, LogicNode __b, double __shortCircuitProbability)
    {
        return and(__a, false, __b, false, __shortCircuitProbability);
    }

    public static LogicNode and(LogicNode __a, boolean __negateA, LogicNode __b, boolean __negateB, double __shortCircuitProbability)
    {
        StructuredGraph __graph = __a.graph();
        ShortCircuitOrNode __notAorNotB = __graph.unique(new ShortCircuitOrNode(__a, !__negateA, __b, !__negateB, __shortCircuitProbability));
        return __graph.unique(new LogicNegationNode(__notAorNotB));
    }

    public static LogicNode or(LogicNode __a, LogicNode __b, double __shortCircuitProbability)
    {
        return or(__a, false, __b, false, __shortCircuitProbability);
    }

    public static LogicNode or(LogicNode __a, boolean __negateA, LogicNode __b, boolean __negateB, double __shortCircuitProbability)
    {
        return __a.graph().unique(new ShortCircuitOrNode(__a, __negateA, __b, __negateB, __shortCircuitProbability));
    }

    public final boolean isTautology()
    {
        if (this instanceof LogicConstantNode)
        {
            LogicConstantNode __logicConstantNode = (LogicConstantNode) this;
            return __logicConstantNode.getValue();
        }

        return false;
    }

    public final boolean isContradiction()
    {
        if (this instanceof LogicConstantNode)
        {
            LogicConstantNode __logicConstantNode = (LogicConstantNode) this;
            return !__logicConstantNode.getValue();
        }

        return false;
    }
}
