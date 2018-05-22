package giraaff.nodes;

import giraaff.nodes.extended.GuardingNode;

/**
 * Shared interface to capture core methods of {@link AbstractFixedGuardNode} and {@link GuardNode}.
 *
 */
public interface DeoptimizingGuard extends GuardingNode, StaticDeoptimizingNode
{
    LogicNode getCondition();

    void setCondition(LogicNode x, boolean negated);

    boolean isNegated();
}
