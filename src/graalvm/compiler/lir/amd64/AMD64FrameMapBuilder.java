package graalvm.compiler.lir.amd64;

import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilderImpl;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;

public class AMD64FrameMapBuilder extends FrameMapBuilderImpl {

    public AMD64FrameMapBuilder(FrameMap frameMap, CodeCacheProvider codeCache, RegisterConfig registerConfig) {
        super(frameMap, codeCache, registerConfig);
    }

    /**
     * For non-leaf methods, RBP is preserved in the special stack slot required by the HotSpot
     * runtime for walking/inspecting frames of such methods.
     */
    public StackSlot allocateRBPSpillSlot() {
        return ((AMD64FrameMap) getFrameMap()).allocateRBPSpillSlot();
    }

    public void freeRBPSpillSlot() {
        ((AMD64FrameMap) getFrameMap()).freeRBPSpillSlot();
    }

    public StackSlot allocateDeoptimizationRescueSlot() {
        return ((AMD64FrameMap) getFrameMap()).allocateDeoptimizationRescueSlot();
    }
}
