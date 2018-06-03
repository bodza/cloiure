package giraaff.nodes.graphbuilderconf;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.Invoke;
import giraaff.nodes.ValueNode;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;

///
// Plugin for handling a specific method invocation.
///
// @iface InvocationPlugin
public interface InvocationPlugin extends GraphBuilderPlugin
{
    ///
    // The receiver in a non-static method. The class literal for this interface must be used with
    // {@link InvocationPlugins#put(InvocationPlugin, boolean, boolean, Class, String, Class...)} to
    // denote the receiver argument for such a non-static method.
    ///
    // @iface InvocationPlugin.Receiver
    public interface Receiver
    {
        ///
        // Gets the receiver value, null checking it first if necessary.
        //
        // @return the receiver value with a {@linkplain StampTool#isPointerNonNull(ValueNode)
        //         non-null} stamp
        ///
        default ValueNode get()
        {
            return get(true);
        }

        ///
        // Gets the receiver value, optionally null checking it first if necessary.
        ///
        ValueNode get(boolean __performNullCheck);

        ///
        // Determines if the receiver is constant.
        ///
        default boolean isConstant()
        {
            return false;
        }
    }

    ///
    // Determines if this plugin is for a method with a polymorphic signature (e.g.
    // {@link MethodHandle#invokeExact(Object...)}).
    ///
    default boolean isSignaturePolymorphic()
    {
        return false;
    }

    ///
    // Determines if this plugin can only be used when inlining the method is it associated with.
    // That is, this plugin cannot be used when the associated method is the compilation root.
    ///
    default boolean inlineOnly()
    {
        return isSignaturePolymorphic();
    }

    ///
    // Determines if this plugin only decorates the method is it associated with. That is, it
    // inserts nodes prior to the invocation (e.g. some kind of marker nodes) but still expects the
    // parser to process the invocation further.
    ///
    default boolean isDecorator()
    {
        return false;
    }

    ///
    // Handles invocation of a signature polymorphic method.
    //
    // @param receiver access to the receiver, {@code null} if {@code targetMethod} is static
    // @param argsIncludingReceiver all arguments to the invocation include the raw receiver in
    //            position 0 if {@code targetMethod} is not static
    // @see #execute
    ///
    default boolean applyPolymorphic(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode... __argsIncludingReceiver)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __argsIncludingReceiver);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver)
    {
        return defaultHandler(__b, __targetMethod, __receiver);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2, ValueNode __arg3)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2, __arg3);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2, ValueNode __arg3, ValueNode __arg4)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2, __arg3, __arg4);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2, ValueNode __arg3, ValueNode __arg4, ValueNode __arg5)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2, __arg3, __arg4, __arg5);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2, ValueNode __arg3, ValueNode __arg4, ValueNode __arg5, ValueNode __arg6)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6);
    }

    ///
    // @see #execute
    ///
    default boolean apply(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode __arg1, ValueNode __arg2, ValueNode __arg3, ValueNode __arg4, ValueNode __arg5, ValueNode __arg6, ValueNode __arg7)
    {
        return defaultHandler(__b, __targetMethod, __receiver, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6, __arg7);
    }

    ///
    // Executes this plugin against a set of invocation arguments.
    //
    // The default implementation in {@link InvocationPlugin} dispatches to the {@code apply(...)}
    // method that matches the number of arguments or to {@link #applyPolymorphic} if {@code plugin}
    // is {@linkplain #isSignaturePolymorphic() signature polymorphic}.
    //
    // @param targetMethod the method for which this plugin is being applied
    // @param receiver access to the receiver, {@code null} if {@code targetMethod} is static
    // @param argsIncludingReceiver all arguments to the invocation include the receiver in position
    //            0 if {@code targetMethod} is not static
    // @return {@code true} if this plugin handled the invocation of {@code targetMethod}
    //         {@code false} if the graph builder should process the invoke further (e.g. by inlining it
    //         or creating an {@link Invoke} node). A plugin that does not handle an invocation must not modify
    //         the graph being constructed unless it is a {@linkplain InvocationPlugin#isDecorator() decorator}.
    ///
    default boolean execute(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode[] __argsIncludingReceiver)
    {
        if (isSignaturePolymorphic())
        {
            return applyPolymorphic(__b, __targetMethod, __receiver, __argsIncludingReceiver);
        }
        else if (__receiver != null)
        {
            if (__argsIncludingReceiver.length == 1)
            {
                return apply(__b, __targetMethod, __receiver);
            }
            else if (__argsIncludingReceiver.length == 2)
            {
                return apply(__b, __targetMethod, __receiver, __argsIncludingReceiver[1]);
            }
            else if (__argsIncludingReceiver.length == 3)
            {
                return apply(__b, __targetMethod, __receiver, __argsIncludingReceiver[1], __argsIncludingReceiver[2]);
            }
            else if (__argsIncludingReceiver.length == 4)
            {
                return apply(__b, __targetMethod, __receiver, __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3]);
            }
            else if (__argsIncludingReceiver.length == 5)
            {
                return apply(__b, __targetMethod, __receiver, __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3], __argsIncludingReceiver[4]);
            }
            else
            {
                return defaultHandler(__b, __targetMethod, __receiver, __argsIncludingReceiver);
            }
        }
        else
        {
            if (__argsIncludingReceiver.length == 0)
            {
                return apply(__b, __targetMethod, null);
            }
            else if (__argsIncludingReceiver.length == 1)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0]);
            }
            else if (__argsIncludingReceiver.length == 2)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1]);
            }
            else if (__argsIncludingReceiver.length == 3)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1], __argsIncludingReceiver[2]);
            }
            else if (__argsIncludingReceiver.length == 4)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3]);
            }
            else if (__argsIncludingReceiver.length == 5)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3], __argsIncludingReceiver[4]);
            }
            else if (__argsIncludingReceiver.length == 6)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3], __argsIncludingReceiver[4], __argsIncludingReceiver[5]);
            }
            else if (__argsIncludingReceiver.length == 7)
            {
                return apply(__b, __targetMethod, null, __argsIncludingReceiver[0], __argsIncludingReceiver[1], __argsIncludingReceiver[2], __argsIncludingReceiver[3], __argsIncludingReceiver[4], __argsIncludingReceiver[5], __argsIncludingReceiver[6]);
            }
            else
            {
                return defaultHandler(__b, __targetMethod, __receiver, __argsIncludingReceiver);
            }
        }
    }

    ///
    // Handles an invocation when a specific {@code apply} method is not available.
    ///
    default boolean defaultHandler(@SuppressWarnings("unused") GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, @SuppressWarnings("unused") InvocationPlugin.Receiver __receiver, ValueNode... __args)
    {
        throw new GraalError("Invocation plugin for %s does not handle invocations with %d arguments", __targetMethod.format("%H.%n(%p)"), __args.length);
    }
}
