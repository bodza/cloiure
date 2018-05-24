package giraaff.nodes.extended;

import giraaff.nodes.ValueNodeInterface;

/**
 * A node that may be guarded by a {@linkplain GuardingNode guarding node}.
 */
public interface GuardedNode extends ValueNodeInterface
{
    GuardingNode getGuard();

    void setGuard(GuardingNode guard);
}