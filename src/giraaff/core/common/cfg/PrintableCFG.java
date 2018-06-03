package giraaff.core.common.cfg;

import java.util.function.BiConsumer;

///
// Represents a control-flow graph where each node can be annotated with arbitrary property pairs of
// the form ({@linkplain String name}, {@linkplain String value}).
///
// @iface PrintableCFG
public interface PrintableCFG
{
    AbstractBlockBase<?>[] getBlocks();

    ///
    // Applies {@code action} to all extra property pairs (name, value) of {@code block}.
    //
    // @param block a block from {@link #getBlocks()}.
    // @param action a {@link BiConsumer consumer}.
    ///
    default void forEachPropertyPair(AbstractBlockBase<?> __block, BiConsumer<String, String> __action)
    {
        // no extra properties per default
    }
}
