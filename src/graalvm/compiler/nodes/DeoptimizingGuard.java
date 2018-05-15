package graalvm.compiler.nodes;

import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.nodes.extended.GuardingNode;

/**
 * Shared interface to capture core methods of {@link AbstractFixedGuardNode} and {@link GuardNode}.
 *
 */
public interface DeoptimizingGuard extends GuardingNode, StaticDeoptimizingNode {

    LogicNode getCondition();

    void setCondition(LogicNode x, boolean negated);

    boolean isNegated();

    NodeSourcePosition getNoDeoptSuccessorPosition();

    void setNoDeoptSuccessorPosition(NodeSourcePosition noDeoptSuccessorPosition);

    default void addCallerToNoDeoptSuccessorPosition(NodeSourcePosition caller) {
        NodeSourcePosition noDeoptSuccessorPosition = getNoDeoptSuccessorPosition();
        if (noDeoptSuccessorPosition == null) {
            return;
        }
        setNoDeoptSuccessorPosition(new NodeSourcePosition(caller, noDeoptSuccessorPosition.getMethod(), noDeoptSuccessorPosition.getBCI()));
    }
}
