package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Condition;
import static graalvm.compiler.nodeinfo.InputType.Guard;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.extended.ValueAnchorNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

public final class ConditionAnchorNode extends FixedWithNextNode implements Canonicalizable.Unary<Node>, Lowerable, GuardingNode
{
    public static final NodeClass<ConditionAnchorNode> TYPE = NodeClass.create(ConditionAnchorNode.class);
    @Input(Condition) LogicNode condition;
    protected boolean negated;

    public ConditionAnchorNode(LogicNode condition)
    {
        this(condition, false);
    }

    public ConditionAnchorNode(LogicNode condition, boolean negated)
    {
        super(TYPE, StampFactory.forVoid());
        this.negated = negated;
        this.condition = condition;
    }

    public LogicNode condition()
    {
        return condition;
    }

    public boolean isNegated()
    {
        return negated;
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name && negated)
        {
            return "!" + super.toString(verbosity);
        }
        else
        {
            return super.toString(verbosity);
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool, Node forValue)
    {
        if (forValue instanceof LogicNegationNode)
        {
            LogicNegationNode negation = (LogicNegationNode) forValue;
            return new ConditionAnchorNode(negation.getValue(), !negated);
        }
        if (forValue instanceof LogicConstantNode)
        {
            LogicConstantNode c = (LogicConstantNode) forValue;
            if (c.getValue() != negated)
            {
                return null;
            }
            else
            {
                return new ValueAnchorNode(null);
            }
        }
        if (tool.allUsagesAvailable() && this.hasNoUsages())
        {
            return null;
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            ValueAnchorNode newAnchor = graph().add(new ValueAnchorNode(null));
            graph().replaceFixedWithFixed(this, newAnchor);
        }
    }

    @Override
    public Node getValue()
    {
        return condition;
    }
}
