package graalvm.compiler.truffle.runtime.hotspot;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleStackTraceLimit;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleTransferToInterpreter;
import static graalvm.compiler.truffle.runtime.hotspot.UnsafeAccess.UNSAFE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import graalvm.compiler.api.runtime.GraalRuntime;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.TTY;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotGraalOptionValues;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.serviceprovider.GraalServices;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompiler;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompiler.Factory;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompilerRuntime;
import graalvm.compiler.truffle.common.hotspot.HotSpotTruffleInstalledCode;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.TruffleCallBoundary;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import jdk.vm.ci.code.stack.StackIntrospection;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotSpeculationLog;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.runtime.JVMCI;

/**
 * HotSpot specific implementation of a Graal-enabled Truffle runtime.
 */
public final class HotSpotTruffleRuntime extends GraalTruffleRuntime implements HotSpotTruffleCompilerRuntime {

    static class Lazy extends BackgroundCompileQueue {
        StackIntrospection stackIntrospection;

        Lazy(HotSpotTruffleRuntime runtime) {
            runtime.installDefaultListeners();
        }
    }

    public HotSpotTruffleRuntime(Supplier<GraalRuntime> graalRuntimeSupplier) {
        super(graalRuntimeSupplier, Arrays.asList(HotSpotOptimizedCallTarget.class));
        setDontInlineCallBoundaryMethod();
    }

    @Override
    public OptionValues getInitialOptions() {
        return HotSpotGraalOptionValues.HOTSPOT_OPTIONS;
    }

    private volatile Lazy lazy;

    private Lazy lazy() {
        if (lazy == null) {
            synchronized (this) {
                if (lazy == null) {
                    lazy = new Lazy(this);
                }
            }
        }
        return lazy;
    }

    private List<ResolvedJavaMethod> truffleCallBoundaryMethods;

    @Override
    public synchronized Iterable<ResolvedJavaMethod> getTruffleCallBoundaryMethods() {
        if (truffleCallBoundaryMethods == null) {
            truffleCallBoundaryMethods = new ArrayList<>();
            MetaAccessProvider metaAccess = getMetaAccess();
            ResolvedJavaType type = metaAccess.lookupJavaType(OptimizedCallTarget.class);
            for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
                if (method.getAnnotation(TruffleCallBoundary.class) != null) {
                    truffleCallBoundaryMethods.add(method);
                }
            }
        }
        return truffleCallBoundaryMethods;
    }

    @Override
    protected StackIntrospection getStackIntrospection() {
        Lazy l = lazy();
        if (l.stackIntrospection == null) {
            l.stackIntrospection = HotSpotJVMCIRuntime.runtime().getHostJVMCIBackend().getStackIntrospection();
        }
        return l.stackIntrospection;
    }

    @Override
    public HotSpotTruffleCompiler getTruffleCompiler() {
        if (truffleCompiler == null) {
            initializeTruffleCompiler();
        }
        return (HotSpotTruffleCompiler) truffleCompiler;
    }

    protected boolean reportedTruffleCompilerInitializationFailure;

    private void initializeTruffleCompiler() {
        synchronized (this) {
            // might occur for multiple compiler threads at the same time.
            if (truffleCompiler == null) {
                try {
                    truffleCompiler = newTruffleCompiler();
                } catch (Throwable e) {
                    if (!reportedTruffleCompilerInitializationFailure) {
                        // This should never happen so report it (once)
                        reportedTruffleCompilerInitializationFailure = true;
                        e.printStackTrace(TTY.out);
                    }
                }
            }
        }
    }

    @Override
    public HotSpotTruffleCompiler newTruffleCompiler() {
        final Factory factory = GraalServices.loadSingle(HotSpotTruffleCompiler.Factory.class, true);
        return factory.create(this);
    }

    @Override
    public OptimizedCallTarget createOptimizedCallTarget(OptimizedCallTarget source, RootNode rootNode) {
        return new HotSpotOptimizedCallTarget(source, rootNode, HotSpotTruffleCompiler.INVALID_CODE);
    }

    @Override
    public void onCodeInstallation(HotSpotTruffleInstalledCode installedCode) {
        HotSpotOptimizedCallTarget callTarget = (HotSpotOptimizedCallTarget) installedCode.getCompilable();
        callTarget.setInstalledCode(installedCode);
    }

    @Override
    public SpeculationLog createSpeculationLog() {
        return new HotSpotSpeculationLog();
    }

    /**
     * Prevents C1 or C2 from inlining a call to a method annotated by {@link TruffleCallBoundary}
     * so that we never miss the chance to switch from the Truffle interpreter to compiled code.
     *
     * @see HotSpotTruffleCompiler#installTruffleCallBoundaryMethods()
     */
    public static void setDontInlineCallBoundaryMethod() {
        MetaAccessProvider metaAccess = getMetaAccess();
        ResolvedJavaType type = metaAccess.lookupJavaType(OptimizedCallTarget.class);
        for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
            if (method.getAnnotation(TruffleCallBoundary.class) != null) {
                setNotInlinableOrCompilable(method);
            }
        }
    }

    static MetaAccessProvider getMetaAccess() {
        return JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
    }

    /**
     * Informs the VM to never compile or inline {@code method}.
     */
    private static void setNotInlinableOrCompilable(ResolvedJavaMethod method) {
        // JDK-8180487 and JDK-8186478 introduced breaking API changes so reflection is required.
        Method[] methods = HotSpotResolvedJavaMethod.class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals("setNotInlineable") || m.getName().equals("setNotInlinableOrCompilable") || m.getName().equals("setNotInlineableOrCompileable")) {
                try {
                    m.invoke(method);
                    return;
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new GraalError(e);
                }
            }
        }
        throw new GraalError("Could not find setNotInlineable, setNotInlinableOrCompilable or setNotInlineableOrCompileable in %s", HotSpotResolvedJavaMethod.class);
    }

    private GraalHotSpotVMConfig getVMConfig() {
        return ((HotSpotGraalRuntimeProvider) getGraalRuntime()).getVMConfig();
    }

    @Override
    protected BackgroundCompileQueue getCompileQueue() {
        return lazy();
    }

    @Override
    public boolean cancelInstalledTask(OptimizedCallTarget optimizedCallTarget, Object source, CharSequence reason) {
        if (lazy == null) {
            // if Truffle wasn't initialized yet, this is a noop
            return false;
        }

        return super.cancelInstalledTask(optimizedCallTarget, source, reason);
    }

    @SuppressWarnings("try")
    @Override
    public void bypassedInstalledCode() {
        getTruffleCompiler().installTruffleCallBoundaryMethods();
    }

    @Override
    protected CallMethods getCallMethods() {
        if (callMethods == null) {
            lookupCallMethods(getMetaAccess());
        }
        return callMethods;
    }

    @Override
    public void notifyTransferToInterpreter() {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleCompilerOptions.getValue(TraceTruffleTransferToInterpreter)) {
            TraceTransferToInterpreterHelper.traceTransferToInterpreter(this, getVMConfig());
        }
    }

    private static class TraceTransferToInterpreterHelper {
        private static final long THREAD_EETOP_OFFSET;

        static {
            try {
                THREAD_EETOP_OFFSET = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("eetop"));
            } catch (Exception e) {
                throw new GraalError(e);
            }
        }

        static void traceTransferToInterpreter(HotSpotTruffleRuntime runtime, GraalHotSpotVMConfig config) {
            long thread = UNSAFE.getLong(Thread.currentThread(), THREAD_EETOP_OFFSET);
            long pendingTransferToInterpreterAddress = thread + config.pendingTransferToInterpreterOffset;
            boolean deoptimized = UNSAFE.getByte(pendingTransferToInterpreterAddress) != 0;
            if (deoptimized) {
                logTransferToInterpreter(runtime);
                UNSAFE.putByte(pendingTransferToInterpreterAddress, (byte) 0);
            }
        }

        private static String formatStackFrame(FrameInstance frameInstance, CallTarget target) {
            StringBuilder builder = new StringBuilder();
            if (target instanceof RootCallTarget) {
                RootNode root = ((RootCallTarget) target).getRootNode();
                String name = root.getName();
                if (name == null) {
                    builder.append("unnamed-root");
                } else {
                    builder.append(name);
                }
                Node callNode = frameInstance.getCallNode();
                SourceSection sourceSection = null;
                if (callNode != null) {
                    sourceSection = callNode.getEncapsulatingSourceSection();
                }
                if (sourceSection == null) {
                    sourceSection = root.getSourceSection();
                }

                if (sourceSection == null || sourceSection.getSource() == null) {
                    builder.append("(Unknown)");
                } else {
                    builder.append("(").append(formatPath(sourceSection)).append(":").append(sourceSection.getStartLine()).append(")");
                }

                if (target instanceof OptimizedCallTarget) {
                    OptimizedCallTarget callTarget = ((OptimizedCallTarget) target);
                    if (callTarget.isValid()) {
                        builder.append(" <opt>");
                    }
                    if (callTarget.getSourceCallTarget() != null) {
                        builder.append(" <split-" + Integer.toHexString(callTarget.hashCode()) + ">");
                    }
                }

            } else {
                builder.append(target.toString());
            }
            return builder.toString();
        }

        private static String formatPath(SourceSection sourceSection) {
            if (sourceSection.getSource().getPath() != null) {
                Path path = FileSystems.getDefault().getPath(".").toAbsolutePath();
                Path filePath = FileSystems.getDefault().getPath(sourceSection.getSource().getPath()).toAbsolutePath();

                try {
                    return path.relativize(filePath).toString();
                } catch (IllegalArgumentException e) {
                    // relativization failed
                }
            }
            return sourceSection.getSource().getName();
        }

        private static void logTransferToInterpreter(final HotSpotTruffleRuntime runtime) {
            final int limit = TruffleCompilerOptions.getValue(TraceTruffleStackTraceLimit);

            runtime.log("[truffle] transferToInterpreter at");
            runtime.iterateFrames(new FrameInstanceVisitor<Object>() {
                int frameIndex = 0;

                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    CallTarget target = frameInstance.getCallTarget();
                    StringBuilder line = new StringBuilder("  ");
                    if (frameIndex > 0) {
                        line.append("  ");
                    }
                    line.append(formatStackFrame(frameInstance, target));
                    frameIndex++;

                    runtime.log(line.toString());
                    if (frameIndex < limit) {
                        return null;
                    } else {
                        runtime.log("    ...");
                        return frameInstance;
                    }
                }

            });
            final int skip = 3;

            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            String suffix = stackTrace.length > skip + limit ? "\n    ..." : "";
            runtime.log(Arrays.stream(stackTrace).skip(skip).limit(limit).map(StackTraceElement::toString).collect(Collectors.joining("\n    ", "  ", suffix)));
        }
    }
}
