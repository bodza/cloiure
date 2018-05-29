package giraaff.lir.amd64;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;

import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilderImpl;

// @class AMD64FrameMapBuilder
public final class AMD64FrameMapBuilder extends FrameMapBuilderImpl
{
    // @cons
    public AMD64FrameMapBuilder(FrameMap frameMap, CodeCacheProvider codeCache, RegisterConfig registerConfig)
    {
        super(frameMap, codeCache, registerConfig);
    }

    /**
     * For non-leaf methods, RBP is preserved in the special stack slot required by the HotSpot
     * runtime for walking/inspecting frames of such methods.
     */
    public StackSlot allocateRBPSpillSlot()
    {
        return ((AMD64FrameMap) getFrameMap()).allocateRBPSpillSlot();
    }

    public void freeRBPSpillSlot()
    {
        ((AMD64FrameMap) getFrameMap()).freeRBPSpillSlot();
    }
}
