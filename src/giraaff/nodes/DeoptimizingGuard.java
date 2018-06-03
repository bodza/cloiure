package giraaff.nodes;

import giraaff.nodes.extended.GuardingNode;

///
// Shared interface to capture core methods of {@link AbstractFixedGuardNode} and {@link GuardNode}.
///
// @iface DeoptimizingGuard
public interface DeoptimizingGuard extends GuardingNode, StaticDeoptimizingNode
{
    LogicNode getCondition();

    void setCondition(LogicNode __x, boolean __negated);

    boolean isNegated();
}
