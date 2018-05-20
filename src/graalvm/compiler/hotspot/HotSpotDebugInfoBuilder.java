package graalvm.compiler.hotspot;

import static jdk.vm.ci.code.BytecodeFrame.isPlaceholderBci;

import graalvm.compiler.core.gen.DebugInfoBuilder;
import graalvm.compiler.graph.GraalGraphError;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeValueMap;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.StackLockValue;
import jdk.vm.ci.code.VirtualObject;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.JavaValue;

/**
 * Extends {@link DebugInfoBuilder} to allocate the extra debug information required for locks.
 */
public class HotSpotDebugInfoBuilder extends DebugInfoBuilder
{
    private final HotSpotLockStack lockStack;

    private int maxInterpreterFrameSize;

    private HotSpotCodeCacheProvider codeCacheProvider;

    public HotSpotDebugInfoBuilder(NodeValueMap nodeValueMap, HotSpotLockStack lockStack, HotSpotLIRGenerator gen)
    {
        super(nodeValueMap);
        this.lockStack = lockStack;
        this.codeCacheProvider = gen.getProviders().getCodeCache();
    }

    public HotSpotLockStack lockStack()
    {
        return lockStack;
    }

    public int maxInterpreterFrameSize()
    {
        return maxInterpreterFrameSize;
    }

    @Override
    protected JavaValue computeLockValue(FrameState state, int lockIndex)
    {
        int lockDepth = lockIndex;
        if (state.outerFrameState() != null)
        {
            lockDepth += state.outerFrameState().nestedLockDepth();
        }
        VirtualStackSlot slot = lockStack.makeLockSlot(lockDepth);
        ValueNode lock = state.lockAt(lockIndex);
        JavaValue object = toJavaValue(lock);
        boolean eliminated = object instanceof VirtualObject || state.monitorIdAt(lockIndex).isEliminated();
        return new StackLockValue(object, slot, eliminated);
    }

    @Override
    protected BytecodeFrame computeFrameForState(FrameState state)
    {
        if (isPlaceholderBci(state.bci) && state.bci != BytecodeFrame.BEFORE_BCI)
        {
            raiseInvalidFrameStateError(state);
        }
        BytecodeFrame result = super.computeFrameForState(state);
        maxInterpreterFrameSize = Math.max(maxInterpreterFrameSize, codeCacheProvider.interpreterFrameSize(result));
        return result;
    }

    protected void raiseInvalidFrameStateError(FrameState state) throws GraalGraphError
    {
        // This is a hard error since an incorrect state could crash hotspot
        throw new GraalGraphError("Invalid frame state " + state);
    }
}
