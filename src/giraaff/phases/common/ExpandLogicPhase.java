package giraaff.phases.common;

import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ShortCircuitOrNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.FloatEqualsNode;
import giraaff.nodes.calc.FloatLessThanNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.NormalizeCompareNode;
import giraaff.phases.Phase;
import giraaff.util.GraalError;

// @class ExpandLogicPhase
public final class ExpandLogicPhase extends Phase
{
    // @def
    private static final double EPSILON = 1E-6;

    @Override
    protected void run(StructuredGraph __graph)
    {
        for (ShortCircuitOrNode __logic : __graph.getNodes(ShortCircuitOrNode.TYPE))
        {
            processBinary(__logic);
        }

        for (NormalizeCompareNode __logic : __graph.getNodes(NormalizeCompareNode.TYPE))
        {
            processNormalizeCompareNode(__logic);
        }
        __graph.setAfterExpandLogic();
    }

    private static void processNormalizeCompareNode(NormalizeCompareNode __normalize)
    {
        LogicNode __equalComp;
        LogicNode __lessComp;
        StructuredGraph __graph = __normalize.graph();
        ValueNode __x = __normalize.getX();
        ValueNode __y = __normalize.getY();
        if (__x.stamp(NodeView.DEFAULT) instanceof FloatStamp)
        {
            __equalComp = __graph.addOrUniqueWithInputs(FloatEqualsNode.create(__x, __y, NodeView.DEFAULT));
            __lessComp = __graph.addOrUniqueWithInputs(FloatLessThanNode.create(__x, __y, __normalize.isUnorderedLess(), NodeView.DEFAULT));
        }
        else
        {
            __equalComp = __graph.addOrUniqueWithInputs(IntegerEqualsNode.create(__x, __y, NodeView.DEFAULT));
            __lessComp = __graph.addOrUniqueWithInputs(IntegerLessThanNode.create(__x, __y, NodeView.DEFAULT));
        }

        Stamp __stamp = __normalize.stamp(NodeView.DEFAULT);
        ConditionalNode __equalValue = __graph.unique(new ConditionalNode(__equalComp, ConstantNode.forIntegerStamp(__stamp, 0, __graph), ConstantNode.forIntegerStamp(__stamp, 1, __graph)));
        ConditionalNode __value = __graph.unique(new ConditionalNode(__lessComp, ConstantNode.forIntegerStamp(__stamp, -1, __graph), __equalValue));
        __normalize.replaceAtUsagesAndDelete(__value);
    }

    private static void processBinary(ShortCircuitOrNode __binary)
    {
        while (__binary.usages().isNotEmpty())
        {
            Node __usage = __binary.usages().first();
            if (__usage instanceof ShortCircuitOrNode)
            {
                processBinary((ShortCircuitOrNode) __usage);
            }
            else if (__usage instanceof IfNode)
            {
                processIf(__binary.getX(), __binary.isXNegated(), __binary.getY(), __binary.isYNegated(), (IfNode) __usage, __binary.getShortCircuitProbability());
            }
            else if (__usage instanceof ConditionalNode)
            {
                processConditional(__binary.getX(), __binary.isXNegated(), __binary.getY(), __binary.isYNegated(), (ConditionalNode) __usage);
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        __binary.safeDelete();
    }

    private static void processIf(LogicNode __x, boolean __xNegated, LogicNode __y, boolean __yNegated, IfNode __ifNode, double __shortCircuitProbability)
    {
        /*
         * this method splits an IfNode, which has a ShortCircuitOrNode as its condition, into two
         * separate IfNodes: if(X) and if(Y)
         *
         * for computing the probabilities P(X) and P(Y), we use two different approaches. The first
         * one assumes that the shortCircuitProbability and the probability on the IfNode were
         * created with each other in mind. If this assumption does not hold, we fall back to
         * another mechanism for computing the probabilities.
         */
        AbstractBeginNode __trueTarget = __ifNode.trueSuccessor();
        AbstractBeginNode __falseTarget = __ifNode.falseSuccessor();

        // 1st approach
        // assumption: P(originalIf.trueSuccessor) == P(X) + ((1 - P(X)) * P(Y))
        double __firstIfTrueProbability = __shortCircuitProbability;
        double __secondIfTrueProbability = sanitizeProbability((__ifNode.getTrueSuccessorProbability() - __shortCircuitProbability) / (1 - __shortCircuitProbability));
        double __expectedOriginalIfTrueProbability = __firstIfTrueProbability + (1 - __firstIfTrueProbability) * __secondIfTrueProbability;

        if (!doubleEquals(__ifNode.getTrueSuccessorProbability(), __expectedOriginalIfTrueProbability))
        {
            /*
             * 2nd approach
             *
             * the assumption above did not hold, so we either used an artificial probability as
             * shortCircuitProbability or the ShortCircuitOrNode was moved to some other IfNode.
             *
             * so, we distribute the if's trueSuccessorProbability between the newly generated if
             * nodes according to the shortCircuitProbability. the following invariant is always
             * true in this case: P(originalIf.trueSuccessor) == P(X) + ((1 - P(X)) * P(Y))
             */
            __firstIfTrueProbability = __ifNode.getTrueSuccessorProbability() * __shortCircuitProbability;
            __secondIfTrueProbability = sanitizeProbability(1 - (__ifNode.probability(__falseTarget) / (1 - __firstIfTrueProbability)));
        }

        __ifNode.clearSuccessors();
        Graph __graph = __ifNode.graph();
        AbstractMergeNode __trueTargetMerge = __graph.add(new MergeNode());
        __trueTargetMerge.setNext(__trueTarget);
        EndNode __firstTrueEnd = __graph.add(new EndNode());
        EndNode __secondTrueEnd = __graph.add(new EndNode());
        __trueTargetMerge.addForwardEnd(__firstTrueEnd);
        __trueTargetMerge.addForwardEnd(__secondTrueEnd);
        AbstractBeginNode __firstTrueTarget = BeginNode.begin(__firstTrueEnd);
        AbstractBeginNode __secondTrueTarget = BeginNode.begin(__secondTrueEnd);
        if (__yNegated)
        {
            __secondIfTrueProbability = 1.0 - __secondIfTrueProbability;
        }
        if (__xNegated)
        {
            __firstIfTrueProbability = 1.0 - __firstIfTrueProbability;
        }
        IfNode __secondIf = new IfNode(__y, __yNegated ? __falseTarget : __secondTrueTarget, __yNegated ? __secondTrueTarget : __falseTarget, __secondIfTrueProbability);
        AbstractBeginNode __secondIfBegin = BeginNode.begin(__graph.add(__secondIf));
        IfNode __firstIf = __graph.add(new IfNode(__x, __xNegated ? __secondIfBegin : __firstTrueTarget, __xNegated ? __firstTrueTarget : __secondIfBegin, __firstIfTrueProbability));
        __ifNode.replaceAtPredecessor(__firstIf);
        __ifNode.safeDelete();
    }

    private static boolean doubleEquals(double __a, double __b)
    {
        return __a - EPSILON < __b && __a + EPSILON > __b;
    }

    private static double sanitizeProbability(double __value)
    {
        double __newValue = Math.min(1.0, Math.max(0.0, __value));
        if (Double.isNaN(__newValue))
        {
            __newValue = 0.5;
        }
        return __newValue;
    }

    private static void processConditional(LogicNode __x, boolean __xNegated, LogicNode __y, boolean __yNegated, ConditionalNode __conditional)
    {
        ValueNode __trueTarget = __conditional.trueValue();
        ValueNode __falseTarget = __conditional.falseValue();
        Graph __graph = __conditional.graph();
        ConditionalNode __secondConditional = __graph.unique(new ConditionalNode(__y, __yNegated ? __falseTarget : __trueTarget, __yNegated ? __trueTarget : __falseTarget));
        ConditionalNode __firstConditional = __graph.unique(new ConditionalNode(__x, __xNegated ? __secondConditional : __trueTarget, __xNegated ? __trueTarget : __secondConditional));
        __conditional.replaceAndDelete(__firstConditional);
    }
}
