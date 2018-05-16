package graalvm.compiler.nodes.graphbuilderconf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.nodes.ValueNode;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Abstract class for a plugin generated for a method annotated by {@link NodeIntrinsic} or
 * {@link Fold}.
 */
public abstract class GeneratedInvocationPlugin implements InvocationPlugin
{
    /**
     * Gets the class of the annotation for which this plugin was generated.
     */
    public abstract Class<? extends Annotation> getSource();

    @Override
    public abstract boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args);

    @Override
    public StackTraceElement getApplySourceLocation(MetaAccessProvider metaAccess)
    {
        Class<?> c = getClass();
        for (Method m : c.getDeclaredMethods())
        {
            if (m.getName().equals("execute"))
            {
                return metaAccess.lookupJavaMethod(m).asStackTraceElement(0);
            }
        }
        throw new GraalError("could not find method named \"execute\" in " + c.getName());
    }

    protected boolean checkInjectedArgument(GraphBuilderContext b, ValueNode arg, ResolvedJavaMethod foldAnnotatedMethod)
    {
        if (arg.isNullConstant())
        {
            return true;
        }

        MetaAccessProvider metaAccess = b.getMetaAccess();
        ResolvedJavaMethod executeMethod = metaAccess.lookupJavaMethod(getExecuteMethod());
        ResolvedJavaType thisClass = metaAccess.lookupJavaType(getClass());
        ResolvedJavaMethod thisExecuteMethod = thisClass.resolveConcreteMethod(executeMethod, thisClass);
        if (b.getMethod().equals(thisExecuteMethod))
        {
            // The "execute" method of this plugin is itself being compiled. In (only) this context,
            // the injected argument of the call to the @Fold annotated method will be non-null.
            return true;
        }
        throw new AssertionError("must pass null to injected argument of " + foldAnnotatedMethod.format("%H.%n(%p)") + ", not " + arg);
    }

    private static Method getExecuteMethod()
    {
        try
        {
            return GeneratedInvocationPlugin.class.getMethod("execute", GraphBuilderContext.class, ResolvedJavaMethod.class, InvocationPlugin.Receiver.class, ValueNode[].class);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            throw new GraalError(e);
        }
    }
}
