package giraaff.nodes.extended;

import giraaff.graph.Node;
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
import giraaff.util.GraalError;

///
// Instances of this node class will look for a preceding if node and put the given probability into
// the if node's taken probability. Then the branch probability node will be removed. This node is
// intended primarily for snippets, so that they can define their fast and slow paths.
///
// @class BranchProbabilityNode
public final class BranchProbabilityNode extends FloatingNode implements Simplifiable, Lowerable
{
    // @def
    public static final NodeClass<BranchProbabilityNode> TYPE = NodeClass.create(BranchProbabilityNode.class);

    // @def
    public static final double LIKELY_PROBABILITY = 0.6;
    // @def
    public static final double NOT_LIKELY_PROBABILITY = 1 - LIKELY_PROBABILITY;

    // @def
    public static final double FREQUENT_PROBABILITY = 0.9;
    // @def
    public static final double NOT_FREQUENT_PROBABILITY = 1 - FREQUENT_PROBABILITY;

    // @def
    public static final double FAST_PATH_PROBABILITY = 0.99;
    // @def
    public static final double SLOW_PATH_PROBABILITY = 1 - FAST_PATH_PROBABILITY;

    // @def
    public static final double VERY_FAST_PATH_PROBABILITY = 0.999;
    // @def
    public static final double VERY_SLOW_PATH_PROBABILITY = 1 - VERY_FAST_PATH_PROBABILITY;

    @Node.Input
    // @field
    ValueNode ___probability;
    @Node.Input
    // @field
    ValueNode ___condition;

    // @cons BranchProbabilityNode
    public BranchProbabilityNode(ValueNode __probability, ValueNode __condition)
    {
        super(TYPE, __condition.stamp(NodeView.DEFAULT));
        this.___probability = __probability;
        this.___condition = __condition;
    }

    public ValueNode getProbability()
    {
        return this.___probability;
    }

    public ValueNode getCondition()
    {
        return this.___condition;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        if (!hasUsages())
        {
            return;
        }
        if (this.___probability.isConstant())
        {
            double __probabilityValue = this.___probability.asJavaConstant().asDouble();
            if (__probabilityValue < 0.0)
            {
                throw new GraalError("A negative probability of " + __probabilityValue + " is not allowed!");
            }
            else if (__probabilityValue > 1.0)
            {
                throw new GraalError("A probability of more than 1.0 (" + __probabilityValue + ") is not allowed!");
            }
            else if (Double.isNaN(__probabilityValue))
            {
                // We allow NaN if the node is in unreachable code that will eventually fall away,
                // or else an error will be thrown during lowering since we keep the node around.
                return;
            }
            boolean __usageFound = false;
            for (IntegerEqualsNode __node : this.usages().filter(IntegerEqualsNode.class))
            {
                ValueNode __other = __node.getX();
                if (__node.getX() == this)
                {
                    __other = __node.getY();
                }
                if (__other.isConstant())
                {
                    double __probabilityToSet = __probabilityValue;
                    if (__other.asJavaConstant().asInt() == 0)
                    {
                        __probabilityToSet = 1.0 - __probabilityToSet;
                    }
                    for (IfNode __ifNodeUsages : __node.usages().filter(IfNode.class))
                    {
                        __usageFound = true;
                        __ifNodeUsages.setTrueSuccessorProbability(__probabilityToSet);
                    }
                    if (!__usageFound)
                    {
                        __usageFound = __node.usages().filter(NodePredicates.isA(FixedGuardNode.class).or(ConditionalNode.class)).isNotEmpty();
                    }
                }
            }
            if (__usageFound)
            {
                ValueNode __currentCondition = this.___condition;
                replaceAndDelete(__currentCondition);
                if (__tool != null)
                {
                    __tool.addToWorkList(__currentCondition.usages());
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

    ///
    // This intrinsic should only be used for the condition of an if statement. The parameter
    // condition should also only denote a simple condition and not a combined condition involving
    // && or || operators. It injects the probability of the condition into the if statement.
    //
    // @param probability the probability that the given condition is true as a double value between
    //            0.0 and 1.0.
    // @param condition the simple condition without any && or || operators
    // @return the condition
    ///
    @Node.NodeIntrinsic
    public static native boolean probability(double __probability, boolean __condition);

    @Override
    public void lower(LoweringTool __tool)
    {
        throw new GraalError("Branch probability could not be injected, because the probability value did not reduce to a constant value.");
    }
}
