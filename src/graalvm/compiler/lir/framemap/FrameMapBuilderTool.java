package graalvm.compiler.lir.framemap;

import java.util.List;

import graalvm.compiler.lir.VirtualStackSlot;

/**
 * A {@link FrameMapBuilder} that allows access to the underlying {@link FrameMap}.
 */
public abstract class FrameMapBuilderTool extends FrameMapBuilder {

    /**
     * Returns the number of {@link VirtualStackSlot}s created by this {@link FrameMapBuilder}. Can
     * be used as an upper bound for an array indexed by {@link VirtualStackSlot#getId()}.
     */
    public abstract int getNumberOfStackSlots();

    public abstract List<VirtualStackSlot> getStackSlots();

    public abstract FrameMap getFrameMap();

}
