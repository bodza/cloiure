package giraaff.phases.schedule;

import giraaff.nodes.cfg.Block;

/**
 * The {@code BlockClosure} interface represents a closure for iterating over blocks.
 */
// @iface BlockClosure
public interface BlockClosure
{
    void apply(Block block);
}
