package giraaff.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "InputType.Guard"
// @class ConditionAnchorNode
public final class ConditionAnchorNode extends FixedWithNextNode implements Canonicalizable.Unary<Node>, Lowerable, GuardingNode
{
    // @def
    public static final NodeClass<ConditionAnchorNode> TYPE = NodeClass.create(ConditionAnchorNode.class);

    @Node.Input(InputType.ConditionI)
    // @field
    LogicNode ___condition;
    // @field
    protected boolean ___negated;

    // @cons ConditionAnchorNode
    public ConditionAnchorNode(LogicNode __condition)
    {
        this(__condition, false);
    }

    // @cons ConditionAnchorNode
    public ConditionAnchorNode(LogicNode __condition, boolean __negated)
    {
        super(TYPE, StampFactory.forVoid());
        this.___negated = __negated;
        this.___condition = __condition;
    }

    public LogicNode condition()
    {
        return this.___condition;
    }

    public boolean isNegated()
    {
        return this.___negated;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, Node __forValue)
    {
        if (__forValue instanceof LogicNegationNode)
        {
            LogicNegationNode __negation = (LogicNegationNode) __forValue;
            return new ConditionAnchorNode(__negation.getValue(), !this.___negated);
        }
        if (__forValue instanceof LogicConstantNode)
        {
            LogicConstantNode __c = (LogicConstantNode) __forValue;
            if (__c.getValue() != this.___negated)
            {
                return null;
            }
            else
            {
                return new ValueAnchorNode(null);
            }
        }
        if (__tool.allUsagesAvailable() && this.hasNoUsages())
        {
            return null;
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            ValueAnchorNode __newAnchor = graph().add(new ValueAnchorNode(null));
            graph().replaceFixedWithFixed(this, __newAnchor);
        }
    }

    @Override
    public Node getValue()
    {
        return this.___condition;
    }
}
