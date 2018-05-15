package graalvm.compiler.truffle.runtime;

import java.lang.reflect.Method;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.truffle.runtime.OptimizedOSRLoopNode.OSRRootNode;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import jdk.vm.ci.code.stack.InspectedFrame;

public final class GraalFrameInstance implements FrameInstance {

    private static final int CALL_TARGET_INDEX = 0;
    private static final int CALL_TARGET_FRAME_INDEX = 1;

    private static final int CALL_NODE_NOTIFY_INDEX = 0;

    public static final Method CALL_TARGET_METHOD;
    public static final Method CALL_NODE_METHOD;
    public static final Method CALL_OSR_METHOD;

    static {
        try {
            CALL_NODE_METHOD = OptimizedDirectCallNode.class.getDeclaredMethod("callProxy", Node.class, CallTarget.class, Object[].class, boolean.class);
            CALL_TARGET_METHOD = OptimizedCallTarget.class.getDeclaredMethod("callProxy", VirtualFrame.class);
            CALL_OSR_METHOD = OptimizedOSRLoopNode.OSRRootNode.class.getDeclaredMethod("callProxy", OSRRootNode.class, VirtualFrame.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new GraalError(e);
        }
    }

    private final InspectedFrame callTargetFrame;
    private final InspectedFrame callNodeFrame;

    public GraalFrameInstance(InspectedFrame callTargetFrame, InspectedFrame callNodeFrame) {
        this.callTargetFrame = callTargetFrame;
        this.callNodeFrame = callNodeFrame;
    }

    @SuppressWarnings("deprecation")
    @Override
    @TruffleBoundary
    public Frame getFrame(FrameAccess access) {
        if (access == FrameAccess.NONE) {
            return null;
        }

        if (access == FrameAccess.READ_WRITE || access == FrameAccess.MATERIALIZE) {
            if (callTargetFrame.isVirtual(CALL_TARGET_FRAME_INDEX)) {
                callTargetFrame.materializeVirtualObjects(false);
            }
        }
        Frame frame = (Frame) callTargetFrame.getLocal(CALL_TARGET_FRAME_INDEX);
        if (access == FrameAccess.MATERIALIZE) {
            frame = frame.materialize();
        }
        return frame;
    }

    @Override
    public boolean isVirtualFrame() {
        return callTargetFrame.isVirtual(CALL_TARGET_FRAME_INDEX);
    }

    @Override
    public CallTarget getCallTarget() {
        return (CallTarget) callTargetFrame.getLocal(CALL_TARGET_INDEX);
    }

    @Override
    public Node getCallNode() {
        if (callNodeFrame != null) {
            Object receiver = callNodeFrame.getLocal(CALL_NODE_NOTIFY_INDEX);
            if (receiver instanceof DirectCallNode || receiver instanceof IndirectCallNode) {
                return (Node) receiver;
            }
        }
        return null;
    }

}