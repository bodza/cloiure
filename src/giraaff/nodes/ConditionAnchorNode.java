package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public final class ConditionAnchorNode extends FixedWithNextNode implements Canonicalizable.Unary<Node>, Lowerable, GuardingNode
{
    public static final NodeClass<ConditionAnchorNode> TYPE = NodeClass.create(ConditionAnchorNode.class);
    @Input(InputType.Condition) LogicNode condition;
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
