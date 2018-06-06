package giraaff.hotspot.stubs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.DeoptimizeCallerNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.word.Word;

///
// A collection of methods used in {@link Stub}s.
///
// @class StubUtil
public final class StubUtil
{
    public static ForeignCallDescriptor newDescriptor(Class<?> __stubClass, String __name, Class<?> __resultType, Class<?>... __argumentTypes)
    {
        return new ForeignCallDescriptor(__name, __resultType, __argumentTypes);
    }

    ///
    // Looks for a {@link StubForeignCallNode} node intrinsic named {@code name} in
    // {@code stubClass} and returns a {@link ForeignCallDescriptor} based on its signature and the
    // value of {@code hasSideEffect}.
    ///
    private static ForeignCallDescriptor descriptorFor(Class<?> __stubClass, String __name)
    {
        Method __found = null;
        for (Method __method : __stubClass.getDeclaredMethods())
        {
            if (Modifier.isStatic(__method.getModifiers()) && __method.getAnnotation(Node.NodeIntrinsic.class) != null && __method.getName().equals(__name))
            {
                if (__method.getAnnotation(Node.NodeIntrinsic.class).value().equals(StubForeignCallNode.class))
                {
                    __found = __method;
                }
            }
        }
        List<Class<?>> __paramList = Arrays.asList(__found.getParameterTypes());
        Class<?>[] __cCallTypes = __paramList.subList(1, __paramList.size()).toArray(new Class<?>[__paramList.size() - 1]);
        return new ForeignCallDescriptor(__name, __found.getReturnType(), __cCallTypes);
    }

    public static void handlePendingException(Word __thread, boolean __isObjectResult)
    {
        if (HotSpotReplacementsUtil.clearPendingException(__thread) != null)
        {
            if (__isObjectResult)
            {
                HotSpotReplacementsUtil.getAndClearObjectResult(__thread);
            }
            DeoptimizeCallerNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
    }
}
