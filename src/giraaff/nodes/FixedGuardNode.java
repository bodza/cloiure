package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "Guard"
// @class FixedGuardNode
public final class FixedGuardNode extends AbstractFixedGuardNode implements Lowerable, IterableNodeType
{
    // @def
    public static final NodeClass<FixedGuardNode> TYPE = NodeClass.create(FixedGuardNode.class);

    // @cons
    public FixedGuardNode(LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action)
    {
        this(__condition, __deoptReason, __action, JavaConstant.NULL_POINTER, false);
    }

    // @cons
    public FixedGuardNode(LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, boolean __negated)
    {
        this(__condition, __deoptReason, __action, JavaConstant.NULL_POINTER, __negated);
    }

    // @cons
    public FixedGuardNode(LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, JavaConstant __speculation, boolean __negated)
    {
        super(TYPE, __condition, __deoptReason, __action, __speculation, __negated);
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        super.simplify(__tool);

        if (getCondition() instanceof LogicConstantNode)
        {
            LogicConstantNode __c = (LogicConstantNode) getCondition();
            if (__c.getValue() == isNegated())
            {
                FixedNode __currentNext = this.next();
                if (__currentNext != null)
                {
                    __tool.deleteBranch(__currentNext);
                }

                DeoptimizeNode __deopt = graph().add(new DeoptimizeNode(getAction(), getReason(), getSpeculation()));
                __deopt.setStateBefore(stateBefore());
                setNext(__deopt);
            }
            this.replaceAtUsages(null);
            graph().removeFixed(this);
        }
        else if (getCondition() instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode __shortCircuitOr = (ShortCircuitOrNode) getCondition();
            if (isNegated() && hasNoUsages())
            {
                graph().addAfterFixed(this, graph().add(new FixedGuardNode(__shortCircuitOr.getY(), getReason(), getAction(), getSpeculation(), !__shortCircuitOr.isYNegated())));
                graph().replaceFixedWithFixed(this, graph().add(new FixedGuardNode(__shortCircuitOr.getX(), getReason(), getAction(), getSpeculation(), !__shortCircuitOr.isXNegated())));
            }
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().allowsFloatingGuards())
        {
            if (getAction() != DeoptimizationAction.None)
            {
                ValueNode __guard = __tool.createGuard(this, getCondition(), getReason(), getAction(), getSpeculation(), isNegated()).asNode();
                this.replaceAtUsages(__guard);
                graph().removeFixed(this);
            }
        }
        else
        {
            lowerToIf().lower(__tool);
        }
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
