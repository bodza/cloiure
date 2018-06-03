package giraaff.nodes.graphbuilderconf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.collections.Pair;
import org.graalvm.collections.UnmodifiableEconomicMap;
import org.graalvm.collections.UnmodifiableMapCursor;

import giraaff.api.replacements.MethodSubstitutionRegistry;
import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import giraaff.nodes.graphbuilderconf.InvocationPlugins.LateClassPlugins;
import giraaff.util.GraalError;

/**
 * Manages a set of {@link InvocationPlugin}s.
 *
 * Most plugins are registered during initialization (i.e., before
 * {@link #lookupInvocation(ResolvedJavaMethod)} or {@link #getBindings} is called). These
 * registrations can be made with {@link Registration},
 * {@link #register(InvocationPlugin, String, String, Type...)},
 * {@link #register(InvocationPlugin, Type, String, Type...)} or
 * {@link #registerOptional(InvocationPlugin, Type, String, Type...)}. Initialization is not
 * thread-safe and so must only be performed by a single thread.
 *
 * Plugins that are not guaranteed to be made during initialization must use
 * {@link LateRegistration}.
 */
// @class InvocationPlugins
public class InvocationPlugins
{
    // @class InvocationPlugins.InvocationPluginReceiver
    public static final class InvocationPluginReceiver implements InvocationPlugin.Receiver
    {
        // @field
        private final GraphBuilderContext parser;
        // @field
        private ValueNode[] args;
        // @field
        private ValueNode value;

        // @cons
        public InvocationPluginReceiver(GraphBuilderContext __parser)
        {
            super();
            this.parser = __parser;
        }

        @Override
        public ValueNode get(boolean __performNullCheck)
        {
            if (!__performNullCheck)
            {
                return args[0];
            }
            if (value == null)
            {
                value = parser.nullCheckedValue(args[0]);
                if (value != args[0])
                {
                    args[0] = value;
                }
            }
            return value;
        }

        @Override
        public boolean isConstant()
        {
            return args[0].isConstant();
        }

        public InvocationPluginReceiver init(ResolvedJavaMethod __targetMethod, ValueNode[] __newArgs)
        {
            if (!__targetMethod.isStatic())
            {
                this.args = __newArgs;
                this.value = null;
                return this;
            }
            return null;
        }
    }

    /**
     * A symbol for an already resolved method.
     */
    // @class InvocationPlugins.ResolvedJavaSymbol
    public static final class ResolvedJavaSymbol implements Type
    {
        // @field
        private final ResolvedJavaType resolved;

        // @cons
        public ResolvedJavaSymbol(ResolvedJavaType __type)
        {
            super();
            this.resolved = __type;
        }

        public ResolvedJavaType getResolved()
        {
            return resolved;
        }
    }

    /**
     * A symbol that is lazily {@linkplain OptionalLazySymbol#resolve() resolved} to a {@link Type}.
     */
    // @class InvocationPlugins.OptionalLazySymbol
    static final class OptionalLazySymbol implements Type
    {
        // @def
        private static final Class<?> MASK_NULL = OptionalLazySymbol.class;
        // @field
        private final String name;
        // @field
        private Class<?> resolved;

        // @cons
        OptionalLazySymbol(String __name)
        {
            super();
            this.name = __name;
        }

        @Override
        public String getTypeName()
        {
            return name;
        }

        /**
         * Gets the resolved {@link Class} corresponding to this symbol or {@code null} if
         * resolution fails.
         */
        public Class<?> resolve()
        {
            if (resolved == null)
            {
                Class<?> __resolvedOrNull = resolveClass(name, true);
                resolved = __resolvedOrNull == null ? MASK_NULL : __resolvedOrNull;
            }
            return resolved == MASK_NULL ? null : resolved;
        }
    }

    /**
     * Utility for {@linkplain InvocationPlugins#register(InvocationPlugin, Class, String, Class...)
     * registration} of invocation plugins.
     */
    // @class InvocationPlugins.Registration
    public static final class Registration implements MethodSubstitutionRegistry
    {
        // @field
        private final InvocationPlugins plugins;
        // @field
        private final Type declaringType;
        // @field
        private final BytecodeProvider methodSubstitutionBytecodeProvider;
        // @field
        private boolean allowOverwrite;

        @Override
        public Class<?> getReceiverType()
        {
            return Receiver.class;
        }

        /**
         * Creates an object for registering {@link InvocationPlugin}s for methods declared by a
         * given class.
         *
         * @param plugins where to register the plugins
         * @param declaringType the class declaring the methods for which plugins will be registered
         *            via this object
         */
        // @cons
        public Registration(InvocationPlugins __plugins, Type __declaringType)
        {
            super();
            this.plugins = __plugins;
            this.declaringType = __declaringType;
            this.methodSubstitutionBytecodeProvider = null;
        }

        /**
         * Creates an object for registering {@link InvocationPlugin}s for methods declared by a
         * given class.
         *
         * @param plugins where to register the plugins
         * @param declaringType the class declaring the methods for which plugins will be registered
         *            via this object
         * @param methodSubstitutionBytecodeProvider provider used to get the bytecodes to parse for
         *            method substitutions
         */
        // @cons
        public Registration(InvocationPlugins __plugins, Type __declaringType, BytecodeProvider __methodSubstitutionBytecodeProvider)
        {
            super();
            this.plugins = __plugins;
            this.declaringType = __declaringType;
            this.methodSubstitutionBytecodeProvider = __methodSubstitutionBytecodeProvider;
        }

        /**
         * Creates an object for registering {@link InvocationPlugin}s for methods declared by a
         * given class.
         *
         * @param plugins where to register the plugins
         * @param declaringClassName the name of the class class declaring the methods for which
         *            plugins will be registered via this object
         * @param methodSubstitutionBytecodeProvider provider used to get the bytecodes to parse for
         *            method substitutions
         */
        // @cons
        public Registration(InvocationPlugins __plugins, String __declaringClassName, BytecodeProvider __methodSubstitutionBytecodeProvider)
        {
            super();
            this.plugins = __plugins;
            this.declaringType = new OptionalLazySymbol(__declaringClassName);
            this.methodSubstitutionBytecodeProvider = __methodSubstitutionBytecodeProvider;
        }

        /**
         * Configures this registration to allow or disallow overwriting of invocation plugins.
         */
        public Registration setAllowOverwrite(boolean __allowOverwrite)
        {
            this.allowOverwrite = __allowOverwrite;
            return this;
        }

        /**
         * Registers a plugin for a method with no arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register0(String __name, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name);
        }

        /**
         * Registers a plugin for a method with 1 argument.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register1(String __name, Type __arg, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg);
        }

        /**
         * Registers a plugin for a method with 2 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register2(String __name, Type __arg1, Type __arg2, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2);
        }

        /**
         * Registers a plugin for a method with 3 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register3(String __name, Type __arg1, Type __arg2, Type __arg3, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3);
        }

        /**
         * Registers a plugin for a method with 4 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register4(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3, __arg4);
        }

        /**
         * Registers a plugin for a method with 5 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register5(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5);
        }

        /**
         * Registers a plugin for a method with 6 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register6(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, Type __arg6, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6);
        }

        /**
         * Registers a plugin for a method with 7 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register7(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, Type __arg6, Type __arg7, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6, __arg7);
        }

        /**
         * Registers a plugin for an optional method with no arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional0(String __name, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, true, allowOverwrite, declaringType, __name);
        }

        /**
         * Registers a plugin for an optional method with 1 argument.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional1(String __name, Type __arg, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, true, allowOverwrite, declaringType, __name, __arg);
        }

        /**
         * Registers a plugin for an optional method with 2 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional2(String __name, Type __arg1, Type __arg2, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, true, allowOverwrite, declaringType, __name, __arg1, __arg2);
        }

        /**
         * Registers a plugin for an optional method with 3 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional3(String __name, Type __arg1, Type __arg2, Type __arg3, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, true, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3);
        }

        /**
         * Registers a plugin for an optional method with 4 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional4(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, InvocationPlugin __plugin)
        {
            plugins.register(__plugin, true, allowOverwrite, declaringType, __name, __arg1, __arg2, __arg3, __arg4);
        }

        /**
         * Registers a plugin that implements a method based on the bytecode of a substitute method.
         *
         * @param substituteDeclaringClass the class declaring the substitute method
         * @param name the name of both the original and substitute method
         * @param argumentTypes the argument types of the method. Element 0 of this array must be
         *            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
         *            is non-static. Upon returning, element 0 will have been rewritten to
         *            {@code declaringClass}
         */
        @Override
        public void registerMethodSubstitution(Class<?> __substituteDeclaringClass, String __name, Type... __argumentTypes)
        {
            registerMethodSubstitution(__substituteDeclaringClass, __name, __name, __argumentTypes);
        }

        /**
         * Registers a plugin that implements a method based on the bytecode of a substitute method.
         *
         * @param substituteDeclaringClass the class declaring the substitute method
         * @param name the name of both the original method
         * @param substituteName the name of the substitute method
         * @param argumentTypes the argument types of the method. Element 0 of this array must be
         *            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
         *            is non-static. Upon returning, element 0 will have been rewritten to
         *            {@code declaringClass}
         */
        @Override
        public void registerMethodSubstitution(Class<?> __substituteDeclaringClass, String __name, String __substituteName, Type... __argumentTypes)
        {
            MethodSubstitutionPlugin __plugin = createMethodSubstitution(__substituteDeclaringClass, __substituteName, __argumentTypes);
            plugins.register(__plugin, false, allowOverwrite, declaringType, __name, __argumentTypes);
        }

        public MethodSubstitutionPlugin createMethodSubstitution(Class<?> __substituteDeclaringClass, String __substituteName, Type... __argumentTypes)
        {
            return new MethodSubstitutionPlugin(methodSubstitutionBytecodeProvider, __substituteDeclaringClass, __substituteName, __argumentTypes);
        }
    }

    /**
     * Utility for registering plugins after Graal may have been initialized. Registrations made via
     * this class are not finalized until {@link #close} is called.
     */
    // @class InvocationPlugins.LateRegistration
    public static final class LateRegistration implements AutoCloseable
    {
        // @field
        private InvocationPlugins plugins;
        // @field
        private final List<Binding> bindings = new ArrayList<>();
        // @field
        private final Type declaringType;

        /**
         * Creates an object for registering {@link InvocationPlugin}s for methods declared by a
         * given class.
         *
         * @param plugins where to register the plugins
         * @param declaringType the class declaring the methods for which plugins will be registered
         *            via this object
         */
        // @cons
        public LateRegistration(InvocationPlugins __plugins, Type __declaringType)
        {
            super();
            this.plugins = __plugins;
            this.declaringType = __declaringType;
        }

        /**
         * Registers an invocation plugin for a given method. There must be no plugin currently
         * registered for {@code method}.
         *
         * @param argumentTypes the argument types of the method. Element 0 of this array must be
         *            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
         *            is non-static. Upon returning, element 0 will have been rewritten to
         *            {@code declaringClass}
         */
        public void register(InvocationPlugin __plugin, String __name, Type... __argumentTypes)
        {
            boolean __isStatic = __argumentTypes.length == 0 || __argumentTypes[0] != InvocationPlugin.Receiver.class;
            if (!__isStatic)
            {
                __argumentTypes[0] = declaringType;
            }

            Binding __binding = new Binding(__plugin, __isStatic, __name, __argumentTypes);
            bindings.add(__binding);
        }

        @Override
        public void close()
        {
            plugins.registerLate(declaringType, bindings);
            plugins = null;
        }
    }

    /**
     * Associates an {@link InvocationPlugin} with the details of a method it substitutes.
     */
    // @class InvocationPlugins.Binding
    public static final class Binding
    {
        /**
         * The plugin this binding is for.
         */
        // @field
        public final InvocationPlugin plugin;

        /**
         * Specifies if the associated method is static.
         */
        // @field
        public final boolean isStatic;

        /**
         * The name of the associated method.
         */
        // @field
        public final String name;

        /**
         * A partial
         * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">method
         * descriptor</a> for the associated method. The descriptor includes enclosing {@code '('}
         * and {@code ')'} characters but omits the return type suffix.
         */
        // @field
        public final String argumentsDescriptor;

        /**
         * Link in a list of bindings.
         */
        // @field
        private Binding next;

        // @cons
        Binding(InvocationPlugin __data, boolean __isStatic, String __name, Type... __argumentTypes)
        {
            super();
            this.plugin = __data;
            this.isStatic = __isStatic;
            this.name = __name;
            StringBuilder __sb = new StringBuilder();
            __sb.append('(');
            for (int __i = __isStatic ? 0 : 1; __i < __argumentTypes.length; __i++)
            {
                __sb.append(MetaUtil.toInternalName(__argumentTypes[__i].getTypeName()));
            }
            __sb.append(')');
            this.argumentsDescriptor = __sb.toString();
        }

        // @cons
        Binding(ResolvedJavaMethod __resolved, InvocationPlugin __data)
        {
            super();
            this.plugin = __data;
            this.isStatic = __resolved.isStatic();
            this.name = __resolved.getName();
            Signature __sig = __resolved.getSignature();
            String __desc = __sig.toMethodDescriptor();
            this.argumentsDescriptor = __desc.substring(0, __desc.indexOf(')') + 1);
        }
    }

    /**
     * Plugin registrations for already resolved methods. If non-null, then {@link #registrations}
     * is null and no further registrations can be made.
     */
    // @field
    private final UnmodifiableEconomicMap<ResolvedJavaMethod, InvocationPlugin> resolvedRegistrations;

    /**
     * Map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form to the
     * invocation plugin bindings for the class. Tf non-null, then {@link #resolvedRegistrations}
     * will be null.
     */
    // @field
    private final EconomicMap<String, ClassPlugins> registrations;

    /**
     * Deferred registrations as well as the guard for delimiting the initial registration phase.
     * The guard uses double-checked locking which is why this field is {@code volatile}.
     */
    // @field
    private volatile List<Runnable> deferredRegistrations = new ArrayList<>();

    /**
     * Adds a {@link Runnable} for doing registration deferred until the first time
     * {@link #get(ResolvedJavaMethod)} or {@link #closeRegistration()} is called on this object.
     */
    public void defer(Runnable __deferrable)
    {
        deferredRegistrations.add(__deferrable);
    }

    /**
     * Support for registering plugins once this object may be accessed by multiple threads.
     */
    // @field
    private volatile LateClassPlugins lateRegistrations;

    /**
     * Per-class bindings.
     */
    // @class InvocationPlugins.ClassPlugins
    static class ClassPlugins
    {
        /**
         * Maps method names to binding lists.
         */
        // @field
        private final EconomicMap<String, Binding> bindings = EconomicMap.create(Equivalence.DEFAULT);

        /**
         * Gets the invocation plugin for a given method.
         *
         * @return the invocation plugin for {@code method} or {@code null}
         */
        InvocationPlugin get(ResolvedJavaMethod __method)
        {
            Binding __binding = bindings.get(__method.getName());
            while (__binding != null)
            {
                if (__method.isStatic() == __binding.isStatic)
                {
                    if (__method.getSignature().toMethodDescriptor().startsWith(__binding.argumentsDescriptor))
                    {
                        return __binding.plugin;
                    }
                }
                __binding = __binding.next;
            }
            return null;
        }

        public void register(Binding __binding, boolean __allowOverwrite)
        {
            if (__allowOverwrite)
            {
                if (lookup(__binding) != null)
                {
                    register(__binding);
                    return;
                }
            }
            register(__binding);
        }

        InvocationPlugin lookup(Binding __binding)
        {
            Binding __b = bindings.get(__binding.name);
            while (__b != null)
            {
                if (__b.isStatic == __binding.isStatic && __b.argumentsDescriptor.equals(__binding.argumentsDescriptor))
                {
                    return __b.plugin;
                }
                __b = __b.next;
            }
            return null;
        }

        /**
         * Registers {@code binding}.
         */
        void register(Binding __binding)
        {
            Binding __head = bindings.get(__binding.name);
            __binding.next = __head;
            bindings.put(__binding.name, __binding);
        }
    }

    // @class InvocationPlugins.LateClassPlugins
    static final class LateClassPlugins extends ClassPlugins
    {
        // @def
        static final String CLOSED_LATE_CLASS_PLUGIN = "-----";

        // @field
        private final String className;
        // @field
        private final LateClassPlugins next;

        // @cons
        LateClassPlugins(LateClassPlugins __next, String __className)
        {
            super();
            this.next = __next;
            this.className = __className;
        }
    }

    /**
     * Registers a binding of a method to an invocation plugin.
     *
     * @param plugin invocation plugin to be associated with the specified method
     * @param isStatic specifies if the method is static
     * @param declaringClass the class declaring the method
     * @param name the name of the method
     * @param argumentTypes the argument types of the method. Element 0 of this array must be
     *            {@code declaringClass} iff the method is non-static.
     * @return an object representing the method
     */
    Binding put(InvocationPlugin __plugin, boolean __isStatic, boolean __allowOverwrite, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        String __internalName = MetaUtil.toInternalName(__declaringClass.getTypeName());

        ClassPlugins __classPlugins = registrations.get(__internalName);
        if (__classPlugins == null)
        {
            __classPlugins = new ClassPlugins();
            registrations.put(__internalName, __classPlugins);
        }
        Binding __binding = new Binding(__plugin, __isStatic, __name, __argumentTypes);
        __classPlugins.register(__binding, __allowOverwrite);
        return __binding;
    }

    InvocationPlugin get(ResolvedJavaMethod __method)
    {
        if (resolvedRegistrations != null)
        {
            return resolvedRegistrations.get(__method);
        }
        else
        {
            if (!__method.isBridge())
            {
                ResolvedJavaType __declaringClass = __method.getDeclaringClass();
                flushDeferrables();
                String __internalName = __declaringClass.getName();
                ClassPlugins __classPlugins = registrations.get(__internalName);
                InvocationPlugin __res = null;
                if (__classPlugins != null)
                {
                    __res = __classPlugins.get(__method);
                }
                if (__res == null)
                {
                    LateClassPlugins __lcp = findLateClassPlugins(__internalName);
                    if (__lcp != null)
                    {
                        __res = __lcp.get(__method);
                    }
                }
                if (__res != null)
                {
                    // A decorator plugin is trusted since it does not replace
                    // the method it intrinsifies.
                    if (__res.isDecorator() || canBeIntrinsified(__declaringClass))
                    {
                        return __res;
                    }
                }
                if (testExtensions != null)
                {
                    // Avoid the synchronization in the common case that there
                    // are no test extensions.
                    synchronized (this)
                    {
                        if (testExtensions != null)
                        {
                            List<Binding> __bindings = testExtensions.get(__internalName);
                            if (__bindings != null)
                            {
                                String __name = __method.getName();
                                String __descriptor = __method.getSignature().toMethodDescriptor();
                                for (Binding __b : __bindings)
                                {
                                    if (__b.isStatic == __method.isStatic() && __b.name.equals(__name) && __descriptor.startsWith(__b.argumentsDescriptor))
                                    {
                                        return __b.plugin;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                // Supporting plugins for bridge methods would require including
                // the return type in the registered signature. Until needed,
                // this extra complexity is best avoided.
            }
        }
        return null;
    }

    /**
     * Determines if methods in a given class can have invocation plugins.
     *
     * @param declaringClass the class to test
     */
    public boolean canBeIntrinsified(ResolvedJavaType __declaringClass)
    {
        return true;
    }

    LateClassPlugins findLateClassPlugins(String __internalClassName)
    {
        for (LateClassPlugins __lcp = lateRegistrations; __lcp != null; __lcp = __lcp.next)
        {
            if (__lcp.className.equals(__internalClassName))
            {
                return __lcp;
            }
        }
        return null;
    }

    // @class InvocationPlugins.InvocationPlugRegistrationError
    static final class InvocationPlugRegistrationError extends GraalError
    {
        // @cons
        InvocationPlugRegistrationError(Throwable __cause)
        {
            super(__cause);
        }
    }

    private void flushDeferrables()
    {
        if (deferredRegistrations != null)
        {
            synchronized (this)
            {
                if (deferredRegistrations != null)
                {
                    try
                    {
                        for (Runnable __deferrable : deferredRegistrations)
                        {
                            __deferrable.run();
                        }
                        deferredRegistrations = null;
                    }
                    catch (InvocationPlugRegistrationError __t)
                    {
                        throw __t;
                    }
                    catch (Throwable __t)
                    {
                        /*
                         * Something went wrong during registration but it's possible we'll end up
                         * coming back into this code. nulling out deferredRegistrations would just
                         * cause other things to break and rerunning them would cause errors about
                         * already registered plugins, so rethrow the original exception during
                         * later invocations.
                         */
                        deferredRegistrations.clear();
                        // @closure
                        Runnable rethrow = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                throw new InvocationPlugRegistrationError(__t);
                            }
                        };
                        deferredRegistrations.add(rethrow);
                        rethrow.run();
                    }
                }
            }
        }
    }

    // @field
    private volatile EconomicMap<String, List<Binding>> testExtensions;

    private static int findBinding(List<Binding> __list, Binding __key)
    {
        for (int __i = 0; __i < __list.size(); __i++)
        {
            Binding __b = __list.get(__i);
            if (__b.isStatic == __key.isStatic && __b.name.equals(__key.name) && __b.argumentsDescriptor.equals(__key.argumentsDescriptor))
            {
                return __i;
            }
        }
        return -1;
    }

    /**
     * Extends the plugins in this object with those from {@code other}. The added plugins should be
     * {@linkplain #removeTestPlugins(InvocationPlugins) removed} after the test.
     *
     * This extension mechanism exists only for tests that want to add extra invocation plugins
     * after the compiler has been initialized.
     *
     * @param ignored if non-null, the bindings from {@code other} already in this object prior to
     *            calling this method are added to this list. These bindings are not added to this object.
     */
    public synchronized void addTestPlugins(InvocationPlugins __other, List<Pair<String, Binding>> __ignored)
    {
        EconomicMap<String, List<Binding>> __otherBindings = __other.getBindings(true, false);
        if (__otherBindings.isEmpty())
        {
            return;
        }
        if (testExtensions == null)
        {
            testExtensions = EconomicMap.create();
        }
        MapCursor<String, List<Binding>> __c = __otherBindings.getEntries();
        while (__c.advance())
        {
            String __declaringClass = __c.getKey();
            List<Binding> __bindings = testExtensions.get(__declaringClass);
            if (__bindings == null)
            {
                __bindings = new ArrayList<>();
                testExtensions.put(__declaringClass, __bindings);
            }
            for (Binding __b : __c.getValue())
            {
                int __index = findBinding(__bindings, __b);
                if (__index != -1)
                {
                    if (__ignored != null)
                    {
                        __ignored.add(Pair.create(__declaringClass, __b));
                    }
                }
                else
                {
                    __bindings.add(__b);
                }
            }
        }
    }

    /**
     * Removes the plugins from {@code other} in this object that were added by
     * {@link #addTestPlugins}.
     */
    public synchronized void removeTestPlugins(InvocationPlugins __other)
    {
        if (testExtensions != null)
        {
            MapCursor<String, List<Binding>> __c = __other.getBindings(false).getEntries();
            while (__c.advance())
            {
                String __declaringClass = __c.getKey();
                List<Binding> __bindings = testExtensions.get(__declaringClass);
                if (__bindings != null)
                {
                    for (Binding __b : __c.getValue())
                    {
                        int __index = findBinding(__bindings, __b);
                        if (__index != -1)
                        {
                            __bindings.remove(__index);
                        }
                    }
                    if (__bindings.isEmpty())
                    {
                        testExtensions.removeKey(__declaringClass);
                    }
                }
            }
            if (testExtensions.isEmpty())
            {
                testExtensions = null;
            }
        }
    }

    synchronized void registerLate(Type __declaringType, List<Binding> __bindings)
    {
        String __internalName = MetaUtil.toInternalName(__declaringType.getTypeName());
        LateClassPlugins __lateClassPlugins = new LateClassPlugins(lateRegistrations, __internalName);
        for (Binding __b : __bindings)
        {
            __lateClassPlugins.register(__b);
        }
        lateRegistrations = __lateClassPlugins;
    }

    private synchronized boolean closeLateRegistrations()
    {
        if (lateRegistrations == null || lateRegistrations.className != LateClassPlugins.CLOSED_LATE_CLASS_PLUGIN)
        {
            lateRegistrations = new LateClassPlugins(lateRegistrations, LateClassPlugins.CLOSED_LATE_CLASS_PLUGIN);
        }
        return true;
    }

    /**
     * Processes deferred registrations and then closes this object for future registration.
     */
    public void closeRegistration()
    {
        flushDeferrables();
    }

    public boolean isEmpty()
    {
        if (resolvedRegistrations != null)
        {
            return resolvedRegistrations.isEmpty();
        }
        return registrations.size() == 0 && lateRegistrations == null;
    }

    /**
     * The plugins {@linkplain #lookupInvocation(ResolvedJavaMethod) searched} before searching in
     * this object.
     */
    // @field
    protected final InvocationPlugins parent;

    /**
     * Creates a set of invocation plugins with no parent.
     */
    // @cons
    public InvocationPlugins()
    {
        this(null);
    }

    /**
     * Creates a set of invocation plugins.
     *
     * @param parent if non-null, this object will be searched first when looking up plugins
     */
    // @cons
    public InvocationPlugins(InvocationPlugins __parent)
    {
        super();
        InvocationPlugins __p = __parent;
        this.parent = __p;
        this.registrations = EconomicMap.create();
        this.resolvedRegistrations = null;
    }

    /**
     * Creates a closed set of invocation plugins for a set of resolved methods. Such an object
     * cannot have further plugins registered.
     */
    // @cons
    public InvocationPlugins(Map<ResolvedJavaMethod, InvocationPlugin> __plugins, InvocationPlugins __parent)
    {
        super();
        this.parent = __parent;
        this.registrations = null;
        this.deferredRegistrations = null;
        EconomicMap<ResolvedJavaMethod, InvocationPlugin> __map = EconomicMap.create(__plugins.size());

        for (Map.Entry<ResolvedJavaMethod, InvocationPlugin> __entry : __plugins.entrySet())
        {
            __map.put(__entry.getKey(), __entry.getValue());
        }
        this.resolvedRegistrations = __map;
    }

    protected void register(InvocationPlugin __plugin, boolean __isOptional, boolean __allowOverwrite, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        boolean __isStatic = __argumentTypes.length == 0 || __argumentTypes[0] != InvocationPlugin.Receiver.class;
        if (!__isStatic)
        {
            __argumentTypes[0] = __declaringClass;
        }
        Binding __binding = put(__plugin, __isStatic, __allowOverwrite, __declaringClass, __name, __argumentTypes);
    }

    /**
     * Registers an invocation plugin for a given method. There must be no plugin currently
     * registered for {@code method}.
     *
     * @param argumentTypes the argument types of the method. Element 0 of this array must be the
     *            {@link Class} value for {@link InvocationPlugin.Receiver} iff the method is
     *            non-static. Upon returning, element 0 will have been rewritten to
     *            {@code declaringClass}
     */
    public void register(InvocationPlugin __plugin, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, false, false, __declaringClass, __name, __argumentTypes);
    }

    public void register(InvocationPlugin __plugin, String __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, false, false, new OptionalLazySymbol(__declaringClass), __name, __argumentTypes);
    }

    /**
     * Registers an invocation plugin for a given, optional method. There must be no plugin
     * currently registered for {@code method}.
     *
     * @param argumentTypes the argument types of the method. Element 0 of this array must be the
     *            {@link Class} value for {@link InvocationPlugin.Receiver} iff the method is
     *            non-static. Upon returning, element 0 will have been rewritten to
     *            {@code declaringClass}
     */
    public void registerOptional(InvocationPlugin __plugin, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, true, false, __declaringClass, __name, __argumentTypes);
    }

    /**
     * Gets the plugin for a given method.
     *
     * @param method the method to lookup
     * @return the plugin associated with {@code method} or {@code null} if none exists
     */
    public InvocationPlugin lookupInvocation(ResolvedJavaMethod __method)
    {
        if (parent != null)
        {
            InvocationPlugin __plugin = parent.lookupInvocation(__method);
            if (__plugin != null)
            {
                return __plugin;
            }
        }
        return get(__method);
    }

    /**
     * Gets the set of registered invocation plugins.
     *
     * @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
     *         to the invocation plugin bindings for methods in the class
     */
    public EconomicMap<String, List<Binding>> getBindings(boolean __includeParents)
    {
        return getBindings(__includeParents, true);
    }

    /**
     * Gets the set of registered invocation plugins.
     *
     * @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
     *         to the invocation plugin bindings for methods in the class
     */
    private EconomicMap<String, List<Binding>> getBindings(boolean __includeParents, boolean __flushDeferrables)
    {
        EconomicMap<String, List<Binding>> __res = EconomicMap.create(Equivalence.DEFAULT);
        if (parent != null && __includeParents)
        {
            __res.putAll(parent.getBindings(true, __flushDeferrables));
        }
        if (resolvedRegistrations != null)
        {
            UnmodifiableMapCursor<ResolvedJavaMethod, InvocationPlugin> __cursor = resolvedRegistrations.getEntries();
            while (__cursor.advance())
            {
                ResolvedJavaMethod __method = __cursor.getKey();
                InvocationPlugin __plugin = __cursor.getValue();
                String __type = __method.getDeclaringClass().getName();
                List<Binding> __bindings = __res.get(__type);
                if (__bindings == null)
                {
                    __bindings = new ArrayList<>();
                    __res.put(__type, __bindings);
                }
                __bindings.add(new Binding(__method, __plugin));
            }
        }
        else
        {
            if (__flushDeferrables)
            {
                flushDeferrables();
            }
            MapCursor<String, ClassPlugins> __classes = registrations.getEntries();
            while (__classes.advance())
            {
                String __type = __classes.getKey();
                ClassPlugins __cp = __classes.getValue();
                collectBindingsTo(__res, __type, __cp);
            }
            for (LateClassPlugins __lcp = lateRegistrations; __lcp != null; __lcp = __lcp.next)
            {
                String __type = __lcp.className;
                collectBindingsTo(__res, __type, __lcp);
            }
            if (testExtensions != null)
            {
                // Avoid the synchronization in the common case that there are no test extensions.
                synchronized (this)
                {
                    if (testExtensions != null)
                    {
                        MapCursor<String, List<Binding>> __c = testExtensions.getEntries();
                        while (__c.advance())
                        {
                            String __name = __c.getKey();
                            List<Binding> __bindings = __res.get(__name);
                            if (__bindings == null)
                            {
                                __bindings = new ArrayList<>();
                                __res.put(__name, __bindings);
                            }
                            __bindings.addAll(__c.getValue());
                        }
                    }
                }
            }
        }
        return __res;
    }

    private static void collectBindingsTo(EconomicMap<String, List<Binding>> __res, String __type, ClassPlugins __cp)
    {
        MapCursor<String, Binding> __methods = __cp.bindings.getEntries();
        while (__methods.advance())
        {
            List<Binding> __bindings = __res.get(__type);
            if (__bindings == null)
            {
                __bindings = new ArrayList<>();
                __res.put(__type, __bindings);
            }
            for (Binding __b = __methods.getValue(); __b != null; __b = __b.next)
            {
                __bindings.add(__b);
            }
        }
    }

    /**
     * Gets the invocation plugins {@linkplain #lookupInvocation(ResolvedJavaMethod) searched}
     * before searching in this object.
     */
    public InvocationPlugins getParent()
    {
        return parent;
    }

    /**
     * Resolves a name to a class.
     *
     * @param className the name of the class to resolve
     * @param optional if true, resolution failure returns null
     * @return the resolved class or null if resolution fails and {@code optional} is true
     */
    public static Class<?> resolveClass(String __className, boolean __optional)
    {
        try
        {
            // Need to use the system class loader to handle classes loaded by the application
            // class loader, which is not delegated to by the JVMCI class loader.
            return Class.forName(__className, false, ClassLoader.getSystemClassLoader());
        }
        catch (ClassNotFoundException __e)
        {
            if (__optional)
            {
                return null;
            }
            throw new GraalError("Could not resolve type " + __className);
        }
    }

    /**
     * Resolves a {@link Type} to a {@link Class}.
     *
     * @param type the type to resolve
     * @param optional if true, resolution failure returns null
     * @return the resolved class or null if resolution fails and {@code optional} is true
     */
    public static Class<?> resolveType(Type __type, boolean __optional)
    {
        if (__type instanceof Class)
        {
            return (Class<?>) __type;
        }
        if (__type instanceof OptionalLazySymbol)
        {
            return ((OptionalLazySymbol) __type).resolve();
        }
        return resolveClass(__type.getTypeName(), __optional);
    }

    private static List<String> toInternalTypeNames(Class<?>[] __types)
    {
        String[] __res = new String[__types.length];
        for (int __i = 0; __i < __types.length; __i++)
        {
            __res[__i] = MetaUtil.toInternalName(__types[__i].getTypeName());
        }
        return Arrays.asList(__res);
    }

    /**
     * Resolves a given binding to a method in a given class. If more than one method with the
     * parameter types matching {@code binding} is found and the return types of all the matching
     * methods form an inheritance chain, the one with the most specific type is returned; otherwise
     * {@link NoSuchMethodError} is thrown.
     *
     * @param declaringClass the class to search for a method matching {@code binding}
     * @return the method (if any) in {@code declaringClass} matching {@code binding}
     */
    public static Method resolveMethod(Class<?> __declaringClass, Binding __binding)
    {
        if (__binding.name.equals("<init>"))
        {
            return null;
        }
        Method[] __methods = __declaringClass.getDeclaredMethods();
        List<String> __parameterTypeNames = parseParameters(__binding.argumentsDescriptor);
        Method __match = null;
        for (int __i = 0; __i < __methods.length; ++__i)
        {
            Method __m = __methods[__i];
            if (__binding.isStatic == Modifier.isStatic(__m.getModifiers()) && __m.getName().equals(__binding.name) && __parameterTypeNames.equals(toInternalTypeNames(__m.getParameterTypes())))
            {
                if (__match == null)
                {
                    __match = __m;
                }
                else if (__match.getReturnType().isAssignableFrom(__m.getReturnType()))
                {
                    // 'm' has a more specific return type - choose it
                    // ('match' is most likely a bridge method)
                    __match = __m;
                }
                else
                {
                    if (!__m.getReturnType().isAssignableFrom(__match.getReturnType()))
                    {
                        throw new NoSuchMethodError(String.format("Found 2 methods with same name and parameter types but unrelated return types:%n %s%n %s", __match, __m));
                    }
                }
            }
        }
        return __match;
    }

    /**
     * Same as {@link #resolveMethod(Class, Binding)} and
     * {@link #resolveConstructor(Class, Binding)} except in terms of {@link ResolvedJavaType} and
     * {@link ResolvedJavaMethod}.
     */
    public static ResolvedJavaMethod resolveJavaMethod(ResolvedJavaType __declaringClass, Binding __binding)
    {
        ResolvedJavaMethod[] __methods = __declaringClass.getDeclaredMethods();
        if (__binding.name.equals("<init>"))
        {
            for (ResolvedJavaMethod __m : __methods)
            {
                if (__m.getName().equals("<init>") && __m.getSignature().toMethodDescriptor().startsWith(__binding.argumentsDescriptor))
                {
                    return __m;
                }
            }
            return null;
        }

        ResolvedJavaMethod __match = null;
        for (int __i = 0; __i < __methods.length; ++__i)
        {
            ResolvedJavaMethod __m = __methods[__i];
            if (__binding.isStatic == __m.isStatic() && __m.getName().equals(__binding.name) && __m.getSignature().toMethodDescriptor().startsWith(__binding.argumentsDescriptor))
            {
                if (__match == null)
                {
                    __match = __m;
                }
                else
                {
                    final ResolvedJavaType __matchReturnType = (ResolvedJavaType) __match.getSignature().getReturnType(__declaringClass);
                    final ResolvedJavaType __mReturnType = (ResolvedJavaType) __m.getSignature().getReturnType(__declaringClass);
                    if (__matchReturnType.isAssignableFrom(__mReturnType))
                    {
                        // 'm' has a more specific return type - choose it
                        // ('match' is most likely a bridge method)
                        __match = __m;
                    }
                    else
                    {
                        if (!__mReturnType.isAssignableFrom(__matchReturnType))
                        {
                            throw new NoSuchMethodError(String.format("Found 2 methods with same name and parameter types but unrelated return types:%n %s%n %s", __match, __m));
                        }
                    }
                }
            }
        }
        return __match;
    }

    /**
     * Resolves a given binding to a constructor in a given class.
     *
     * @param declaringClass the class to search for a constructor matching {@code binding}
     * @return the constructor (if any) in {@code declaringClass} matching binding
     */
    public static Constructor<?> resolveConstructor(Class<?> __declaringClass, Binding __binding)
    {
        if (!__binding.name.equals("<init>"))
        {
            return null;
        }
        Constructor<?>[] __constructors = __declaringClass.getDeclaredConstructors();
        List<String> __parameterTypeNames = parseParameters(__binding.argumentsDescriptor);
        for (int __i = 0; __i < __constructors.length; ++__i)
        {
            Constructor<?> __c = __constructors[__i];
            if (__parameterTypeNames.equals(toInternalTypeNames(__c.getParameterTypes())))
            {
                return __c;
            }
        }
        return null;
    }

    private static List<String> parseParameters(String __argumentsDescriptor)
    {
        List<String> __res = new ArrayList<>();
        int __cur = 1;
        int __end = __argumentsDescriptor.length() - 1;
        while (__cur != __end)
        {
            char __first;
            int __start = __cur;
            do
            {
                __first = __argumentsDescriptor.charAt(__cur++);
            } while (__first == '[');

            switch (__first)
            {
                case 'L':
                {
                    int __endObject = __argumentsDescriptor.indexOf(';', __cur);
                    if (__endObject == -1)
                    {
                        throw new GraalError("Invalid object type at index %d in signature: %s", __cur, __argumentsDescriptor);
                    }
                    __cur = __endObject + 1;
                    break;
                }
                case 'V':
                case 'I':
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'J':
                case 'S':
                case 'Z':
                    break;
                default:
                    throw new GraalError("Invalid character at index %d in signature: %s", __cur, __argumentsDescriptor);
            }
            __res.add(__argumentsDescriptor.substring(__start, __cur));
        }
        return __res;
    }
}
