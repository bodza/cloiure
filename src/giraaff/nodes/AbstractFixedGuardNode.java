package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.util.GraphUtil;

// @class AbstractFixedGuardNode
public abstract class AbstractFixedGuardNode extends DeoptimizingFixedWithNextNode implements Simplifiable, GuardingNode, DeoptimizingGuard
{
    // @def
    public static final NodeClass<AbstractFixedGuardNode> TYPE = NodeClass.create(AbstractFixedGuardNode.class);

    @Input(InputType.Condition)
    // @field
    protected LogicNode condition;
    // @field
    protected DeoptimizationReason reason;
    // @field
    protected DeoptimizationAction action;
    // @field
    protected JavaConstant speculation;
    // @field
    protected boolean negated;

    @Override
    public LogicNode getCondition()
    {
        return condition;
    }

    public LogicNode condition()
    {
        return getCondition();
    }

    @Override
    public void setCondition(LogicNode __x, boolean __negated)
    {
        updateUsages(condition, __x);
        condition = __x;
        this.negated = __negated;
    }

    // @cons
    protected AbstractFixedGuardNode(NodeClass<? extends AbstractFixedGuardNode> __c, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, JavaConstant __speculation, boolean __negated)
    {
        super(__c, StampFactory.forVoid());
        this.action = __action;
        this.speculation = __speculation;
        this.negated = __negated;
        this.condition = __condition;
        this.reason = __deoptReason;
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

    @Override
    public boolean isNegated()
    {
        return negated;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        while (condition instanceof LogicNegationNode)
        {
            LogicNegationNode __negation = (LogicNegationNode) condition;
            setCondition(__negation.getValue(), !negated);
        }
    }

    public DeoptimizeNode lowerToIf()
    {
        FixedNode __currentNext = next();
        setNext(null);
        DeoptimizeNode __deopt = graph().add(new DeoptimizeNode(action, reason, speculation));
        __deopt.setStateBefore(stateBefore());
        IfNode __ifNode;
        AbstractBeginNode __noDeoptSuccessor;
        if (negated)
        {
            __ifNode = graph().add(new IfNode(condition, __deopt, __currentNext, 0));
            __noDeoptSuccessor = __ifNode.falseSuccessor();
        }
        else
        {
            __ifNode = graph().add(new IfNode(condition, __currentNext, __deopt, 1));
            __noDeoptSuccessor = __ifNode.trueSuccessor();
        }
        ((FixedWithNextNode) predecessor()).setNext(__ifNode);
        this.replaceAtUsages(__noDeoptSuccessor);
        GraphUtil.killWithUnusedFloatingInputs(this);

        return __deopt;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public void setAction(DeoptimizationAction __action)
    {
        this.action = __action;
    }

    @Override
    public void setReason(DeoptimizationReason __reason)
    {
        this.reason = __reason;
    }
}
