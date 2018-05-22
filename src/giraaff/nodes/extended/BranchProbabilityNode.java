package giraaff.nodes.extended;

import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodePredicates;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * Instances of this node class will look for a preceding if node and put the given probability into
 * the if node's taken probability. Then the branch probability node will be removed. This node is
 * intended primarily for snippets, so that they can define their fast and slow paths.
 */
public final class BranchProbabilityNode extends FloatingNode implements Simplifiable, Lowerable
{
    public static final NodeClass<BranchProbabilityNode> TYPE = NodeClass.create(BranchProbabilityNode.class);
    public static final double LIKELY_PROBABILITY = 0.6;
    public static final double NOT_LIKELY_PROBABILITY = 1 - LIKELY_PROBABILITY;

    public static final double FREQUENT_PROBABILITY = 0.9;
    public static final double NOT_FREQUENT_PROBABILITY = 1 - FREQUENT_PROBABILITY;

    public static final double FAST_PATH_PROBABILITY = 0.99;
    public static final double SLOW_PATH_PROBABILITY = 1 - FAST_PATH_PROBABILITY;

    public static final double VERY_FAST_PATH_PROBABILITY = 0.999;
    public static final double VERY_SLOW_PATH_PROBABILITY = 1 - VERY_FAST_PATH_PROBABILITY;

    @Input ValueNode probability;
    @Input ValueNode condition;

    public BranchProbabilityNode(ValueNode probability, ValueNode condition)
    {
        super(TYPE, condition.stamp(NodeView.DEFAULT));
        this.probability = probability;
        this.condition = condition;
    }

    public ValueNode getProbability()
    {
        return probability;
    }

    public ValueNode getCondition()
    {
        return condition;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        if (!hasUsages())
        {
            return;
        }
        if (probability.isConstant())
        {
            double probabilityValue = probability.asJavaConstant().asDouble();
            if (probabilityValue < 0.0)
            {
                throw new GraalError("A negative probability of " + probabilityValue + " is not allowed!");
            }
            else if (probabilityValue > 1.0)
            {
                throw new GraalError("A probability of more than 1.0 (" + probabilityValue + ") is not allowed!");
            }
            else if (Double.isNaN(probabilityValue))
            {
                /*
                 * We allow NaN if the node is in unreachable code that will eventually fall away,
                 * or else an error will be thrown during lowering since we keep the node around.
                 */
                return;
            }
            boolean usageFound = false;
            for (IntegerEqualsNode node : this.usages().filter(IntegerEqualsNode.class))
            {
                ValueNode other = node.getX();
                if (node.getX() == this)
                {
                    other = node.getY();
                }
                if (other.isConstant())
                {
                    double probabilityToSet = probabilityValue;
                    if (other.asJavaConstant().asInt() == 0)
                    {
                        probabilityToSet = 1.0 - probabilityToSet;
                    }
                    for (IfNode ifNodeUsages : node.usages().filter(IfNode.class))
                    {
                        usageFound = true;
                        ifNodeUsages.setTrueSuccessorProbability(probabilityToSet);
                    }
                    if (!usageFound)
                    {
                        usageFound = node.usages().filter(NodePredicates.isA(FixedGuardNode.class).or(ConditionalNode.class)).isNotEmpty();
                    }
                }
            }
            if (usageFound)
            {
                ValueNode currentCondition = condition;
                replaceAndDelete(currentCondition);
                if (tool != null)
                {
                    tool.addToWorkList(currentCondition.usages());
                }
            }
            else
            {
                if (!isSubstitutionGraph())
                {
                    throw new GraalError("Wrong usage of branch probability injection!");
                }
            }
        }
    }

    private boolean isSubstitutionGraph()
    {
        return hasExactlyOneUsage() && usages().first() instanceof ReturnNode;
    }

    /**
     * This intrinsic should only be used for the condition of an if statement. The parameter
     * condition should also only denote a simple condition and not a combined condition involving
     * &amp;&amp; or || operators. It injects the probability of the condition into the if
     * statement.
     *
     * @param probability the probability that the given condition is true as a double value between
     *            0.0 and 1.0.
     * @param condition the simple condition without any &amp;&amp; or || operators
     * @return the condition
     */
    @NodeIntrinsic
    public static native boolean probability(double probability, boolean condition);

    @Override
    public void lower(LoweringTool tool)
    {
        throw new GraalError("Branch probability could not be injected, because the probability value did not reduce to a constant value.");
    }
}
