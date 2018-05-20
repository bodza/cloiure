package graalvm.compiler.hotspot.stubs;

import static jdk.vm.ci.meta.DeoptimizationReason.RuntimeConstraint;
import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.clearPendingException;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.getAndClearObjectResult;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.loadHubIntrinsic;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.verifyOops;
import static graalvm.compiler.replacements.nodes.CStringConstant.cstring;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.nodes.DeoptimizeCallerNode;
import graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import graalvm.compiler.hotspot.nodes.VMErrorNode;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.SnippetAnchorNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.replacements.Log;
import graalvm.compiler.word.Word;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.meta.DeoptimizationAction;

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
        if (clearPendingException(thread) != null)
        {
            if (isObjectResult)
            {
                getAndClearObjectResult(thread);
            }
            DeoptimizeCallerNode.deopt(DeoptimizationAction.None, RuntimeConstraint);
        }
    }

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void vmMessageC(@ConstantNodeParameter ForeignCallDescriptor stubPrintfC, boolean vmError, Word format, long v1, long v2, long v3);

    /**
     * Prints a message to the log stream.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param message a message string
     */
    public static void printf(String message)
    {
        vmMessageC(VM_MESSAGE_C, false, cstring(message), 0L, 0L, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param value the value associated with the first conversion specifier in {@code format}
     */
    public static void printf(String format, long value)
    {
        vmMessageC(VM_MESSAGE_C, false, cstring(format), value, 0L, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     */
    public static void printf(String format, long v1, long v2)
    {
        vmMessageC(VM_MESSAGE_C, false, cstring(format), v1, v2, 0L);
    }

    /**
     * Prints a message to the log stream.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long, long, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     * @param v3 the value associated with the third conversion specifier in {@code format}
     */
    public static void printf(String format, long v1, long v2, long v3)
    {
        vmMessageC(VM_MESSAGE_C, false, cstring(format), v1, v2, v3);
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
     * <b>Stubs must use this instead of {@link VMErrorNode#vmError(String, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param message an error message
     */
    public static void fatal(String message)
    {
        vmMessageC(VM_MESSAGE_C, true, cstring(message), 0L, 0L, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long, long, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param value the value associated with the first conversion specifier in {@code format}
     */
    public static void fatal(String format, long value)
    {
        vmMessageC(VM_MESSAGE_C, true, cstring(format), value, 0L, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long, long, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     */
    public static void fatal(String format, long v1, long v2)
    {
        vmMessageC(VM_MESSAGE_C, true, cstring(format), v1, v2, 0L);
    }

    /**
     * Exits the VM with a given error message.
     *
     * <b>Stubs must use this instead of {@link Log#printf(String, long, long, long)} to avoid an object constant in a RuntimeStub.</b>
     *
     * @param format a C style printf format value
     * @param v1 the value associated with the first conversion specifier in {@code format}
     * @param v2 the value associated with the second conversion specifier in {@code format}
     * @param v3 the value associated with the third conversion specifier in {@code format}
     */
    public static void fatal(String format, long v1, long v2, long v3)
    {
        vmMessageC(VM_MESSAGE_C, true, cstring(format), v1, v2, v3);
    }

    /**
     * Verifies that a given object value is well formed if {@code -XX:+VerifyOops} is enabled.
     */
    public static Object verifyObject(Object object)
    {
        if (verifyOops(INJECTED_VMCONFIG))
        {
            Word verifyOopCounter = WordFactory.unsigned(verifyOopCounterAddress(INJECTED_VMCONFIG));
            verifyOopCounter.writeInt(0, verifyOopCounter.readInt(0) + 1);

            Pointer oop = Word.objectToTrackedPointer(object);
            if (object != null)
            {
                GuardingNode anchorNode = SnippetAnchorNode.anchor();
                // make sure object is 'reasonable'
                if (!oop.and(WordFactory.unsigned(verifyOopMask(INJECTED_VMCONFIG))).equal(WordFactory.unsigned(verifyOopBits(INJECTED_VMCONFIG))))
                {
                    fatal("oop not in heap: %p", oop.rawValue());
                }

                KlassPointer klass = loadHubIntrinsic(PiNode.piCastNonNull(object, anchorNode));
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
