package giraaff.hotspot.stubs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Fold.InjectedParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.nodes.DeoptimizeCallerNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.PiNode;
import giraaff.nodes.SnippetAnchorNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;

/**
 * A collection of methods used in {@link Stub}s.
 */
public class StubUtil
{
    public static final ForeignCallDescriptor VM_MESSAGE_C = newDescriptor(StubUtil.class, "vmMessageC", void.class, boolean.class, Word.class, long.class, long.class, long.class);

    public static ForeignCallDescriptor newDescriptor(Class<?> stubClass, String name, Class<?> resultType, Class<?>... argumentTypes)
    {
        ForeignCallDescriptor d = new ForeignCallDescriptor(name, resultType, argumentTypes);
        return d;
    }

    /**
     * Looks for a {@link StubForeignCallNode} node intrinsic named {@code name} in
     * {@code stubClass} and returns a {@link ForeignCallDescriptor} based on its signature and the
     * value of {@code hasSideEffect}.
     */
    private static ForeignCallDescriptor descriptorFor(Class<?> stubClass, String name)
    {
        Method found = null;
        for (Method method : stubClass.getDeclaredMethods())
        {
            if (Modifier.isStatic(method.getModifiers()) && method.getAnnotation(NodeIntrinsic.class) != null && method.getName().equals(name))
            {
                if (method.getAnnotation(NodeIntrinsic.class).value().equals(StubForeignCallNode.class))
                {
                    found = method;
                }
            }
        }
        List<Class<?>> paramList = Arrays.asList(found.getParameterTypes());
        Class<?>[] cCallTypes = paramList.subList(1, paramList.size()).toArray(new Class<?>[paramList.size() - 1]);
        return new ForeignCallDescriptor(name, found.getReturnType(), cCallTypes);
    }

    public static void handlePendingException(Word thread, boolean isObjectResult)
    {
        if (HotSpotReplacementsUtil.clearPendingException(thread) != null)
        {
            if (isObjectResult)
            {
                HotSpotReplacementsUtil.getAndClearObjectResult(thread);
            }
            DeoptimizeCallerNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void vmMessageC(@ConstantNodeParameter ForeignCallDescriptor stubPrintfC, boolean vmError, Word format, long v1, long v2, long v3);

    /**
     * Prints a message to the log stream.
     *
     * @param message a message string
     */
    public static void printf(String message)
    {
        vmMessageC(VM_MESSAGE_C, false, CStringConstant.cstring(message), 0L, 0L, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * @param format a C style printf format value
     * @param value the value associated with the first conversion specifier in {@code format}
     */
    public static void printf(String format, long value)
    {
        vmMessageC(VM_MESSAGE_C, false, CStringConstant.cstring(format), value, 0L, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     */
    public static void printf(String format, long v1, long v2)
    {
        vmMessageC(VM_MESSAGE_C, false, CStringConstant.cstring(format), v1, v2, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     * @param v3 the value associated with the third conversion specifier in {@code format}
     */
    public static void printf(String format, long v1, long v2, long v3)
    {
        vmMessageC(VM_MESSAGE_C, false, CStringConstant.cstring(format), v1, v2, v3);
    }

    /**
     * Analyzes a given value and prints information about it to the log stream.
     */
    public static void decipher(long value)
    {
        vmMessageC(VM_MESSAGE_C, false, WordFactory.zero(), value, 0L, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * @param message an error message
     */
    public static void fatal(String message)
    {
        vmMessageC(VM_MESSAGE_C, true, CStringConstant.cstring(message), 0L, 0L, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * @param format a C style printf format value
     * @param value the value associated with the first conversion specifier in {@code format}
     */
    public static void fatal(String format, long value)
    {
        vmMessageC(VM_MESSAGE_C, true, CStringConstant.cstring(format), value, 0L, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     */
    public static void fatal(String format, long v1, long v2)
    {
        vmMessageC(VM_MESSAGE_C, true, CStringConstant.cstring(format), v1, v2, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     * @param v3 the value associated with the third conversion specifier in {@code format}
     */
    public static void fatal(String format, long v1, long v2, long v3)
    {
        vmMessageC(VM_MESSAGE_C, true, CStringConstant.cstring(format), v1, v2, v3);
    }

    /**
     * Verifies that a given object value is well formed if {@code -XX:+VerifyOops} is enabled.
     */
    public static Object verifyObject(Object object)
    {
        if (HotSpotReplacementsUtil.verifyOops(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
        {
            Word verifyOopCounter = WordFactory.unsigned(verifyOopCounterAddress(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
            verifyOopCounter.writeInt(0, verifyOopCounter.readInt(0) + 1);

            Pointer oop = Word.objectToTrackedPointer(object);
            if (object != null)
            {
                GuardingNode anchorNode = SnippetAnchorNode.anchor();
                // make sure object is 'reasonable'
                if (!oop.and(WordFactory.unsigned(verifyOopMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG))).equal(WordFactory.unsigned(verifyOopBits(GraalHotSpotVMConfig.INJECTED_VMCONFIG))))
                {
                    fatal("oop not in heap: %p", oop.rawValue());
                }

                KlassPointer klass = HotSpotReplacementsUtil.loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
                if (klass.isNull())
                {
                    fatal("klass for oop %p is null", oop.rawValue());
                }
            }
        }
        return object;
    }

    @Fold
    static long verifyOopCounterAddress(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.verifyOopCounterAddress;
    }

    @Fold
    static long verifyOopMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.verifyOopMask;
    }

    @Fold
    static long verifyOopBits(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.verifyOopBits;
    }

    @Fold
    static int hubOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.hubOffset;
    }
}
