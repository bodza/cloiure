package graalvm.compiler.truffle.runtime;

import java.util.function.Supplier;

import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.impl.TVMCI;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

final class GraalTVMCI extends TVMCI {

    @Override
    public void onLoopCount(Node source, int count) {
        Node node = source;
        Node parentNode = source != null ? source.getParent() : null;
        while (node != null) {
            if (node instanceof OptimizedOSRLoopNode) {
                ((OptimizedOSRLoopNode) node).reportChildLoopCount(count);
            }
            parentNode = node;
            node = node.getParent();
        }
        if (parentNode != null && parentNode instanceof RootNode) {
            CallTarget target = ((RootNode) parentNode).getCallTarget();
            if (target instanceof OptimizedCallTarget) {
                ((OptimizedCallTarget) target).onLoopCount(count);
            }
        }
    }

    @Override
    protected boolean isGuestCallStackFrame(StackTraceElement e) {
        return e.getMethodName().equals(OptimizedCallTarget.CALL_BOUNDARY_METHOD_NAME) && e.getClassName().equals(OptimizedCallTarget.class.getName());
    }

    /**
     * Initializes the argument profile with a custom profile without calling it. A call target must
     * never be called prior initialization of argument types. Also the argument types must be final
     * if used in combination with {@link #callProfiled(CallTarget, Object...)}.
     */
    @Override
    protected void initializeProfile(CallTarget target, Class<?>[] argumentTypes) {
        ((OptimizedCallTarget) target).getCompilationProfile().initializeArgumentTypes(argumentTypes);
    }

    /**
     * Call without verifying the argument profile. Needs to be initialized by
     * {@link #initializeProfile(CallTarget, Class[])}. Potentially crashes the VM if the argument
     * profile is incompatible with the actual arguments. Use with caution.
     */
    @Override
    protected Object callProfiled(CallTarget target, Object... args) {
        OptimizedCallTarget castTarget = (OptimizedCallTarget) target;
        assert castTarget.compilationProfile != null && castTarget.compilationProfile.isValidArgumentProfile(args) : "Invalid argument profile. UnsafeCalls need to explicity initialize the profile.";
        return castTarget.doInvoke(args);
    }

    @Override
    protected OptionDescriptors getCompilerOptionDescriptors() {
        return PolyglotCompilerOptions.getDescriptors();
    }

    @Override
    protected OptionValues getCompilerOptionValues(RootNode rootNode) {
        return super.getCompilerOptionValues(rootNode);
    }

    void onFirstExecution(OptimizedCallTarget callTarget) {
        super.onFirstExecution(callTarget.getRootNode());
    }

    @Override
    protected void onLoad(RootNode rootNode) {
        super.onLoad(rootNode);
    }

    @Override
    protected void markFrameMaterializeCalled(FrameDescriptor descriptor) {
        super.markFrameMaterializeCalled(descriptor);
    }

    @Override
    protected void onThrowable(Node callNode, RootCallTarget root, Throwable e, Frame frame) {
        super.onThrowable(callNode, root, e, frame);
    }

    @Override
    protected boolean getFrameMaterializeCalled(FrameDescriptor descriptor) {
        return super.getFrameMaterializeCalled(descriptor);
    }

    @Override
    public RootNode cloneUninitialized(RootNode root) {
        return super.cloneUninitialized(root);
    }

    @Override
    protected int adoptChildrenAndCount(RootNode root) {
        return super.adoptChildrenAndCount(root);
    }

    @Override
    public boolean isCloneUninitializedSupported(RootNode root) {
        return super.isCloneUninitializedSupported(root);
    }

    @Override
    protected <T> T getOrCreateRuntimeData(RootNode rootNode, Supplier<T> constructor) {
        return super.getOrCreateRuntimeData(rootNode, constructor);
    }

    /**
     * Class used to store data used by the compiler in the Engine. Enables "global" compiler state
     * per engine.
     */
    static class EngineData {
        int splitLimit;
        int splitCount;
    }

    EngineData getEngineData(RootNode rootNode) {
        return getOrCreateRuntimeData(rootNode, new Supplier<EngineData>() {
            @Override
            public EngineData get() {
                return new EngineData();
            }
        });
    }

    @Override
    protected void reportPolymorphicSpecialize(Node source) {
        if (TruffleCompilerOptions.getValue(TruffleCompilerOptions.TruffleExperimentalSplitting)) {
            TruffleSplittingStrategy.newPolymorphicSpecialize(source);
            final RootNode rootNode = source.getRootNode();
            final OptimizedCallTarget callTarget = rootNode == null ? null : (OptimizedCallTarget) rootNode.getCallTarget();
            if (callTarget != null) {
                callTarget.polymorphicSpecialize(source);
            }
        }
    }
}
