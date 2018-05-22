package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.util.GraphUtil;

public abstract class AbstractFixedGuardNode extends DeoptimizingFixedWithNextNode implements Simplifiable, GuardingNode, DeoptimizingGuard
{
    public static final NodeClass<AbstractFixedGuardNode> TYPE = NodeClass.create(AbstractFixedGuardNode.class);
    @Input(InputType.Condition) protected LogicNode condition;
    protected DeoptimizationReason reason;
    protected DeoptimizationAction action;
    protected JavaConstant speculation;
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
    public void setCondition(LogicNode x, boolean negated)
    {
        updateUsages(condition, x);
        condition = x;
        this.negated = negated;
    }

    protected AbstractFixedGuardNode(NodeClass<? extends AbstractFixedGuardNode> c, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, JavaConstant speculation, boolean negated)
    {
        super(c, StampFactory.forVoid());
        this.action = action;
        this.speculation = speculation;
        this.negated = negated;
        this.condition = condition;
        this.reason = deoptReason;
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
    public void simplify(SimplifierTool tool)
    {
        while (condition instanceof LogicNegationNode)
        {
            LogicNegationNode negation = (LogicNegationNode) condition;
            setCondition(negation.getValue(), !negated);
        }
    }

    public DeoptimizeNode lowerToIf()
    {
        FixedNode currentNext = next();
        setNext(null);
        DeoptimizeNode deopt = graph().add(new DeoptimizeNode(action, reason, speculation));
        deopt.setStateBefore(stateBefore());
        IfNode ifNode;
        AbstractBeginNode noDeoptSuccessor;
        if (negated)
        {
            ifNode = graph().add(new IfNode(condition, deopt, currentNext, 0));
            noDeoptSuccessor = ifNode.falseSuccessor();
        }
        else
        {
            ifNode = graph().add(new IfNode(condition, currentNext, deopt, 1));
            noDeoptSuccessor = ifNode.trueSuccessor();
        }
        ((FixedWithNextNode) predecessor()).setNext(ifNode);
        this.replaceAtUsages(noDeoptSuccessor);
        GraphUtil.killWithUnusedFloatingInputs(this);

        return deopt;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public void setAction(DeoptimizationAction action)
    {
        this.action = action;
    }

    @Override
    public void setReason(DeoptimizationReason reason)
    {
        this.reason = reason;
    }
}
