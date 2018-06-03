package giraaff.hotspot.replacements;

import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.CurrentJavaThreadNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.word.Word;

/**
 * Substitutions for {@link java.lang.Thread} methods.
 */
@ClassSubstitution(Thread.class)
// @class ThreadSubstitutions
public final class ThreadSubstitutions
{
    /**
     * hidden in 9.
     */
    @MethodSubstitution(isStatic = false, optional = true)
    public static boolean isInterrupted(final Thread __thisObject, boolean __clearInterrupted)
    {
        Word __javaThread = CurrentJavaThreadNode.get();
        Object __thread = __javaThread.readObject(HotSpotRuntime.threadObjectOffset, HotSpotReplacementsUtil.JAVA_THREAD_THREAD_OBJECT_LOCATION);
        if (__thisObject == __thread)
        {
            Word __osThread = __javaThread.readWord(HotSpotRuntime.osThreadOffset, HotSpotReplacementsUtil.JAVA_THREAD_OSTHREAD_LOCATION);
            boolean __interrupted = __osThread.readInt(HotSpotRuntime.osThreadInterruptedOffset, LocationIdentity.any()) != 0;
            if (!__interrupted || !__clearInterrupted)
            {
                return __interrupted;
            }
        }

        return threadIsInterruptedStub(THREAD_IS_INTERRUPTED, __thisObject, __clearInterrupted);
    }

    // @def
    public static final ForeignCallDescriptor THREAD_IS_INTERRUPTED = new ForeignCallDescriptor("thread_is_interrupted", boolean.class, Thread.class, boolean.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native boolean threadIsInterruptedStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Thread thread, boolean clearIsInterrupted);
}
