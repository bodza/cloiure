package graalvm.compiler.nodes.extended;

import graalvm.compiler.nodes.ValueNodeInterface;

/**
 * A node that may be guarded by a {@linkplain GuardingNode guarding node}.
 */
public interface GuardedNode extends ValueNodeInterface {

    GuardingNode getGuard();

    void setGuard(GuardingNode guard);
}
