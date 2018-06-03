package giraaff.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.calc.Condition;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.calc.NormalizeCompareNode;
import giraaff.nodes.calc.ObjectEqualsNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

/**
 * The {@code IfNode} represents a branch that can go one of two directions depending on the outcome
 * of a comparison.
 */
// @class IfNode
public final class IfNode extends ControlSplitNode implements Simplifiable, LIRLowerable
{
    // @def
    public static final NodeClass<IfNode> TYPE = NodeClass.create(IfNode.class);

    @Successor
    // @field
    AbstractBeginNode trueSuccessor;
    @Successor
    // @field
    AbstractBeginNode falseSuccessor;
    @Input(InputType.Condition)
    // @field
    LogicNode condition;
    // @field
    protected double trueSuccessorProbability;

    public LogicNode condition()
    {
        return condition;
    }

    public void setCondition(LogicNode __x)
    {
        updateUsages(condition, __x);
        condition = __x;
    }

    // @cons
    public IfNode(LogicNode __condition, FixedNode __trueSuccessor, FixedNode __falseSuccessor, double __trueSuccessorProbability)
    {
        this(__condition, BeginNode.begin(__trueSuccessor), BeginNode.begin(__falseSuccessor), __trueSuccessorProbability);
    }

    // @cons
    public IfNode(LogicNode __condition, AbstractBeginNode __trueSuccessor, AbstractBeginNode __falseSuccessor, double __trueSuccessorProbability)
    {
        super(TYPE, StampFactory.forVoid());
        this.condition = __condition;
        this.falseSuccessor = __falseSuccessor;
        this.trueSuccessor = __trueSuccessor;
        setTrueSuccessorProbability(__trueSuccessorProbability);
    }

    /**
     * Gets the true successor.
     *
     * @return the true successor
     */
    public AbstractBeginNode trueSuccessor()
    {
        return trueSuccessor;
    }

    /**
     * Gets the false successor.
     *
     * @return the false successor
     */
    public AbstractBeginNode falseSuccessor()
    {
        return falseSuccessor;
    }

    public double getTrueSuccessorProbability()
    {
        return this.trueSuccessorProbability;
    }

    public void setTrueSuccessor(AbstractBeginNode __node)
    {
        updatePredecessor(trueSuccessor, __node);
        trueSuccessor = __node;
    }

    public void setFalseSuccessor(AbstractBeginNode __node)
    {
        updatePredecessor(falseSuccessor, __node);
        falseSuccessor = __node;
    }

    /**
     * Gets the node corresponding to the specified outcome of the branch.
     *
     * @param istrue {@code true} if the true successor is requested, {@code false} otherwise
     * @return the corresponding successor
     */
    public AbstractBeginNode successor(boolean __istrue)
    {
        return __istrue ? trueSuccessor : falseSuccessor;
    }

    public void setTrueSuccessorProbability(double __prob)
    {
        trueSuccessorProbability = Math.min(1.0, Math.max(0.0, __prob));
    }

    @Override
    public double probability(AbstractBeginNode __successor)
    {
        return __successor == trueSuccessor ? trueSuccessorProbability : 1 - trueSuccessorProbability;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.emitIf(this);
    }

    public void eliminateNegation()
    {
        AbstractBeginNode __oldTrueSuccessor = trueSuccessor;
        AbstractBeginNode __oldFalseSuccessor = falseSuccessor;
        trueSuccessor = __oldFalseSuccessor;
        falseSuccessor = __oldTrueSuccessor;
        trueSuccessorProbability = 1 - trueSuccessorProbability;
        setCondition(((LogicNegationNode) condition).getValue());
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        if (trueSuccessor().next() instanceof DeoptimizeNode)
        {
            if (trueSuccessorProbability != 0)
            {
                trueSuccessorProbability = 0;
            }
        }
        else if (falseSuccessor().next() instanceof DeoptimizeNode)
        {
            if (trueSuccessorProbability != 1)
            {
                trueSuccessorProbability = 1;
            }
        }

        if (condition() instanceof LogicNegationNode)
        {
            eliminateNegation();
        }
        if (condition() instanceof LogicConstantNode)
        {
            LogicConstantNode __c = (LogicConstantNode) condition();
            if (__c.getValue())
            {
                __tool.deleteBranch(falseSuccessor());
                __tool.addToWorkList(trueSuccessor());
                graph().removeSplit(this, trueSuccessor());
            }
            else
            {
                __tool.deleteBranch(trueSuccessor());
                __tool.addToWorkList(falseSuccessor());
                graph().removeSplit(this, falseSuccessor());
            }
            return;
        }
        if (__tool.allUsagesAvailable() && trueSuccessor().hasNoUsages() && falseSuccessor().hasNoUsages())
        {
            pushNodesThroughIf(__tool);

            if (checkForUnsignedCompare(__tool) || removeOrMaterializeIf(__tool))
            {
                return;
            }
        }

        if (removeIntermediateMaterialization(__tool))
        {
            return;
        }

        if (splitIfAtPhi(__tool))
        {
            return;
        }

        if (conditionalNodeOptimization(__tool))
        {
            return;
        }

        if (falseSuccessor().hasNoUsages() && (!(falseSuccessor() instanceof LoopExitNode)) && falseSuccessor().next() instanceof IfNode)
        {
            AbstractBeginNode __intermediateBegin = falseSuccessor();
            IfNode __nextIf = (IfNode) __intermediateBegin.next();
            double __probabilityB = (1.0 - this.trueSuccessorProbability) * __nextIf.trueSuccessorProbability;
            if (this.trueSuccessorProbability < __probabilityB)
            {
                // Reordering of those two if statements is beneficial from the point of view of their probabilities.
                if (prepareForSwap(__tool, condition(), __nextIf.condition()))
                {
                    // Reordering is allowed from (if1 => begin => if2) to (if2 => begin => if1).
                    AbstractBeginNode __bothFalseBegin = __nextIf.falseSuccessor();
                    __nextIf.setFalseSuccessor(null);
                    __intermediateBegin.setNext(null);
                    this.setFalseSuccessor(null);

                    this.replaceAtPredecessor(__nextIf);
                    __nextIf.setFalseSuccessor(__intermediateBegin);
                    __intermediateBegin.setNext(this);
                    this.setFalseSuccessor(__bothFalseBegin);

                    __nextIf.setTrueSuccessorProbability(__probabilityB);
                    if (__probabilityB == 1.0)
                    {
                        this.setTrueSuccessorProbability(0.0);
                    }
                    else
                    {
                        double __newProbability = this.trueSuccessorProbability / (1.0 - __probabilityB);
                        this.setTrueSuccessorProbability(Math.min(1.0, __newProbability));
                    }
                    return;
                }
            }
        }

        if (tryEliminateBoxedReferenceEquals(__tool))
        {
            return;
        }
    }

    private boolean isUnboxedFrom(MetaAccessProvider __meta, NodeView __view, ValueNode __x, ValueNode __src)
    {
        if (__x == __src)
        {
            return true;
        }
        else if (__x instanceof UnboxNode)
        {
            return isUnboxedFrom(__meta, __view, ((UnboxNode) __x).getValue(), __src);
        }
        else if (__x instanceof PiNode)
        {
            PiNode __pi = (PiNode) __x;
            return isUnboxedFrom(__meta, __view, __pi.getOriginalNode(), __src);
        }
        else if (__x instanceof LoadFieldNode)
        {
            LoadFieldNode __load = (LoadFieldNode) __x;
            ResolvedJavaType __integerType = __meta.lookupJavaType(Integer.class);
            if (__load.getValue().stamp(__view).javaType(__meta).equals(__integerType))
            {
                return isUnboxedFrom(__meta, __view, __load.getValue(), __src);
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * Attempts to replace the following pattern:
     *
     * <pre>
     * Integer x = ...;
     * Integer y = ...;
     * if ((x == y) || x.equals(y)) { ... }
     * </pre>
     *
     * with:
     *
     * <pre>
     * Integer x = ...;
     * Integer y = ...;
     * if (x.equals(y)) { ... }
     * </pre>
     *
     * whenever the probability that the reference check will pass is relatively small.
     *
     * See GR-1315 for more information.
     */
    private boolean tryEliminateBoxedReferenceEquals(SimplifierTool __tool)
    {
        if (!(condition instanceof ObjectEqualsNode))
        {
            return false;
        }

        MetaAccessProvider __meta = __tool.getMetaAccess();
        ObjectEqualsNode __equalsCondition = (ObjectEqualsNode) condition;
        ValueNode __x = __equalsCondition.getX();
        ValueNode __y = __equalsCondition.getY();
        ResolvedJavaType __integerType = __meta.lookupJavaType(Integer.class);

        // At least one argument for reference equal must be a boxed primitive.
        NodeView __view = NodeView.from(__tool);
        if (!__x.stamp(__view).javaType(__meta).equals(__integerType) && !__y.stamp(__view).javaType(__meta).equals(__integerType))
        {
            return false;
        }

        // The reference equality check is usually more efficient compared to a boxing check.
        // The success of the reference equals must therefore be relatively rare, otherwise
        // it makes no sense to eliminate it.
        if (getTrueSuccessorProbability() > 0.4)
        {
            return false;
        }

        // True branch must be empty.
        if (trueSuccessor instanceof BeginNode || trueSuccessor instanceof LoopExitNode)
        {
            if (trueSuccessor.next() instanceof EndNode)
            {
                // Empty true branch.
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // False branch must only check the unboxed values.
        UnboxNode __unbox = null;
        FixedGuardNode __unboxCheck = null;
        for (FixedNode __node : falseSuccessor.getBlockNodes())
        {
            if (!(__node instanceof BeginNode || __node instanceof UnboxNode || __node instanceof FixedGuardNode || __node instanceof EndNode || __node instanceof LoadFieldNode || __node instanceof LoopExitNode))
            {
                return false;
            }
            if (__node instanceof UnboxNode)
            {
                if (__unbox == null)
                {
                    __unbox = (UnboxNode) __node;
                }
                else
                {
                    return false;
                }
            }
            if (!(__node instanceof FixedGuardNode))
            {
                continue;
            }
            FixedGuardNode __fixed = (FixedGuardNode) __node;
            if (!(__fixed.condition() instanceof IntegerEqualsNode))
            {
                continue;
            }
            IntegerEqualsNode __equals = (IntegerEqualsNode) __fixed.condition();
            if ((isUnboxedFrom(__meta, __view, __equals.getX(), __x) && isUnboxedFrom(__meta, __view, __equals.getY(), __y)) || (isUnboxedFrom(__meta, __view, __equals.getX(), __y) && isUnboxedFrom(__meta, __view, __equals.getY(), __x)))
            {
                __unboxCheck = __fixed;
            }
        }
        if (__unbox == null || __unboxCheck == null)
        {
            return false;
        }

        // Falsify the reference check.
        setCondition(graph().addOrUniqueWithInputs(LogicConstantNode.contradiction()));

        return true;
    }

    /**
     * Try to optimize this as if it were a {@link ConditionalNode}.
     */
    private boolean conditionalNodeOptimization(SimplifierTool __tool)
    {
        if (trueSuccessor().next() instanceof AbstractEndNode && falseSuccessor().next() instanceof AbstractEndNode)
        {
            AbstractEndNode __trueEnd = (AbstractEndNode) trueSuccessor().next();
            AbstractEndNode __falseEnd = (AbstractEndNode) falseSuccessor().next();
            if (__trueEnd.merge() != __falseEnd.merge())
            {
                return false;
            }
            if (!(__trueEnd.merge() instanceof MergeNode))
            {
                return false;
            }
            MergeNode __merge = (MergeNode) __trueEnd.merge();
            if (__merge.usages().count() != 1 || __merge.phis().count() != 1)
            {
                return false;
            }

            if (trueSuccessor().anchored().isNotEmpty() || falseSuccessor().anchored().isNotEmpty())
            {
                return false;
            }

            PhiNode __phi = __merge.phis().first();
            ValueNode __falseValue = __phi.valueAt(__falseEnd);
            ValueNode __trueValue = __phi.valueAt(__trueEnd);

            NodeView __view = NodeView.from(__tool);
            ValueNode __result = ConditionalNode.canonicalizeConditional(condition, __trueValue, __falseValue, __phi.stamp(__view), __view);
            if (__result != null)
            {
                // canonicalizeConditional returns possibly new nodes so add them to the graph
                if (__result.graph() == null)
                {
                    __result = graph().addOrUniqueWithInputs(__result);
                }
                __result = proxyReplacement(__result);
                /*
                 * This optimization can be performed even if multiple values merge at this phi
                 * since the two inputs get simplified into one.
                 */
                __phi.setValueAt(__trueEnd, __result);
                removeThroughFalseBranch(__tool, __merge);
                return true;
            }
        }

        return false;
    }

    private void pushNodesThroughIf(SimplifierTool __tool)
    {
        // push similar nodes upwards through the if, thereby deduplicating them
        do
        {
            AbstractBeginNode __trueSucc = trueSuccessor();
            AbstractBeginNode __falseSucc = falseSuccessor();
            if (__trueSucc instanceof BeginNode && __falseSucc instanceof BeginNode && __trueSucc.next() instanceof FixedWithNextNode && __falseSucc.next() instanceof FixedWithNextNode)
            {
                FixedWithNextNode __trueNext = (FixedWithNextNode) __trueSucc.next();
                FixedWithNextNode __falseNext = (FixedWithNextNode) __falseSucc.next();
                NodeClass<?> __nodeClass = __trueNext.getNodeClass();
                if (__trueNext.getClass() == __falseNext.getClass())
                {
                    if (__trueNext instanceof AbstractBeginNode)
                    {
                        // Cannot do this optimization for begin nodes, because it could
                        // move guards above the if that need to stay below a branch.
                    }
                    else if (__nodeClass.equalInputs(__trueNext, __falseNext) && __trueNext.valueEquals(__falseNext))
                    {
                        __falseNext.replaceAtUsages(__trueNext);
                        graph().removeFixed(__falseNext);
                        GraphUtil.unlinkFixedNode(__trueNext);
                        graph().addBeforeFixed(this, __trueNext);
                        for (Node __usage : __trueNext.usages().snapshot())
                        {
                            if (__usage.isAlive())
                            {
                                NodeClass<?> __usageNodeClass = __usage.getNodeClass();
                                if (__usageNodeClass.valueNumberable() && !__usageNodeClass.isLeafNode())
                                {
                                    Node __newNode = graph().findDuplicate(__usage);
                                    if (__newNode != null)
                                    {
                                        __usage.replaceAtUsagesAndDelete(__newNode);
                                    }
                                }
                                if (__usage.isAlive())
                                {
                                    __tool.addToWorkList(__usage);
                                }
                            }
                        }
                        continue;
                    }
                }
            }
            break;
        } while (true);
    }

    /**
     * Recognize a couple patterns that can be merged into an unsigned compare.
     *
     * @return true if a replacement was done.
     */
    private boolean checkForUnsignedCompare(SimplifierTool __tool)
    {
        if (condition() instanceof IntegerLessThanNode)
        {
            NodeView __view = NodeView.from(__tool);
            IntegerLessThanNode __lessThan = (IntegerLessThanNode) condition();
            Constant __y = __lessThan.getY().stamp(__view).asConstant();
            if (__y instanceof PrimitiveConstant && ((PrimitiveConstant) __y).asLong() == 0 && falseSuccessor().next() instanceof IfNode)
            {
                IfNode __ifNode2 = (IfNode) falseSuccessor().next();
                if (__ifNode2.condition() instanceof IntegerLessThanNode)
                {
                    IntegerLessThanNode __lessThan2 = (IntegerLessThanNode) __ifNode2.condition();
                    AbstractBeginNode __falseSucc = __ifNode2.falseSuccessor();
                    AbstractBeginNode __trueSucc = __ifNode2.trueSuccessor();
                    IntegerBelowNode __below = null;
                    /*
                     * Convert x >= 0 && x < positive which is represented as !(x < 0) && x < <positive> into an unsigned compare.
                     */
                    if (__lessThan2.getX() == __lessThan.getX() && __lessThan2.getY().stamp(__view) instanceof IntegerStamp && ((IntegerStamp) __lessThan2.getY().stamp(__view)).isPositive() && sameDestination(trueSuccessor(), __ifNode2.falseSuccessor))
                    {
                        __below = graph().unique(new IntegerBelowNode(__lessThan2.getX(), __lessThan2.getY()));
                        // swap direction
                        AbstractBeginNode __tmp = __falseSucc;
                        __falseSucc = __trueSucc;
                        __trueSucc = __tmp;
                    }
                    else if (__lessThan2.getY() == __lessThan.getX() && sameDestination(trueSuccessor(), __ifNode2.trueSuccessor))
                    {
                        /*
                         * Convert x >= 0 && x <= positive which is represented as !(x < 0) &&
                         * !(<positive> > x), into x <| positive + 1. This can only be done for
                         * constants since there isn't a IntegerBelowEqualThanNode but that doesn't
                         * appear to be interesting.
                         */
                        JavaConstant __positive = __lessThan2.getX().asJavaConstant();
                        if (__positive != null && __positive.asLong() > 0 && __positive.asLong() < __positive.getJavaKind().getMaxValue())
                        {
                            ConstantNode __newLimit = ConstantNode.forIntegerStamp(__lessThan2.getX().stamp(__view), __positive.asLong() + 1, graph());
                            __below = graph().unique(new IntegerBelowNode(__lessThan.getX(), __newLimit));
                        }
                    }
                    if (__below != null)
                    {
                        __ifNode2.setTrueSuccessor(null);
                        __ifNode2.setFalseSuccessor(null);

                        IfNode __newIfNode = graph().add(new IfNode(__below, __falseSucc, __trueSucc, 1 - trueSuccessorProbability));
                        // Remove the < 0 test.
                        __tool.deleteBranch(trueSuccessor);
                        graph().removeSplit(this, falseSuccessor);

                        // Replace the second test with the new one.
                        __ifNode2.predecessor().replaceFirstSuccessor(__ifNode2, __newIfNode);
                        __ifNode2.safeDelete();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check it these two blocks end up at the same place. Meeting at the same merge, or
     * deoptimizing in the same way.
     */
    private static boolean sameDestination(AbstractBeginNode __succ1, AbstractBeginNode __succ2)
    {
        Node __next1 = __succ1.next();
        Node __next2 = __succ2.next();
        if (__next1 instanceof EndNode && __next2 instanceof EndNode)
        {
            EndNode __end1 = (EndNode) __next1;
            EndNode __end2 = (EndNode) __next2;
            if (__end1.merge() == __end2.merge())
            {
                for (PhiNode __phi : __end1.merge().phis())
                {
                    if (__phi.valueAt(__end1) != __phi.valueAt(__end2))
                    {
                        return false;
                    }
                }
                // they go to the same MergeNode and merge the same values
                return true;
            }
        }
        else if (__next1 instanceof DeoptimizeNode && __next2 instanceof DeoptimizeNode)
        {
            DeoptimizeNode __deopt1 = (DeoptimizeNode) __next1;
            DeoptimizeNode __deopt2 = (DeoptimizeNode) __next2;
            if (__deopt1.getReason() == __deopt2.getReason() && __deopt1.getAction() == __deopt2.getAction())
            {
                // same deoptimization reason and action
                return true;
            }
        }
        else if (__next1 instanceof LoopExitNode && __next2 instanceof LoopExitNode)
        {
            LoopExitNode __exit1 = (LoopExitNode) __next1;
            LoopExitNode __exit2 = (LoopExitNode) __next2;
            if (__exit1.loopBegin() == __exit2.loopBegin() && __exit1.stateAfter() == __exit2.stateAfter() && __exit1.stateAfter() == null && sameDestination(__exit1, __exit2))
            {
                // exit the same loop and end up at the same place
                return true;
            }
        }
        else if (__next1 instanceof ReturnNode && __next2 instanceof ReturnNode)
        {
            ReturnNode __exit1 = (ReturnNode) __next1;
            ReturnNode __exit2 = (ReturnNode) __next2;
            if (__exit1.result() == __exit2.result())
            {
                // exit the same loop and end up at the same place
                return true;
            }
        }
        return false;
    }

    private static boolean prepareForSwap(SimplifierTool __tool, LogicNode __a, LogicNode __b)
    {
        if (__a instanceof InstanceOfNode)
        {
            InstanceOfNode __instanceOfA = (InstanceOfNode) __a;
            if (__b instanceof IsNullNode)
            {
                IsNullNode __isNullNode = (IsNullNode) __b;
                if (__isNullNode.getValue() == __instanceOfA.getValue())
                {
                    return true;
                }
            }
            else if (__b instanceof InstanceOfNode)
            {
                InstanceOfNode __instanceOfB = (InstanceOfNode) __b;
                if (__instanceOfA.getValue() == __instanceOfB.getValue() && !__instanceOfA.type().getType().isInterface() && !__instanceOfB.type().getType().isInterface() && !__instanceOfA.type().getType().isAssignableFrom(__instanceOfB.type().getType()) && !__instanceOfB.type().getType().isAssignableFrom(__instanceOfA.type().getType()))
                {
                    // Two instanceof on the same value with mutually exclusive types.
                    return true;
                }
            }
        }
        else if (__a instanceof CompareNode)
        {
            CompareNode __compareA = (CompareNode) __a;
            Condition __conditionA = __compareA.condition().asCondition();
            if (__compareA.unorderedIsTrue())
            {
                return false;
            }
            if (__b instanceof CompareNode)
            {
                CompareNode __compareB = (CompareNode) __b;
                if (__compareA == __compareB)
                {
                    return false;
                }
                if (__compareB.unorderedIsTrue())
                {
                    return false;
                }
                Condition __comparableCondition = null;
                Condition __conditionB = __compareB.condition().asCondition();
                if (__compareB.getX() == __compareA.getX() && __compareB.getY() == __compareA.getY())
                {
                    __comparableCondition = __conditionB;
                }
                else if (__compareB.getX() == __compareA.getY() && __compareB.getY() == __compareA.getX())
                {
                    __comparableCondition = __conditionB.mirror();
                }

                if (__comparableCondition != null)
                {
                    Condition __combined = __conditionA.join(__comparableCondition);
                    if (__combined == null)
                    {
                        // The two conditions are disjoint => can reorder.
                        return true;
                    }
                }
                else if (__conditionA == Condition.EQ && __conditionB == Condition.EQ)
                {
                    boolean __canSwap = false;
                    if ((__compareA.getX() == __compareB.getX() && valuesDistinct(__tool, __compareA.getY(), __compareB.getY())))
                    {
                        __canSwap = true;
                    }
                    else if ((__compareA.getX() == __compareB.getY() && valuesDistinct(__tool, __compareA.getY(), __compareB.getX())))
                    {
                        __canSwap = true;
                    }
                    else if ((__compareA.getY() == __compareB.getX() && valuesDistinct(__tool, __compareA.getX(), __compareB.getY())))
                    {
                        __canSwap = true;
                    }
                    else if ((__compareA.getY() == __compareB.getY() && valuesDistinct(__tool, __compareA.getX(), __compareB.getX())))
                    {
                        __canSwap = true;
                    }

                    if (__canSwap)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean valuesDistinct(SimplifierTool __tool, ValueNode __a, ValueNode __b)
    {
        if (__a.isConstant() && __b.isConstant())
        {
            Boolean __equal = __tool.getConstantReflection().constantEquals(__a.asConstant(), __b.asConstant());
            if (__equal != null)
            {
                return !__equal.booleanValue();
            }
        }

        NodeView __view = NodeView.from(__tool);
        Stamp __stampA = __a.stamp(__view);
        Stamp __stampB = __b.stamp(__view);
        return __stampA.alwaysDistinct(__stampB);
    }

    /**
     * Tries to remove an empty if construct or replace an if construct with a materialization.
     *
     * @return true if a transformation was made, false otherwise
     */
    private boolean removeOrMaterializeIf(SimplifierTool __tool)
    {
        if (trueSuccessor().next() instanceof AbstractEndNode && falseSuccessor().next() instanceof AbstractEndNode)
        {
            AbstractEndNode __trueEnd = (AbstractEndNode) trueSuccessor().next();
            AbstractEndNode __falseEnd = (AbstractEndNode) falseSuccessor().next();
            AbstractMergeNode __merge = __trueEnd.merge();
            if (__merge == __falseEnd.merge() && trueSuccessor().anchored().isEmpty() && falseSuccessor().anchored().isEmpty())
            {
                PhiNode __singlePhi = null;
                int __distinct = 0;
                for (PhiNode __phi : __merge.phis())
                {
                    ValueNode __trueValue = __phi.valueAt(__trueEnd);
                    ValueNode __falseValue = __phi.valueAt(__falseEnd);
                    if (__trueValue != __falseValue)
                    {
                        __distinct++;
                        __singlePhi = __phi;
                    }
                }
                if (__distinct == 0)
                {
                    // multiple phis, but merging same values for true and false, so simply delete the path
                    removeThroughFalseBranch(__tool, __merge);
                    return true;
                }
                else if (__distinct == 1)
                {
                    ValueNode __trueValue = __singlePhi.valueAt(__trueEnd);
                    ValueNode __falseValue = __singlePhi.valueAt(__falseEnd);
                    ValueNode __conditional = canonicalizeConditionalCascade(__tool, __trueValue, __falseValue);
                    if (__conditional != null)
                    {
                        __conditional = proxyReplacement(__conditional);
                        __singlePhi.setValueAt(__trueEnd, __conditional);
                        removeThroughFalseBranch(__tool, __merge);
                        return true;
                    }
                }
            }
        }
        if (trueSuccessor().next() instanceof ReturnNode && falseSuccessor().next() instanceof ReturnNode)
        {
            ReturnNode __trueEnd = (ReturnNode) trueSuccessor().next();
            ReturnNode __falseEnd = (ReturnNode) falseSuccessor().next();
            ValueNode __trueValue = __trueEnd.result();
            ValueNode __falseValue = __falseEnd.result();
            ValueNode __value = null;
            if (__trueValue != null)
            {
                if (__trueValue == __falseValue)
                {
                    __value = __trueValue;
                }
                else
                {
                    __value = canonicalizeConditionalCascade(__tool, __trueValue, __falseValue);
                    if (__value == null)
                    {
                        return false;
                    }
                }
            }
            ReturnNode __newReturn = graph().add(new ReturnNode(__value));
            replaceAtPredecessor(__newReturn);
            GraphUtil.killCFG(this);
            return true;
        }
        return false;
    }

    private ValueNode proxyReplacement(ValueNode __replacement)
    {
        /*
         * Special case: Every empty diamond we collapse to a conditional node can potentially
         * contain loop exit nodes on both branches. See the graph below: The two loop exits
         * (instanceof begin node) exit the same loop. The resulting phi is defined outside the
         * loop, but the resulting conditional node will be inside the loop, so we need to proxy the
         * resulting conditional node. Callers of this method ensure that true and false successor
         * have no usages, therefore a and b in the graph below can never be proxies themselves.
         *
         *              +--+
         *              |If|
         *              +--+      +-----+ +-----+
         *         +----+  +----+ |  a  | |  b  |
         *         |Lex |  |Lex | +----^+ +^----+
         *         +----+  +----+      |   |
         *           +-------+         +---+
         *           | Merge +---------+Phi|
         *           +-------+         +---+
         */
        if (this.graph().hasValueProxies())
        {
            if (trueSuccessor instanceof LoopExitNode && falseSuccessor instanceof LoopExitNode)
            {
                return this.graph().addOrUnique(new ValueProxyNode(__replacement, (LoopExitNode) trueSuccessor));
            }
        }
        return __replacement;
    }

    protected void removeThroughFalseBranch(SimplifierTool __tool, AbstractMergeNode __merge)
    {
        AbstractBeginNode __trueBegin = trueSuccessor();
        LogicNode __conditionNode = condition();
        graph().removeSplitPropagate(this, __trueBegin);
        __tool.addToWorkList(__trueBegin);
        if (__conditionNode != null)
        {
            GraphUtil.tryKillUnused(__conditionNode);
        }
        if (__merge.isAlive() && __merge.forwardEndCount() > 1)
        {
            for (FixedNode __end : __merge.forwardEnds())
            {
                Node __cur = __end;
                while (__cur != null && __cur.predecessor() instanceof BeginNode)
                {
                    __cur = __cur.predecessor();
                }
                if (__cur != null && __cur.predecessor() instanceof IfNode)
                {
                    __tool.addToWorkList(__cur.predecessor());
                }
            }
        }
    }

    private ValueNode canonicalizeConditionalCascade(SimplifierTool __tool, ValueNode __trueValue, ValueNode __falseValue)
    {
        if (__trueValue.getStackKind() != __falseValue.getStackKind())
        {
            return null;
        }
        if (__trueValue.getStackKind() != JavaKind.Int && __trueValue.getStackKind() != JavaKind.Long)
        {
            return null;
        }
        if (__trueValue.isConstant() && __falseValue.isConstant())
        {
            return graph().unique(new ConditionalNode(condition(), __trueValue, __falseValue));
        }
        else if (!graph().isAfterExpandLogic())
        {
            ConditionalNode __conditional = null;
            ValueNode __constant = null;
            boolean __negateCondition;
            if (__trueValue instanceof ConditionalNode && __falseValue.isConstant())
            {
                __conditional = (ConditionalNode) __trueValue;
                __constant = __falseValue;
                __negateCondition = true;
            }
            else if (__falseValue instanceof ConditionalNode && __trueValue.isConstant())
            {
                __conditional = (ConditionalNode) __falseValue;
                __constant = __trueValue;
                __negateCondition = false;
            }
            else
            {
                return null;
            }
            boolean __negateConditionalCondition = false;
            ValueNode __otherValue = null;
            if (__constant == __conditional.trueValue())
            {
                __otherValue = __conditional.falseValue();
                __negateConditionalCondition = false;
            }
            else if (__constant == __conditional.falseValue())
            {
                __otherValue = __conditional.trueValue();
                __negateConditionalCondition = true;
            }
            if (__otherValue != null && __otherValue.isConstant())
            {
                double __shortCutProbability = probability(trueSuccessor());
                LogicNode __newCondition = LogicNode.or(condition(), __negateCondition, __conditional.condition(), __negateConditionalCondition, __shortCutProbability);
                return graph().unique(new ConditionalNode(__newCondition, __constant, __otherValue));
            }
            else if (!__negateCondition && __constant.isJavaConstant() && __conditional.trueValue().isJavaConstant() && __conditional.falseValue().isJavaConstant())
            {
                IntegerLessThanNode __lessThan = null;
                IntegerEqualsNode __equals = null;
                if (condition() instanceof IntegerLessThanNode && __conditional.condition() instanceof IntegerEqualsNode && __constant.asJavaConstant().asLong() == -1 && __conditional.trueValue().asJavaConstant().asLong() == 0 && __conditional.falseValue().asJavaConstant().asLong() == 1)
                {
                    __lessThan = (IntegerLessThanNode) condition();
                    __equals = (IntegerEqualsNode) __conditional.condition();
                }
                else if (condition() instanceof IntegerEqualsNode && __conditional.condition() instanceof IntegerLessThanNode && __constant.asJavaConstant().asLong() == 0 && __conditional.trueValue().asJavaConstant().asLong() == -1 && __conditional.falseValue().asJavaConstant().asLong() == 1)
                {
                    __lessThan = (IntegerLessThanNode) __conditional.condition();
                    __equals = (IntegerEqualsNode) condition();
                }
                if (__lessThan != null)
                {
                    NodeView __view = NodeView.from(__tool);
                    if ((__lessThan.getX() == __equals.getX() && __lessThan.getY() == __equals.getY()) || (__lessThan.getX() == __equals.getY() && __lessThan.getY() == __equals.getX()))
                    {
                        return graph().unique(new NormalizeCompareNode(__lessThan.getX(), __lessThan.getY(), __conditional.trueValue().stamp(__view).getStackKind(), false));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Take an if that is immediately dominated by a merge with a single phi and split off any paths
     * where the test would be statically decidable creating a new merge below the approriate side
     * of the IfNode. Any undecidable tests will continue to use the original IfNode.
     */
    private boolean splitIfAtPhi(SimplifierTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtSideEffects())
        {
            // disabled until we make sure we have no FrameState-less merges at this stage
            return false;
        }

        if (!(predecessor() instanceof MergeNode))
        {
            return false;
        }
        MergeNode __merge = (MergeNode) predecessor();
        if (__merge.forwardEndCount() == 1)
        {
            // don't bother
            return false;
        }
        if (__merge.usages().count() != 1 || __merge.phis().count() != 1)
        {
            return false;
        }
        if (__merge.stateAfter() != null)
        {
            // we'll get the chance to simplify this after frame state assignment
            return false;
        }
        PhiNode __phi = __merge.phis().first();
        if (__phi.usages().count() != 1)
        {
            // for simplicity, the code below assumes that phi goes dead at the end, so skip this case
            return false;
        }

        // check that the condition uses the phi and that there is only one user of the condition expression
        if (!conditionUses(condition(), __phi))
        {
            return false;
        }

        /*
         * We could additionally filter for the case that at least some of the Phi inputs or one of
         * the condition inputs are constants but there are cases where a non-constant is simplifiable,
         * usually where the stamp allows the question to be answered.
         */

        // each successor of the if gets a new merge if needed
        MergeNode __trueMerge = null;
        MergeNode __falseMerge = null;

        for (EndNode __end : __merge.forwardEnds().snapshot())
        {
            Node __value = __phi.valueAt(__end);
            LogicNode __result = computeCondition(__tool, condition, __phi, __value);
            if (__result instanceof LogicConstantNode)
            {
                __merge.removeEnd(__end);
                if (((LogicConstantNode) __result).getValue())
                {
                    if (__trueMerge == null)
                    {
                        __trueMerge = insertMerge(trueSuccessor());
                    }
                    __trueMerge.addForwardEnd(__end);
                }
                else
                {
                    if (__falseMerge == null)
                    {
                        __falseMerge = insertMerge(falseSuccessor());
                    }
                    __falseMerge.addForwardEnd(__end);
                }
            }
            else if (__result != condition)
            {
                // build a new IfNode using the new condition
                BeginNode __trueBegin = graph().add(new BeginNode());
                BeginNode __falseBegin = graph().add(new BeginNode());

                if (__result.graph() == null)
                {
                    __result = graph().addOrUniqueWithInputs(__result);
                }
                IfNode __newIfNode = graph().add(new IfNode(__result, __trueBegin, __falseBegin, trueSuccessorProbability));
                __merge.removeEnd(__end);
                ((FixedWithNextNode) __end.predecessor()).setNext(__newIfNode);

                if (__trueMerge == null)
                {
                    __trueMerge = insertMerge(trueSuccessor());
                }
                __trueBegin.setNext(graph().add(new EndNode()));
                __trueMerge.addForwardEnd((EndNode) __trueBegin.next());

                if (__falseMerge == null)
                {
                    __falseMerge = insertMerge(falseSuccessor());
                }
                __falseBegin.setNext(graph().add(new EndNode()));
                __falseMerge.addForwardEnd((EndNode) __falseBegin.next());

                __end.safeDelete();
            }
        }

        transferProxies(trueSuccessor(), __trueMerge);
        transferProxies(falseSuccessor(), __falseMerge);

        cleanupMerge(__merge);
        cleanupMerge(__trueMerge);
        cleanupMerge(__falseMerge);

        return true;
    }

    /**
     * @return true if the passed in {@code condition} uses {@code phi} and the condition is only
     *         used once. Since the phi will go dead the condition using it will also have to be
     *         dead after the optimization.
     */
    private static boolean conditionUses(LogicNode __condition, PhiNode __phi)
    {
        if (__condition.usages().count() != 1)
        {
            return false;
        }
        if (__condition instanceof ShortCircuitOrNode)
        {
            if (__condition.graph().getGuardsStage().areDeoptsFixed())
            {
                /*
                 * It can be unsafe to simplify a ShortCircuitOr before deopts are fixed because
                 * conversion to guards assumes that all the required conditions are being tested.
                 * Simplfying the condition based on context before this happens may lose a condition.
                 */
                ShortCircuitOrNode __orNode = (ShortCircuitOrNode) __condition;
                return (conditionUses(__orNode.x, __phi) || conditionUses(__orNode.y, __phi));
            }
        }
        else if (__condition instanceof Canonicalizable.Unary<?>)
        {
            Canonicalizable.Unary<?> __unary = (Canonicalizable.Unary<?>) __condition;
            return __unary.getValue() == __phi;
        }
        else if (__condition instanceof Canonicalizable.Binary<?>)
        {
            Canonicalizable.Binary<?> __binary = (Canonicalizable.Binary<?>) __condition;
            return __binary.getX() == __phi || __binary.getY() == __phi;
        }
        return false;
    }

    /**
     * Canonicalize {@code} condition using {@code value} in place of {@code phi}.
     *
     * @return an improved LogicNode or the original condition
     */
    @SuppressWarnings("unchecked")
    private static LogicNode computeCondition(SimplifierTool __tool, LogicNode __condition, PhiNode __phi, Node __value)
    {
        if (__condition instanceof ShortCircuitOrNode)
        {
            if (__condition.graph().getGuardsStage().areDeoptsFixed() && !__condition.graph().isAfterExpandLogic())
            {
                ShortCircuitOrNode __orNode = (ShortCircuitOrNode) __condition;
                LogicNode __resultX = computeCondition(__tool, __orNode.x, __phi, __value);
                LogicNode __resultY = computeCondition(__tool, __orNode.y, __phi, __value);
                if (__resultX != __orNode.x || __resultY != __orNode.y)
                {
                    LogicNode __result = __orNode.canonical(__tool, __resultX, __resultY);
                    if (__result != __orNode)
                    {
                        return __result;
                    }
                    // Create a new node to carry the optimized inputs.
                    ShortCircuitOrNode __newOr = new ShortCircuitOrNode(__resultX, __orNode.xNegated, __resultY, __orNode.yNegated, __orNode.getShortCircuitProbability());
                    return __newOr.canonical(__tool);
                }
                return __orNode;
            }
        }
        else if (__condition instanceof Canonicalizable.Binary<?>)
        {
            Canonicalizable.Binary<Node> __compare = (Canonicalizable.Binary<Node>) __condition;
            if (__compare.getX() == __phi)
            {
                return (LogicNode) __compare.canonical(__tool, __value, __compare.getY());
            }
            else if (__compare.getY() == __phi)
            {
                return (LogicNode) __compare.canonical(__tool, __compare.getX(), __value);
            }
        }
        else if (__condition instanceof Canonicalizable.Unary<?>)
        {
            Canonicalizable.Unary<Node> __compare = (Canonicalizable.Unary<Node>) __condition;
            if (__compare.getValue() == __phi)
            {
                return (LogicNode) __compare.canonical(__tool, __value);
            }
        }
        if (__condition instanceof Canonicalizable)
        {
            return (LogicNode) ((Canonicalizable) __condition).canonical(__tool);
        }
        return __condition;
    }

    private static void transferProxies(AbstractBeginNode __successor, MergeNode __falseMerge)
    {
        if (__successor instanceof LoopExitNode && __falseMerge != null)
        {
            LoopExitNode __loopExitNode = (LoopExitNode) __successor;
            for (ProxyNode __proxy : __loopExitNode.proxies().snapshot())
            {
                __proxy.replaceFirstInput(__successor, __falseMerge);
            }
        }
    }

    private void cleanupMerge(MergeNode __merge)
    {
        if (__merge != null && __merge.isAlive())
        {
            if (__merge.forwardEndCount() == 0)
            {
                GraphUtil.killCFG(__merge);
            }
            else if (__merge.forwardEndCount() == 1)
            {
                graph().reduceTrivialMerge(__merge);
            }
        }
    }

    private MergeNode insertMerge(AbstractBeginNode __begin)
    {
        MergeNode __merge = graph().add(new MergeNode());
        if (!__begin.anchored().isEmpty())
        {
            Object __before = null;
            __before = __begin.anchored().snapshot();
            __begin.replaceAtUsages(InputType.Guard, __merge);
            __begin.replaceAtUsages(InputType.Anchor, __merge);
        }

        AbstractBeginNode __theBegin = __begin;
        if (__begin instanceof LoopExitNode)
        {
            // Insert an extra begin to make it easier.
            __theBegin = graph().add(new BeginNode());
            __begin.replaceAtPredecessor(__theBegin);
            __theBegin.setNext(__begin);
        }
        FixedNode __next = __theBegin.next();
        __next.replaceAtPredecessor(__merge);
        __theBegin.setNext(graph().add(new EndNode()));
        __merge.addForwardEnd((EndNode) __theBegin.next());
        __merge.setNext(__next);
        return __merge;
    }

    /**
     * Tries to connect code that initializes a variable directly with the successors of an if
     * construct that switches on the variable. For example, the pseudo code below:
     *
     * <pre>
     * contains(list, e, yes, no) {
     *     if (list == null || e == null) {
     *         condition = false;
     *     } else {
     *         condition = false;
     *         for (i in list) {
     *             if (i.equals(e)) {
     *                 condition = true;
     *                 break;
     *             }
     *         }
     *     }
     *     if (condition) {
     *         return yes;
     *     } else {
     *         return no;
     *     }
     * }
     * </pre>
     *
     * will be transformed into:
     *
     * <pre>
     * contains(list, e, yes, no) {
     *     if (list == null || e == null) {
     *         return no;
     *     } else {
     *         condition = false;
     *         for (i in list) {
     *             if (i.equals(e)) {
     *                 return yes;
     *             }
     *         }
     *         return no;
     *     }
     * }
     * </pre>
     *
     * @return true if a transformation was made, false otherwise
     */
    private boolean removeIntermediateMaterialization(SimplifierTool __tool)
    {
        if (!(predecessor() instanceof AbstractMergeNode) || predecessor() instanceof LoopBeginNode)
        {
            return false;
        }
        AbstractMergeNode __merge = (AbstractMergeNode) predecessor();

        if (!(condition() instanceof CompareNode))
        {
            return false;
        }

        CompareNode __compare = (CompareNode) condition();
        if (__compare.getUsageCount() != 1)
        {
            return false;
        }

        // only consider merges with a single usage that is both a phi and an operand of the comparison
        NodeIterable<Node> __mergeUsages = __merge.usages();
        if (__mergeUsages.count() != 1)
        {
            return false;
        }
        Node __singleUsage = __mergeUsages.first();
        if (!(__singleUsage instanceof ValuePhiNode) || (__singleUsage != __compare.getX() && __singleUsage != __compare.getY()))
        {
            return false;
        }

        // ensure phi is used by at most the comparison and the merge's frame state (if any)
        ValuePhiNode __phi = (ValuePhiNode) __singleUsage;
        NodeIterable<Node> __phiUsages = __phi.usages();
        if (__phiUsages.count() > 2)
        {
            return false;
        }
        for (Node __usage : __phiUsages)
        {
            if (__usage != __compare && __usage != __merge.stateAfter())
            {
                return false;
            }
        }

        List<EndNode> __mergePredecessors = __merge.cfgPredecessors().snapshot();

        Constant[] __xs = constantValues(__compare.getX(), __merge, false);
        Constant[] __ys = constantValues(__compare.getY(), __merge, false);
        if (__xs == null || __ys == null)
        {
            return false;
        }

        // Sanity check that both ends are not followed by a merge without frame state.
        if (!checkFrameState(trueSuccessor()) && !checkFrameState(falseSuccessor()))
        {
            return false;
        }

        List<EndNode> __falseEnds = new ArrayList<>(__mergePredecessors.size());
        List<EndNode> __trueEnds = new ArrayList<>(__mergePredecessors.size());
        EconomicMap<AbstractEndNode, ValueNode> __phiValues = EconomicMap.create(Equivalence.IDENTITY, __mergePredecessors.size());

        AbstractBeginNode __oldFalseSuccessor = falseSuccessor();
        AbstractBeginNode __oldTrueSuccessor = trueSuccessor();

        setFalseSuccessor(null);
        setTrueSuccessor(null);

        Iterator<EndNode> __ends = __mergePredecessors.iterator();
        for (int __i = 0; __i < __xs.length; __i++)
        {
            EndNode __end = __ends.next();
            __phiValues.put(__end, __phi.valueAt(__end));
            if (__compare.condition().foldCondition(__xs[__i], __ys[__i], __tool.getConstantReflection(), __compare.unorderedIsTrue()))
            {
                __trueEnds.add(__end);
            }
            else
            {
                __falseEnds.add(__end);
            }
        }

        connectEnds(__falseEnds, __phiValues, __oldFalseSuccessor, __merge, __tool);
        connectEnds(__trueEnds, __phiValues, __oldTrueSuccessor, __merge, __tool);

        if (this.trueSuccessorProbability == 0.0)
        {
            for (AbstractEndNode __endNode : __trueEnds)
            {
                propagateZeroProbability(__endNode);
            }
        }

        if (this.trueSuccessorProbability == 1.0)
        {
            for (AbstractEndNode __endNode : __falseEnds)
            {
                propagateZeroProbability(__endNode);
            }
        }

        /*
         * Remove obsolete ends only after processing all ends, otherwise oldTrueSuccessor or
         * oldFalseSuccessor might have been removed if it is a LoopExitNode.
         */
        if (__falseEnds.isEmpty())
        {
            GraphUtil.killCFG(__oldFalseSuccessor);
        }
        if (__trueEnds.isEmpty())
        {
            GraphUtil.killCFG(__oldTrueSuccessor);
        }
        GraphUtil.killCFG(__merge);

        return true;
    }

    private void propagateZeroProbability(FixedNode __startNode)
    {
        Node __prev = null;
        for (FixedNode __node : GraphUtil.predecessorIterable(__startNode))
        {
            if (__node instanceof IfNode)
            {
                IfNode __ifNode = (IfNode) __node;
                if (__ifNode.trueSuccessor() == __prev)
                {
                    if (__ifNode.trueSuccessorProbability == 0.0)
                    {
                        return;
                    }
                    else if (__ifNode.trueSuccessorProbability == 1.0)
                    {
                        continue;
                    }
                    else
                    {
                        __ifNode.setTrueSuccessorProbability(0.0);
                        return;
                    }
                }
                else if (__ifNode.falseSuccessor() == __prev)
                {
                    if (__ifNode.trueSuccessorProbability == 1.0)
                    {
                        return;
                    }
                    else if (__ifNode.trueSuccessorProbability == 0.0)
                    {
                        continue;
                    }
                    else
                    {
                        __ifNode.setTrueSuccessorProbability(1.0);
                        return;
                    }
                }
                else
                {
                    throw new GraalError("Illegal state");
                }
            }
            else if (__node instanceof AbstractMergeNode && !(__node instanceof LoopBeginNode))
            {
                for (AbstractEndNode __endNode : ((AbstractMergeNode) __node).cfgPredecessors())
                {
                    propagateZeroProbability(__endNode);
                }
                return;
            }
            __prev = __node;
        }
    }

    private static boolean checkFrameState(FixedNode __start)
    {
        FixedNode __node = __start;
        while (true)
        {
            if (__node instanceof AbstractMergeNode)
            {
                AbstractMergeNode __mergeNode = (AbstractMergeNode) __node;
                if (__mergeNode.stateAfter() == null)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            else if (__node instanceof StateSplit)
            {
                StateSplit __stateSplitNode = (StateSplit) __node;
                if (__stateSplitNode.stateAfter() != null)
                {
                    return true;
                }
            }

            if (__node instanceof ControlSplitNode)
            {
                ControlSplitNode __controlSplitNode = (ControlSplitNode) __node;
                for (Node __succ : __controlSplitNode.cfgSuccessors())
                {
                    if (checkFrameState((FixedNode) __succ))
                    {
                        return true;
                    }
                }
                return false;
            }
            else if (__node instanceof FixedWithNextNode)
            {
                FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __node;
                __node = __fixedWithNextNode.next();
            }
            else if (__node instanceof AbstractEndNode)
            {
                AbstractEndNode __endNode = (AbstractEndNode) __node;
                __node = __endNode.merge();
            }
            else if (__node instanceof ControlSinkNode)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * Connects a set of ends to a given successor, inserting a merge node if there is more than one
     * end. If {@code ends} is not empty, then {@code successor} is added to {@code tool}'s
     * {@linkplain SimplifierTool#addToWorkList(giraaff.graph.Node) work list}.
     *
     * @param oldMerge the merge being removed
     * @param phiValues the values of the phi at the merge, keyed by the merge ends
     */
    private void connectEnds(List<EndNode> __ends, EconomicMap<AbstractEndNode, ValueNode> __phiValues, AbstractBeginNode __successor, AbstractMergeNode __oldMerge, SimplifierTool __tool)
    {
        if (!__ends.isEmpty())
        {
            if (__ends.size() == 1)
            {
                AbstractEndNode __end = __ends.get(0);
                ((FixedWithNextNode) __end.predecessor()).setNext(__successor);
                __oldMerge.removeEnd(__end);
                GraphUtil.killCFG(__end);
            }
            else
            {
                // need a new phi in case the frame state is used by more than the merge being removed
                NodeView __view = NodeView.from(__tool);
                AbstractMergeNode __newMerge = graph().add(new MergeNode());
                PhiNode __oldPhi = (PhiNode) __oldMerge.usages().first();
                PhiNode __newPhi = graph().addWithoutUnique(new ValuePhiNode(__oldPhi.stamp(__view), __newMerge));

                for (EndNode __end : __ends)
                {
                    __newPhi.addInput(__phiValues.get(__end));
                    __newMerge.addForwardEnd(__end);
                }

                FrameState __stateAfter = __oldMerge.stateAfter();
                if (__stateAfter != null)
                {
                    __stateAfter = __stateAfter.duplicate();
                    __stateAfter.replaceFirstInput(__oldPhi, __newPhi);
                    __newMerge.setStateAfter(__stateAfter);
                }

                __newMerge.setNext(__successor);
            }
            __tool.addToWorkList(__successor);
        }
    }

    /**
     * Gets an array of constants derived from a node that is either a {@link ConstantNode} or a
     * {@link PhiNode} whose input values are all constants. The length of the returned array is
     * equal to the number of ends terminating in a given merge node.
     *
     * @return null if {@code node} is neither a {@link ConstantNode} nor a {@link PhiNode} whose
     *         input values are all constants
     */
    public static Constant[] constantValues(ValueNode __node, AbstractMergeNode __merge, boolean __allowNull)
    {
        if (__node.isConstant())
        {
            Constant[] __result = new Constant[__merge.forwardEndCount()];
            Arrays.fill(__result, __node.asConstant());
            return __result;
        }

        if (__node instanceof PhiNode)
        {
            PhiNode __phi = (PhiNode) __node;
            if (__phi.merge() == __merge && __phi instanceof ValuePhiNode && __phi.valueCount() == __merge.forwardEndCount())
            {
                Constant[] __result = new Constant[__merge.forwardEndCount()];
                int __i = 0;
                for (ValueNode __n : __phi.values())
                {
                    if (!__allowNull && !__n.isConstant())
                    {
                        return null;
                    }
                    __result[__i++] = __n.asConstant();
                }
                return __result;
            }
        }

        return null;
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return null;
    }

    public AbstractBeginNode getSuccessor(boolean __result)
    {
        return __result ? this.trueSuccessor() : this.falseSuccessor();
    }

    @Override
    public boolean setProbability(AbstractBeginNode __successor, double __value)
    {
        if (__successor == this.trueSuccessor())
        {
            this.setTrueSuccessorProbability(__value);
            return true;
        }
        else if (__successor == this.falseSuccessor())
        {
            this.setTrueSuccessorProbability(1.0 - __value);
            return true;
        }
        return false;
    }

    @Override
    public int getSuccessorCount()
    {
        return 2;
    }
}
