package giraaff.nodes.graphbuilderconf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.Node;
import giraaff.nodes.ValueNode;
import giraaff.util.GraalError;

///
// Abstract class for a plugin generated for a method annotated by {@link Node.NodeIntrinsic} or {@link Fold}.
///
// @class GeneratedInvocationPlugin
public abstract class GeneratedInvocationPlugin implements InvocationPlugin
{
    ///
    // Gets the class of the annotation for which this plugin was generated.
    ///
    public abstract Class<? extends Annotation> getSource();

    @Override
    public abstract boolean execute(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode[] __args);

    protected boolean checkInjectedArgument(GraphBuilderContext __b, ValueNode __arg, ResolvedJavaMethod __foldAnnotatedMethod)
    {
        if (__arg.isNullConstant())
        {
            return true;
        }

        MetaAccessProvider __metaAccess = __b.getMetaAccess();
        ResolvedJavaMethod __executeMethod = __metaAccess.lookupJavaMethod(getExecuteMethod());
        ResolvedJavaType __thisClass = __metaAccess.lookupJavaType(getClass());
        ResolvedJavaMethod __thisExecuteMethod = __thisClass.resolveConcreteMethod(__executeMethod, __thisClass);
        if (__b.getMethod().equals(__thisExecuteMethod))
        {
            // The "execute" method of this plugin is itself being compiled. In (only) this context,
            // the injected argument of the call to the @Fold annotated method will be non-null.
            return true;
        }
        throw new AssertionError("must pass null to injected argument of " + __foldAnnotatedMethod.format("%H.%n(%p)") + ", not " + __arg);
    }

    private static Method getExecuteMethod()
    {
        try
        {
            return GeneratedInvocationPlugin.class.getMethod("execute", GraphBuilderContext.class, ResolvedJavaMethod.class, InvocationPlugin.Receiver.class, ValueNode[].class);
        }
        catch (NoSuchMethodException | SecurityException __e)
        {
            throw new GraalError(__e);
        }
    }
}
