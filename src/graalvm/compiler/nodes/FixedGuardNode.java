package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Guard;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

@NodeInfo(nameTemplate = "FixedGuard(!={p#negated}) {p#reason/s}", allowedUsageTypes = Guard, size = SIZE_2, cycles = CYCLES_2)
public final class FixedGuardNode extends AbstractFixedGuardNode implements Lowerable, IterableNodeType
{
    public static final NodeClass<FixedGuardNode> TYPE = NodeClass.create(FixedGuardNode.class);

    public FixedGuardNode(LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action)
    {
        this(condition, deoptReason, action, JavaConstant.NULL_POINTER, false);
    }

    public FixedGuardNode(LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, boolean negated)
    {
        this(condition, deoptReason, action, JavaConstant.NULL_POINTER, negated);
    }

    public FixedGuardNode(LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, JavaConstant speculation, boolean negated)
    {
        super(TYPE, condition, deoptReason, action, speculation, negated);
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        super.simplify(tool);

        if (getCondition() instanceof LogicConstantNode)
        {
            LogicConstantNode c = (LogicConstantNode) getCondition();
            if (c.getValue() == isNegated())
            {
                FixedNode currentNext = this.next();
                if (currentNext != null)
                {
                    tool.deleteBranch(currentNext);
                }

                DeoptimizeNode deopt = graph().add(new DeoptimizeNode(getAction(), getReason(), getSpeculation()));
                deopt.setStateBefore(stateBefore());
                setNext(deopt);
            }
            this.replaceAtUsages(null);
            graph().removeFixed(this);
        }
        else if (getCondition() instanceof ShortCircuitOrNode)
        {
            ShortCircuitOrNode shortCircuitOr = (ShortCircuitOrNode) getCondition();
            if (isNegated() && hasNoUsages())
            {
                graph().addAfterFixed(this, graph().add(new FixedGuardNode(shortCircuitOr.getY(), getReason(), getAction(), getSpeculation(), !shortCircuitOr.isYNegated())));
                graph().replaceFixedWithFixed(this, graph().add(new FixedGuardNode(shortCircuitOr.getX(), getReason(), getAction(), getSpeculation(), !shortCircuitOr.isXNegated())));
            }
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage().allowsFloatingGuards())
        {
            if (getAction() != DeoptimizationAction.None)
            {
                ValueNode guard = tool.createGuard(this, getCondition(), getReason(), getAction(), getSpeculation(), isNegated()).asNode();
                this.replaceAtUsages(guard);
                graph().removeFixed(this);
            }
        }
        else
        {
            lowerToIf().lower(tool);
        }
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }
}
