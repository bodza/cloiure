package graalvm.compiler.hotspot;

import static jdk.vm.ci.code.BytecodeFrame.isPlaceholderBci;

import java.util.ArrayList;
import java.util.List;

import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.core.gen.DebugInfoBuilder;
import graalvm.compiler.graph.GraalGraphError;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeValueMap;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.StackLockValue;
import jdk.vm.ci.code.VirtualObject;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Extends {@link DebugInfoBuilder} to allocate the extra debug information required for locks.
 */
public class HotSpotDebugInfoBuilder extends DebugInfoBuilder {

    private final HotSpotLockStack lockStack;

    private int maxInterpreterFrameSize;

    private HotSpotCodeCacheProvider codeCacheProvider;

    public HotSpotDebugInfoBuilder(NodeValueMap nodeValueMap, HotSpotLockStack lockStack, HotSpotLIRGenerator gen) {
        super(nodeValueMap, gen.getResult().getLIR().getDebug());
        this.lockStack = lockStack;
        this.codeCacheProvider = gen.getProviders().getCodeCache();
    }

    public HotSpotLockStack lockStack() {
        return lockStack;
    }

    public int maxInterpreterFrameSize() {
        return maxInterpreterFrameSize;
    }

    @Override
    protected JavaValue computeLockValue(FrameState state, int lockIndex) {
        int lockDepth = lockIndex;
        if (state.outerFrameState() != null) {
            lockDepth += state.outerFrameState().nestedLockDepth();
        }
        VirtualStackSlot slot = lockStack.makeLockSlot(lockDepth);
        ValueNode lock = state.lockAt(lockIndex);
        JavaValue object = toJavaValue(lock);
        boolean eliminated = object instanceof VirtualObject || state.monitorIdAt(lockIndex).isEliminated();
        assert state.monitorIdAt(lockIndex).getLockDepth() == lockDepth;
        return new StackLockValue(object, slot, eliminated);
    }

    @Override
    protected BytecodeFrame computeFrameForState(FrameState state) {
        if (isPlaceholderBci(state.bci) && state.bci != BytecodeFrame.BEFORE_BCI) {
            raiseInvalidFrameStateError(state);
        }
        BytecodeFrame result = super.computeFrameForState(state);
        maxInterpreterFrameSize = Math.max(maxInterpreterFrameSize, codeCacheProvider.interpreterFrameSize(result));
        return result;
    }

    protected void raiseInvalidFrameStateError(FrameState state) throws GraalGraphError {
        // This is a hard error since an incorrect state could crash hotspot
        NodeSourcePosition sourcePosition = state.getNodeSourcePosition();
        List<String> context = new ArrayList<>();
        ResolvedJavaMethod replacementMethodWithProblematicSideEffect = null;
        if (sourcePosition != null) {
            NodeSourcePosition pos = sourcePosition;
            while (pos != null) {
                StringBuilder sb = new StringBuilder("parsing ");
                ResolvedJavaMethod method = pos.getMethod();
                MetaUtil.appendLocation(sb, method, pos.getBCI());
                if (method.getAnnotation(MethodSubstitution.class) != null ||
                                method.getAnnotation(Snippet.class) != null) {
                    replacementMethodWithProblematicSideEffect = method;
                }
                context.add(sb.toString());
                pos = pos.getCaller();
            }
        }
        String message = "Invalid frame state " + state;
        if (replacementMethodWithProblematicSideEffect != null) {
            message += " associated with a side effect in " + replacementMethodWithProblematicSideEffect.format("%H.%n(%p)") + " at a position that cannot be deoptimized to";
        }
        GraalGraphError error = new GraalGraphError(message);
        for (String c : context) {
            error.addContext(c);
        }
        throw error;
    }
}
