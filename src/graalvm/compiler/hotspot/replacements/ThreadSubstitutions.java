package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.JAVA_THREAD_OSTHREAD_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.JAVA_THREAD_THREAD_OBJECT_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.osThreadInterruptedOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.osThreadOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.threadObjectOffset;
import static org.graalvm.word.LocationIdentity.any;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.nodes.CurrentJavaThreadNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.word.Word;

/**
 * Substitutions for {@link java.lang.Thread} methods.
 */
@ClassSubstitution(Thread.class)
public class ThreadSubstitutions {

    /**
     * hidden in 9.
     */
    @MethodSubstitution(isStatic = false, optional = true)
    public static boolean isInterrupted(final Thread thisObject, boolean clearInterrupted) {
        Word javaThread = CurrentJavaThreadNode.get();
        Object thread = javaThread.readObject(threadObjectOffset(INJECTED_VMCONFIG), JAVA_THREAD_THREAD_OBJECT_LOCATION);
        if (thisObject == thread) {
            Word osThread = javaThread.readWord(osThreadOffset(INJECTED_VMCONFIG), JAVA_THREAD_OSTHREAD_LOCATION);
            boolean interrupted = osThread.readInt(osThreadInterruptedOffset(INJECTED_VMCONFIG), any()) != 0;
            if (!interrupted || !clearInterrupted) {
                return interrupted;
            }
        }

        return threadIsInterruptedStub(THREAD_IS_INTERRUPTED, thisObject, clearInterrupted);
    }

    public static final ForeignCallDescriptor THREAD_IS_INTERRUPTED = new ForeignCallDescriptor("thread_is_interrupted", boolean.class, Thread.class, boolean.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native boolean threadIsInterruptedStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Thread thread, boolean clearIsInterrupted);
}
