package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
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

    @Node.Input(InputType.ConditionI)
    // @field
    protected LogicNode ___condition;
    // @field
    protected DeoptimizationReason ___reason;
    // @field
    protected DeoptimizationAction ___action;
    // @field
    protected JavaConstant ___speculation;
    // @field
    protected boolean ___negated;

    @Override
    public LogicNode getCondition()
    {
        return this.___condition;
    }

    public LogicNode condition()
    {
        return getCondition();
    }

    @Override
    public void setCondition(LogicNode __x, boolean __negated)
    {
        updateUsages(this.___condition, __x);
        this.___condition = __x;
        this.___negated = __negated;
    }

    // @cons AbstractFixedGuardNode
    protected AbstractFixedGuardNode(NodeClass<? extends AbstractFixedGuardNode> __c, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, JavaConstant __speculation, boolean __negated)
    {
        super(__c, StampFactory.forVoid());
        this.___action = __action;
        this.___speculation = __speculation;
        this.___negated = __negated;
        this.___condition = __condition;
        this.___reason = __deoptReason;
    }

    @Override
    public DeoptimizationReason getReason()
    {
        return this.___reason;
    }

    @Override
    public DeoptimizationAction getAction()
    {
        return this.___action;
    }

    @Override
    public JavaConstant getSpeculation()
    {
        return this.___speculation;
    }

    @Override
    public boolean isNegated()
    {
        return this.___negated;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        while (this.___condition instanceof LogicNegationNode)
        {
            LogicNegationNode __negation = (LogicNegationNode) this.___condition;
            setCondition(__negation.getValue(), !this.___negated);
        }
    }

    public DeoptimizeNode lowerToIf()
    {
        FixedNode __currentNext = next();
        setNext(null);
        DeoptimizeNode __deopt = graph().add(new DeoptimizeNode(this.___action, this.___reason, this.___speculation));
        __deopt.setStateBefore(stateBefore());
        IfNode __ifNode;
        AbstractBeginNode __noDeoptSuccessor;
        if (this.___negated)
        {
            __ifNode = graph().add(new IfNode(this.___condition, __deopt, __currentNext, 0));
            __noDeoptSuccessor = __ifNode.falseSuccessor();
        }
        else
        {
            __ifNode = graph().add(new IfNode(this.___condition, __currentNext, __deopt, 1));
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
        this.___action = __action;
    }

    @Override
    public void setReason(DeoptimizationReason __reason)
    {
        this.___reason = __reason;
    }
}
