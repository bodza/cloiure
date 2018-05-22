package graalvm.compiler.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.extended.AnchoringNode;
import graalvm.compiler.nodes.extended.GuardingNode;

/**
 * A guard is a node that deoptimizes based on a conditional expression. Guards are not attached to
 * a certain frame state, they can move around freely and will always use the correct frame state
 * when the nodes are scheduled (i.e., the last emitted frame state). The node that is guarded has a
 * data dependency on the guard and the guard in turn has a data dependency on the condition. A
 * guard may only be executed if it is guaranteed that the guarded node is executed too (if no
 * exceptions are thrown). Therefore, an anchor is placed after a control flow split and the guard
 * has a data dependency to the anchor. The anchor is the most distant node that is post-dominated
 * by the guarded node and the guard can be scheduled anywhere between those two nodes. This ensures
 * maximum flexibility for the guard node and guarantees that deoptimization occurs only if the
 * control flow would have reached the guarded node (without taking exceptions into account).
 */
public class GuardNode extends FloatingAnchoredNode implements Canonicalizable, GuardingNode, DeoptimizingGuard, IterableNodeType
{
    public static final NodeClass<GuardNode> TYPE = NodeClass.create(GuardNode.class);
    @Input(InputType.Condition) protected LogicNode condition;
    protected DeoptimizationReason reason;
    protected DeoptimizationAction action;
    protected JavaConstant speculation;
    protected boolean negated;

    public GuardNode(LogicNode condition, AnchoringNode anchor, DeoptimizationReason reason, DeoptimizationAction action, boolean negated, JavaConstant speculation)
    {
        this(TYPE, condition, anchor, reason, action, negated, speculation);
    }

    protected GuardNode(NodeClass<? extends GuardNode> c, LogicNode condition, AnchoringNode anchor, DeoptimizationReason reason, DeoptimizationAction action, boolean negated, JavaConstant speculation)
    {
        super(c, StampFactory.forVoid(), anchor);
        this.condition = condition;
        this.reason = reason;
        this.action = action;
        this.negated = negated;
        this.speculation = speculation;
    }

    /**
     * The instruction that produces the tested boolean value.
     */
    @Override
    public LogicNode getCondition()
    {
        return condition;
    }

    @Override
    public void setCondition(LogicNode x, boolean negated)
    {
        updateUsages(condition, x);
        condition = x;
        this.negated = negated;
    }

    @Override
    public boolean isNegated()
    {
        return negated;
    }

    @Override
    public DeoptimizationReason getReason()
    {
        return reason;
    }

    @Override
    public DeoptimizationAction getAction()
    {
        return action;
    }

    @Override
    public JavaConstant getSpeculation()
    {
        return speculation;
    }

    public void setSpeculation(JavaConstant speculation)
    {
        this.speculation = speculation;
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
    public Node canonical(CanonicalizerTool tool)
    {
        if (getCondition() instanceof LogicNegationNode)
        {
            LogicNegationNode negation = (LogicNegationNode) getCondition();
            return new GuardNode(negation.getValue(), getAnchor(), reason, action, !negated, speculation);
        }
        if (getCondition() instanceof LogicConstantNode)
        {
            LogicConstantNode c = (LogicConstantNode) getCondition();
            if (c.getValue() != negated)
            {
                return null;
            }
        }
        return this;
    }

    public FixedWithNextNode lowerGuard()
    {
        return null;
    }

    public void negate()
    {
        negated = !negated;
    }

    @Override
    public void setAction(DeoptimizationAction invalidaterecompile)
    {
        this.action = invalidaterecompile;
    }

    @Override
    public void setReason(DeoptimizationReason reason)
    {
        this.reason = reason;
    }
}
