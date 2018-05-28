package giraaff.hotspot.stubs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.nodes.DeoptimizeCallerNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.word.Word;

/**
 * A collection of methods used in {@link Stub}s.
 */
public class StubUtil
{
    public static ForeignCallDescriptor newDescriptor(Class<?> stubClass, String name, Class<?> resultType, Class<?>... argumentTypes)
    {
        return new ForeignCallDescriptor(name, resultType, argumentTypes);
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
}
