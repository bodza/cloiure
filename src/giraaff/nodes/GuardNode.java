package giraaff.nodes;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.GuardingNode;

///
// A guard is a node that deoptimizes based on a conditional expression. Guards are not attached to
// a certain frame state, they can move around freely and will always use the correct frame state
// when the nodes are scheduled (i.e., the last emitted frame state). The node that is guarded has a
// data dependency on the guard and the guard in turn has a data dependency on the condition. A
// guard may only be executed if it is guaranteed that the guarded node is executed too (if no
// exceptions are thrown). Therefore, an anchor is placed after a control flow split and the guard
// has a data dependency to the anchor. The anchor is the most distant node that is post-dominated
// by the guarded node and the guard can be scheduled anywhere between those two nodes. This ensures
// maximum flexibility for the guard node and guarantees that deoptimization occurs only if the
// control flow would have reached the guarded node (without taking exceptions into account).
///
// @NodeInfo.allowedUsageTypes "Guard"
// @class GuardNode
public final class GuardNode extends FloatingAnchoredNode implements Canonicalizable, GuardingNode, DeoptimizingGuard, IterableNodeType
{
    // @def
    public static final NodeClass<GuardNode> TYPE = NodeClass.create(GuardNode.class);

    @Input(InputType.Condition)
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

    // @cons
    public GuardNode(LogicNode __condition, AnchoringNode __anchor, DeoptimizationReason __reason, DeoptimizationAction __action, boolean __negated, JavaConstant __speculation)
    {
        this(TYPE, __condition, __anchor, __reason, __action, __negated, __speculation);
    }

    // @cons
    protected GuardNode(NodeClass<? extends GuardNode> __c, LogicNode __condition, AnchoringNode __anchor, DeoptimizationReason __reason, DeoptimizationAction __action, boolean __negated, JavaConstant __speculation)
    {
        super(__c, StampFactory.forVoid(), __anchor);
        this.___condition = __condition;
        this.___reason = __reason;
        this.___action = __action;
        this.___negated = __negated;
        this.___speculation = __speculation;
    }

    ///
    // The instruction that produces the tested boolean value.
    ///
    @Override
    public LogicNode getCondition()
    {
        return this.___condition;
    }

    @Override
    public void setCondition(LogicNode __x, boolean __negated)
    {
        updateUsages(this.___condition, __x);
        this.___condition = __x;
        this.___negated = __negated;
    }

    @Override
    public boolean isNegated()
    {
        return this.___negated;
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

    public void setSpeculation(JavaConstant __speculation)
    {
        this.___speculation = __speculation;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (getCondition() instanceof LogicNegationNode)
        {
            LogicNegationNode __negation = (LogicNegationNode) getCondition();
            return new GuardNode(__negation.getValue(), getAnchor(), this.___reason, this.___action, !this.___negated, this.___speculation);
        }
        if (getCondition() instanceof LogicConstantNode)
        {
            LogicConstantNode __c = (LogicConstantNode) getCondition();
            if (__c.getValue() != this.___negated)
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
        this.___negated = !this.___negated;
    }

    @Override
    public void setAction(DeoptimizationAction __invalidaterecompile)
    {
        this.___action = __invalidaterecompile;
    }

    @Override
    public void setReason(DeoptimizationReason __reason)
    {
        this.___reason = __reason;
    }
}
