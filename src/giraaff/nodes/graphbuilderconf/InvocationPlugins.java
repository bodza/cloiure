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
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.util.GraalError;

///
// Manages a set of {@link InvocationPlugin}s.
//
// Most plugins are registered during initialization (i.e., before
// {@link #lookupInvocation(ResolvedJavaMethod)} or {@link #getBindings} is called).
// These registrations can be made with {@link InvocationPlugins.Registration},
// {@link #register(InvocationPlugin, String, String, Type...)},
// {@link #register(InvocationPlugin, Type, String, Type...)} or
// {@link #registerOptional(InvocationPlugin, Type, String, Type...)}.
// Initialization is not thread-safe and so must only be performed by a single thread.
//
// Plugins that are not guaranteed to be made during initialization must use
// {@link InvocationPlugins.LateRegistration}.
///
// @class InvocationPlugins
public class InvocationPlugins
{
    // @class InvocationPlugins.InvocationPluginReceiver
    public static final class InvocationPluginReceiver implements InvocationPlugin.Receiver
    {
        // @field
        private final GraphBuilderContext ___parser;
        // @field
        private ValueNode[] ___args;
        // @field
        private ValueNode ___value;

        // @cons InvocationPlugins.InvocationPluginReceiver
        public InvocationPluginReceiver(GraphBuilderContext __parser)
        {
            super();
            this.___parser = __parser;
        }

        @Override
        public ValueNode get(boolean __performNullCheck)
        {
            if (!__performNullCheck)
            {
                return this.___args[0];
            }
            if (this.___value == null)
            {
                this.___value = this.___parser.nullCheckedValue(this.___args[0]);
                if (this.___value != this.___args[0])
                {
                    this.___args[0] = this.___value;
                }
            }
            return this.___value;
        }

        @Override
        public boolean isConstant()
        {
            return this.___args[0].isConstant();
        }

        public InvocationPlugins.InvocationPluginReceiver init(ResolvedJavaMethod __targetMethod, ValueNode[] __newArgs)
        {
            if (!__targetMethod.isStatic())
            {
                this.___args = __newArgs;
                this.___value = null;
                return this;
            }
            return null;
        }
    }

    ///
    // A symbol for an already resolved method.
    ///
    // @class InvocationPlugins.ResolvedJavaSymbol
    public static final class ResolvedJavaSymbol implements Type
    {
        // @field
        private final ResolvedJavaType ___resolved;

        // @cons InvocationPlugins.ResolvedJavaSymbol
        public ResolvedJavaSymbol(ResolvedJavaType __type)
        {
            super();
            this.___resolved = __type;
        }

        public ResolvedJavaType getResolved()
        {
            return this.___resolved;
        }
    }

    ///
    // A symbol that is lazily {@linkplain InvocationPlugins.OptionalLazySymbol#resolve() resolved} to a {@link Type}.
    ///
    // @class InvocationPlugins.OptionalLazySymbol
    static final class OptionalLazySymbol implements Type
    {
        // @def
        private static final Class<?> MASK_NULL = InvocationPlugins.OptionalLazySymbol.class;
        // @field
        private final String ___name;
        // @field
        private Class<?> ___resolved;

        // @cons InvocationPlugins.OptionalLazySymbol
        OptionalLazySymbol(String __name)
        {
            super();
            this.___name = __name;
        }

        @Override
        public String getTypeName()
        {
            return this.___name;
        }

        ///
        // Gets the resolved {@link Class} corresponding to this symbol or {@code null} if
        // resolution fails.
        ///
        public Class<?> resolve()
        {
            if (this.___resolved == null)
            {
                Class<?> __resolvedOrNull = resolveClass(this.___name, true);
                this.___resolved = __resolvedOrNull == null ? MASK_NULL : __resolvedOrNull;
            }
            return this.___resolved == MASK_NULL ? null : this.___resolved;
        }
    }

    ///
    // Utility for {@linkplain InvocationPlugins#register(InvocationPlugin, Class, String, Class...)
    // registration} of invocation plugins.
    ///
    // @class InvocationPlugins.Registration
    public static final class Registration implements MethodSubstitutionRegistry
    {
        // @field
        private final InvocationPlugins ___plugins;
        // @field
        private final Type ___declaringType;
        // @field
        private final BytecodeProvider ___methodSubstitutionBytecodeProvider;
        // @field
        private boolean ___allowOverwrite;

        @Override
        public Class<?> getReceiverType()
        {
            return InvocationPlugin.Receiver.class;
        }

        ///
        // Creates an object for registering {@link InvocationPlugin}s for methods declared by a
        // given class.
        //
        // @param plugins where to register the plugins
        // @param declaringType the class declaring the methods for which plugins will be registered
        //            via this object
        ///
        // @cons InvocationPlugins.Registration
        public Registration(InvocationPlugins __plugins, Type __declaringType)
        {
            super();
            this.___plugins = __plugins;
            this.___declaringType = __declaringType;
            this.___methodSubstitutionBytecodeProvider = null;
        }

        ///
        // Creates an object for registering {@link InvocationPlugin}s for methods declared by a
        // given class.
        //
        // @param plugins where to register the plugins
        // @param declaringType the class declaring the methods for which plugins will be registered
        //            via this object
        // @param methodSubstitutionBytecodeProvider provider used to get the bytecodes to parse for
        //            method substitutions
        ///
        // @cons InvocationPlugins.Registration
        public Registration(InvocationPlugins __plugins, Type __declaringType, BytecodeProvider __methodSubstitutionBytecodeProvider)
        {
            super();
            this.___plugins = __plugins;
            this.___declaringType = __declaringType;
            this.___methodSubstitutionBytecodeProvider = __methodSubstitutionBytecodeProvider;
        }

        ///
        // Creates an object for registering {@link InvocationPlugin}s for methods declared by a
        // given class.
        //
        // @param plugins where to register the plugins
        // @param declaringClassName the name of the class class declaring the methods for which
        //            plugins will be registered via this object
        // @param methodSubstitutionBytecodeProvider provider used to get the bytecodes to parse for
        //            method substitutions
        ///
        // @cons InvocationPlugins.Registration
        public Registration(InvocationPlugins __plugins, String __declaringClassName, BytecodeProvider __methodSubstitutionBytecodeProvider)
        {
            super();
            this.___plugins = __plugins;
            this.___declaringType = new InvocationPlugins.OptionalLazySymbol(__declaringClassName);
            this.___methodSubstitutionBytecodeProvider = __methodSubstitutionBytecodeProvider;
        }

        ///
        // Configures this registration to allow or disallow overwriting of invocation plugins.
        ///
        public InvocationPlugins.Registration setAllowOverwrite(boolean __allowOverwrite)
        {
            this.___allowOverwrite = __allowOverwrite;
            return this;
        }

        ///
        // Registers a plugin for a method with no arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register0(String __name, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name);
        }

        ///
        // Registers a plugin for a method with 1 argument.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register1(String __name, Type __arg, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg);
        }

        ///
        // Registers a plugin for a method with 2 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register2(String __name, Type __arg1, Type __arg2, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2);
        }

        ///
        // Registers a plugin for a method with 3 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register3(String __name, Type __arg1, Type __arg2, Type __arg3, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3);
        }

        ///
        // Registers a plugin for a method with 4 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register4(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3, __arg4);
        }

        ///
        // Registers a plugin for a method with 5 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register5(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5);
        }

        ///
        // Registers a plugin for a method with 6 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register6(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, Type __arg6, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6);
        }

        ///
        // Registers a plugin for a method with 7 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void register7(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, Type __arg5, Type __arg6, Type __arg7, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3, __arg4, __arg5, __arg6, __arg7);
        }

        ///
        // Registers a plugin for an optional method with no arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void registerOptional0(String __name, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, true, this.___allowOverwrite, this.___declaringType, __name);
        }

        ///
        // Registers a plugin for an optional method with 1 argument.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void registerOptional1(String __name, Type __arg, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, true, this.___allowOverwrite, this.___declaringType, __name, __arg);
        }

        ///
        // Registers a plugin for an optional method with 2 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void registerOptional2(String __name, Type __arg1, Type __arg2, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, true, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2);
        }

        ///
        // Registers a plugin for an optional method with 3 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void registerOptional3(String __name, Type __arg1, Type __arg2, Type __arg3, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, true, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3);
        }

        ///
        // Registers a plugin for an optional method with 4 arguments.
        //
        // @param name the name of the method
        // @param plugin the plugin to be registered
        ///
        public void registerOptional4(String __name, Type __arg1, Type __arg2, Type __arg3, Type __arg4, InvocationPlugin __plugin)
        {
            this.___plugins.register(__plugin, true, this.___allowOverwrite, this.___declaringType, __name, __arg1, __arg2, __arg3, __arg4);
        }

        ///
        // Registers a plugin that implements a method based on the bytecode of a substitute method.
        //
        // @param substituteDeclaringClass the class declaring the substitute method
        // @param name the name of both the original and substitute method
        // @param argumentTypes the argument types of the method. Element 0 of this array must be
        //            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
        //            is non-static. Upon returning, element 0 will have been rewritten to
        //            {@code declaringClass}
        ///
        @Override
        public void registerMethodSubstitution(Class<?> __substituteDeclaringClass, String __name, Type... __argumentTypes)
        {
            registerMethodSubstitution(__substituteDeclaringClass, __name, __name, __argumentTypes);
        }

        ///
        // Registers a plugin that implements a method based on the bytecode of a substitute method.
        //
        // @param substituteDeclaringClass the class declaring the substitute method
        // @param name the name of both the original method
        // @param substituteName the name of the substitute method
        // @param argumentTypes the argument types of the method. Element 0 of this array must be
        //            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
        //            is non-static. Upon returning, element 0 will have been rewritten to
        //            {@code declaringClass}
        ///
        @Override
        public void registerMethodSubstitution(Class<?> __substituteDeclaringClass, String __name, String __substituteName, Type... __argumentTypes)
        {
            MethodSubstitutionPlugin __plugin = createMethodSubstitution(__substituteDeclaringClass, __substituteName, __argumentTypes);
            this.___plugins.register(__plugin, false, this.___allowOverwrite, this.___declaringType, __name, __argumentTypes);
        }

        public MethodSubstitutionPlugin createMethodSubstitution(Class<?> __substituteDeclaringClass, String __substituteName, Type... __argumentTypes)
        {
            return new MethodSubstitutionPlugin(this.___methodSubstitutionBytecodeProvider, __substituteDeclaringClass, __substituteName, __argumentTypes);
        }
    }

    ///
    // Utility for registering plugins after Graal may have been initialized. Registrations made via
    // this class are not finalized until {@link #close} is called.
    ///
    // @class InvocationPlugins.LateRegistration
    public static final class LateRegistration implements AutoCloseable
    {
        // @field
        private InvocationPlugins ___plugins;
        // @field
        private final List<InvocationPlugins.Binding> ___bindings = new ArrayList<>();
        // @field
        private final Type ___declaringType;

        ///
        // Creates an object for registering {@link InvocationPlugin}s for methods declared by a
        // given class.
        //
        // @param plugins where to register the plugins
        // @param declaringType the class declaring the methods for which plugins will be registered
        //            via this object
        ///
        // @cons InvocationPlugins.LateRegistration
        public LateRegistration(InvocationPlugins __plugins, Type __declaringType)
        {
            super();
            this.___plugins = __plugins;
            this.___declaringType = __declaringType;
        }

        ///
        // Registers an invocation plugin for a given method. There must be no plugin currently
        // registered for {@code method}.
        //
        // @param argumentTypes the argument types of the method. Element 0 of this array must be
        //            the {@link Class} value for {@link InvocationPlugin.Receiver} iff the method
        //            is non-static. Upon returning, element 0 will have been rewritten to
        //            {@code declaringClass}
        ///
        public void register(InvocationPlugin __plugin, String __name, Type... __argumentTypes)
        {
            boolean __isStatic = __argumentTypes.length == 0 || __argumentTypes[0] != InvocationPlugin.Receiver.class;
            if (!__isStatic)
            {
                __argumentTypes[0] = this.___declaringType;
            }

            InvocationPlugins.Binding __binding = new InvocationPlugins.Binding(__plugin, __isStatic, __name, __argumentTypes);
            this.___bindings.add(__binding);
        }

        @Override
        public void close()
        {
            this.___plugins.registerLate(this.___declaringType, this.___bindings);
            this.___plugins = null;
        }
    }

    ///
    // Associates an {@link InvocationPlugin} with the details of a method it substitutes.
    ///
    // @class InvocationPlugins.Binding
    public static final class Binding
    {
        ///
        // The plugin this binding is for.
        ///
        // @field
        public final InvocationPlugin ___plugin;

        ///
        // Specifies if the associated method is static.
        ///
        // @field
        public final boolean ___isStatic;

        ///
        // The name of the associated method.
        ///
        // @field
        public final String ___name;

        ///
        // A partial
        // <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">method
        // descriptor</a> for the associated method. The descriptor includes enclosing {@code '('}
        // and {@code ')'} characters but omits the return type suffix.
        ///
        // @field
        public final String ___argumentsDescriptor;

        ///
        // Link in a list of bindings.
        ///
        // @field
        private InvocationPlugins.Binding ___next;

        // @cons InvocationPlugins.Binding
        Binding(InvocationPlugin __data, boolean __isStatic, String __name, Type... __argumentTypes)
        {
            super();
            this.___plugin = __data;
            this.___isStatic = __isStatic;
            this.___name = __name;
            StringBuilder __sb = new StringBuilder();
            __sb.append('(');
            for (int __i = __isStatic ? 0 : 1; __i < __argumentTypes.length; __i++)
            {
                __sb.append(MetaUtil.toInternalName(__argumentTypes[__i].getTypeName()));
            }
            __sb.append(')');
            this.___argumentsDescriptor = __sb.toString();
        }

        // @cons InvocationPlugins.Binding
        Binding(ResolvedJavaMethod __resolved, InvocationPlugin __data)
        {
            super();
            this.___plugin = __data;
            this.___isStatic = __resolved.isStatic();
            this.___name = __resolved.getName();
            Signature __sig = __resolved.getSignature();
            String __desc = __sig.toMethodDescriptor();
            this.___argumentsDescriptor = __desc.substring(0, __desc.indexOf(')') + 1);
        }
    }

    ///
    // Plugin registrations for already resolved methods. If non-null, then {@link #registrations}
    // is null and no further registrations can be made.
    ///
    // @field
    private final UnmodifiableEconomicMap<ResolvedJavaMethod, InvocationPlugin> ___resolvedRegistrations;

    ///
    // Map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form to the
    // invocation plugin bindings for the class. Tf non-null, then {@link #resolvedRegistrations}
    // will be null.
    ///
    // @field
    private final EconomicMap<String, InvocationPlugins.ClassPlugins> ___registrations;

    ///
    // Deferred registrations as well as the guard for delimiting the initial registration phase.
    // The guard uses double-checked locking which is why this field is {@code volatile}.
    ///
    // @field
    private volatile List<Runnable> ___deferredRegistrations = new ArrayList<>();

    ///
    // Adds a {@link Runnable} for doing registration deferred until the first time
    // {@link #get(ResolvedJavaMethod)} or {@link #closeRegistration()} is called on this object.
    ///
    public void defer(Runnable __deferrable)
    {
        this.___deferredRegistrations.add(__deferrable);
    }

    ///
    // Support for registering plugins once this object may be accessed by multiple threads.
    ///
    // @field
    private volatile InvocationPlugins.LateClassPlugins ___lateRegistrations;

    ///
    // Per-class bindings.
    ///
    // @class InvocationPlugins.ClassPlugins
    static class ClassPlugins
    {
        ///
        // Maps method names to binding lists.
        ///
        // @field
        private final EconomicMap<String, InvocationPlugins.Binding> ___bindings = EconomicMap.create(Equivalence.DEFAULT);

        ///
        // Gets the invocation plugin for a given method.
        //
        // @return the invocation plugin for {@code method} or {@code null}
        ///
        InvocationPlugin get(ResolvedJavaMethod __method)
        {
            InvocationPlugins.Binding __binding = this.___bindings.get(__method.getName());
            while (__binding != null)
            {
                if (__method.isStatic() == __binding.___isStatic)
                {
                    if (__method.getSignature().toMethodDescriptor().startsWith(__binding.___argumentsDescriptor))
                    {
                        return __binding.___plugin;
                    }
                }
                __binding = __binding.___next;
            }
            return null;
        }

        public void register(InvocationPlugins.Binding __binding, boolean __allowOverwrite)
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

        InvocationPlugin lookup(InvocationPlugins.Binding __binding)
        {
            InvocationPlugins.Binding __b = this.___bindings.get(__binding.___name);
            while (__b != null)
            {
                if (__b.___isStatic == __binding.___isStatic && __b.___argumentsDescriptor.equals(__binding.___argumentsDescriptor))
                {
                    return __b.___plugin;
                }
                __b = __b.___next;
            }
            return null;
        }

        ///
        // Registers {@code binding}.
        ///
        void register(InvocationPlugins.Binding __binding)
        {
            InvocationPlugins.Binding __head = this.___bindings.get(__binding.___name);
            __binding.___next = __head;
            this.___bindings.put(__binding.___name, __binding);
        }
    }

    // @class InvocationPlugins.LateClassPlugins
    static final class LateClassPlugins extends InvocationPlugins.ClassPlugins
    {
        // @def
        static final String CLOSED_LATE_CLASS_PLUGIN = "-----";

        // @field
        private final String ___className;
        // @field
        private final InvocationPlugins.LateClassPlugins ___next;

        // @cons InvocationPlugins.LateClassPlugins
        LateClassPlugins(InvocationPlugins.LateClassPlugins __next, String __className)
        {
            super();
            this.___next = __next;
            this.___className = __className;
        }
    }

    ///
    // Registers a binding of a method to an invocation plugin.
    //
    // @param plugin invocation plugin to be associated with the specified method
    // @param isStatic specifies if the method is static
    // @param declaringClass the class declaring the method
    // @param name the name of the method
    // @param argumentTypes the argument types of the method. Element 0 of this array must be
    //            {@code declaringClass} iff the method is non-static.
    // @return an object representing the method
    ///
    InvocationPlugins.Binding put(InvocationPlugin __plugin, boolean __isStatic, boolean __allowOverwrite, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        String __internalName = MetaUtil.toInternalName(__declaringClass.getTypeName());

        InvocationPlugins.ClassPlugins __classPlugins = this.___registrations.get(__internalName);
        if (__classPlugins == null)
        {
            __classPlugins = new InvocationPlugins.ClassPlugins();
            this.___registrations.put(__internalName, __classPlugins);
        }
        InvocationPlugins.Binding __binding = new InvocationPlugins.Binding(__plugin, __isStatic, __name, __argumentTypes);
        __classPlugins.register(__binding, __allowOverwrite);
        return __binding;
    }

    InvocationPlugin get(ResolvedJavaMethod __method)
    {
        if (this.___resolvedRegistrations != null)
        {
            return this.___resolvedRegistrations.get(__method);
        }
        else
        {
            if (!__method.isBridge())
            {
                ResolvedJavaType __declaringClass = __method.getDeclaringClass();
                flushDeferrables();
                String __internalName = __declaringClass.getName();
                InvocationPlugins.ClassPlugins __classPlugins = this.___registrations.get(__internalName);
                InvocationPlugin __res = null;
                if (__classPlugins != null)
                {
                    __res = __classPlugins.get(__method);
                }
                if (__res == null)
                {
                    InvocationPlugins.LateClassPlugins __lcp = findLateClassPlugins(__internalName);
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
                if (this.___testExtensions != null)
                {
                    // Avoid the synchronization in the common case that there
                    // are no test extensions.
                    synchronized (this)
                    {
                        if (this.___testExtensions != null)
                        {
                            List<InvocationPlugins.Binding> __bindings = this.___testExtensions.get(__internalName);
                            if (__bindings != null)
                            {
                                String __name = __method.getName();
                                String __descriptor = __method.getSignature().toMethodDescriptor();
                                for (InvocationPlugins.Binding __b : __bindings)
                                {
                                    if (__b.___isStatic == __method.isStatic() && __b.___name.equals(__name) && __descriptor.startsWith(__b.___argumentsDescriptor))
                                    {
                                        return __b.___plugin;
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

    ///
    // Determines if methods in a given class can have invocation plugins.
    //
    // @param declaringClass the class to test
    ///
    public boolean canBeIntrinsified(ResolvedJavaType __declaringClass)
    {
        return true;
    }

    InvocationPlugins.LateClassPlugins findLateClassPlugins(String __internalClassName)
    {
        for (InvocationPlugins.LateClassPlugins __lcp = this.___lateRegistrations; __lcp != null; __lcp = __lcp.___next)
        {
            if (__lcp.___className.equals(__internalClassName))
            {
                return __lcp;
            }
        }
        return null;
    }

    // @class InvocationPlugins.InvocationPlugRegistrationError
    static final class InvocationPlugRegistrationError extends GraalError
    {
        // @cons InvocationPlugins.InvocationPlugRegistrationError
        InvocationPlugRegistrationError(Throwable __cause)
        {
            super(__cause);
        }
    }

    private void flushDeferrables()
    {
        if (this.___deferredRegistrations != null)
        {
            synchronized (this)
            {
                if (this.___deferredRegistrations != null)
                {
                    try
                    {
                        for (Runnable __deferrable : this.___deferredRegistrations)
                        {
                            __deferrable.run();
                        }
                        this.___deferredRegistrations = null;
                    }
                    catch (InvocationPlugins.InvocationPlugRegistrationError __t)
                    {
                        throw __t;
                    }
                    catch (Throwable __t)
                    {
                        // Something went wrong during registration but it's possible we'll end up
                        // coming back into this code. nulling out deferredRegistrations would just
                        // cause other things to break and rerunning them would cause errors about
                        // already registered plugins, so rethrow the original exception during
                        // later invocations.
                        this.___deferredRegistrations.clear();
                        // @closure
                        Runnable rethrow = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                throw new InvocationPlugins.InvocationPlugRegistrationError(__t);
                            }
                        };
                        this.___deferredRegistrations.add(rethrow);
                        rethrow.run();
                    }
                }
            }
        }
    }

    // @field
    private volatile EconomicMap<String, List<InvocationPlugins.Binding>> ___testExtensions;

    private static int findBinding(List<InvocationPlugins.Binding> __list, InvocationPlugins.Binding __key)
    {
        for (int __i = 0; __i < __list.size(); __i++)
        {
            InvocationPlugins.Binding __b = __list.get(__i);
            if (__b.___isStatic == __key.___isStatic && __b.___name.equals(__key.___name) && __b.___argumentsDescriptor.equals(__key.___argumentsDescriptor))
            {
                return __i;
            }
        }
        return -1;
    }

    ///
    // Extends the plugins in this object with those from {@code other}. The added plugins should be
    // {@linkplain #removeTestPlugins(InvocationPlugins) removed} after the test.
    //
    // This extension mechanism exists only for tests that want to add extra invocation plugins
    // after the compiler has been initialized.
    //
    // @param ignored if non-null, the bindings from {@code other} already in this object prior to
    //            calling this method are added to this list. These bindings are not added to this object.
    ///
    public synchronized void addTestPlugins(InvocationPlugins __other, List<Pair<String, InvocationPlugins.Binding>> __ignored)
    {
        EconomicMap<String, List<InvocationPlugins.Binding>> __otherBindings = __other.getBindings(true, false);
        if (__otherBindings.isEmpty())
        {
            return;
        }
        if (this.___testExtensions == null)
        {
            this.___testExtensions = EconomicMap.create();
        }
        MapCursor<String, List<InvocationPlugins.Binding>> __c = __otherBindings.getEntries();
        while (__c.advance())
        {
            String __declaringClass = __c.getKey();
            List<InvocationPlugins.Binding> __bindings = this.___testExtensions.get(__declaringClass);
            if (__bindings == null)
            {
                __bindings = new ArrayList<>();
                this.___testExtensions.put(__declaringClass, __bindings);
            }
            for (InvocationPlugins.Binding __b : __c.getValue())
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

    ///
    // Removes the plugins from {@code other} in this object that were added by
    // {@link #addTestPlugins}.
    ///
    public synchronized void removeTestPlugins(InvocationPlugins __other)
    {
        if (this.___testExtensions != null)
        {
            MapCursor<String, List<InvocationPlugins.Binding>> __c = __other.getBindings(false).getEntries();
            while (__c.advance())
            {
                String __declaringClass = __c.getKey();
                List<InvocationPlugins.Binding> __bindings = this.___testExtensions.get(__declaringClass);
                if (__bindings != null)
                {
                    for (InvocationPlugins.Binding __b : __c.getValue())
                    {
                        int __index = findBinding(__bindings, __b);
                        if (__index != -1)
                        {
                            __bindings.remove(__index);
                        }
                    }
                    if (__bindings.isEmpty())
                    {
                        this.___testExtensions.removeKey(__declaringClass);
                    }
                }
            }
            if (this.___testExtensions.isEmpty())
            {
                this.___testExtensions = null;
            }
        }
    }

    synchronized void registerLate(Type __declaringType, List<InvocationPlugins.Binding> __bindings)
    {
        String __internalName = MetaUtil.toInternalName(__declaringType.getTypeName());
        InvocationPlugins.LateClassPlugins __lateClassPlugins = new InvocationPlugins.LateClassPlugins(this.___lateRegistrations, __internalName);
        for (InvocationPlugins.Binding __b : __bindings)
        {
            __lateClassPlugins.register(__b);
        }
        this.___lateRegistrations = __lateClassPlugins;
    }

    private synchronized boolean closeLateRegistrations()
    {
        if (this.___lateRegistrations == null || this.___lateRegistrations.___className != InvocationPlugins.LateClassPlugins.CLOSED_LATE_CLASS_PLUGIN)
        {
            this.___lateRegistrations = new InvocationPlugins.LateClassPlugins(this.___lateRegistrations, InvocationPlugins.LateClassPlugins.CLOSED_LATE_CLASS_PLUGIN);
        }
        return true;
    }

    ///
    // Processes deferred registrations and then closes this object for future registration.
    ///
    public void closeRegistration()
    {
        flushDeferrables();
    }

    public boolean isEmpty()
    {
        if (this.___resolvedRegistrations != null)
        {
            return this.___resolvedRegistrations.isEmpty();
        }
        return this.___registrations.size() == 0 && this.___lateRegistrations == null;
    }

    ///
    // The plugins {@linkplain #lookupInvocation(ResolvedJavaMethod) searched} before searching in
    // this object.
    ///
    // @field
    protected final InvocationPlugins ___parent;

    ///
    // Creates a set of invocation plugins with no parent.
    ///
    // @cons InvocationPlugins
    public InvocationPlugins()
    {
        this(null);
    }

    ///
    // Creates a set of invocation plugins.
    //
    // @param parent if non-null, this object will be searched first when looking up plugins
    ///
    // @cons InvocationPlugins
    public InvocationPlugins(InvocationPlugins __parent)
    {
        super();
        InvocationPlugins __p = __parent;
        this.___parent = __p;
        this.___registrations = EconomicMap.create();
        this.___resolvedRegistrations = null;
    }

    ///
    // Creates a closed set of invocation plugins for a set of resolved methods. Such an object
    // cannot have further plugins registered.
    ///
    // @cons InvocationPlugins
    public InvocationPlugins(Map<ResolvedJavaMethod, InvocationPlugin> __plugins, InvocationPlugins __parent)
    {
        super();
        this.___parent = __parent;
        this.___registrations = null;
        this.___deferredRegistrations = null;
        EconomicMap<ResolvedJavaMethod, InvocationPlugin> __map = EconomicMap.create(__plugins.size());

        for (Map.Entry<ResolvedJavaMethod, InvocationPlugin> __entry : __plugins.entrySet())
        {
            __map.put(__entry.getKey(), __entry.getValue());
        }
        this.___resolvedRegistrations = __map;
    }

    protected void register(InvocationPlugin __plugin, boolean __isOptional, boolean __allowOverwrite, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        boolean __isStatic = __argumentTypes.length == 0 || __argumentTypes[0] != InvocationPlugin.Receiver.class;
        if (!__isStatic)
        {
            __argumentTypes[0] = __declaringClass;
        }
        InvocationPlugins.Binding __binding = put(__plugin, __isStatic, __allowOverwrite, __declaringClass, __name, __argumentTypes);
    }

    ///
    // Registers an invocation plugin for a given method. There must be no plugin currently
    // registered for {@code method}.
    //
    // @param argumentTypes the argument types of the method. Element 0 of this array must be the
    //            {@link Class} value for {@link InvocationPlugin.Receiver} iff the method is
    //            non-static. Upon returning, element 0 will have been rewritten to
    //            {@code declaringClass}
    ///
    public void register(InvocationPlugin __plugin, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, false, false, __declaringClass, __name, __argumentTypes);
    }

    public void register(InvocationPlugin __plugin, String __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, false, false, new InvocationPlugins.OptionalLazySymbol(__declaringClass), __name, __argumentTypes);
    }

    ///
    // Registers an invocation plugin for a given, optional method. There must be no plugin
    // currently registered for {@code method}.
    //
    // @param argumentTypes the argument types of the method. Element 0 of this array must be the
    //            {@link Class} value for {@link InvocationPlugin.Receiver} iff the method is
    //            non-static. Upon returning, element 0 will have been rewritten to
    //            {@code declaringClass}
    ///
    public void registerOptional(InvocationPlugin __plugin, Type __declaringClass, String __name, Type... __argumentTypes)
    {
        register(__plugin, true, false, __declaringClass, __name, __argumentTypes);
    }

    ///
    // Gets the plugin for a given method.
    //
    // @param method the method to lookup
    // @return the plugin associated with {@code method} or {@code null} if none exists
    ///
    public InvocationPlugin lookupInvocation(ResolvedJavaMethod __method)
    {
        if (this.___parent != null)
        {
            InvocationPlugin __plugin = this.___parent.lookupInvocation(__method);
            if (__plugin != null)
            {
                return __plugin;
            }
        }
        return get(__method);
    }

    ///
    // Gets the set of registered invocation plugins.
    //
    // @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
    //         to the invocation plugin bindings for methods in the class
    ///
    public EconomicMap<String, List<InvocationPlugins.Binding>> getBindings(boolean __includeParents)
    {
        return getBindings(__includeParents, true);
    }

    ///
    // Gets the set of registered invocation plugins.
    //
    // @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
    //         to the invocation plugin bindings for methods in the class
    ///
    private EconomicMap<String, List<InvocationPlugins.Binding>> getBindings(boolean __includeParents, boolean __flushDeferrables)
    {
        EconomicMap<String, List<InvocationPlugins.Binding>> __res = EconomicMap.create(Equivalence.DEFAULT);
        if (this.___parent != null && __includeParents)
        {
            __res.putAll(this.___parent.getBindings(true, __flushDeferrables));
        }
        if (this.___resolvedRegistrations != null)
        {
            UnmodifiableMapCursor<ResolvedJavaMethod, InvocationPlugin> __cursor = this.___resolvedRegistrations.getEntries();
            while (__cursor.advance())
            {
                ResolvedJavaMethod __method = __cursor.getKey();
                InvocationPlugin __plugin = __cursor.getValue();
                String __type = __method.getDeclaringClass().getName();
                List<InvocationPlugins.Binding> __bindings = __res.get(__type);
                if (__bindings == null)
                {
                    __bindings = new ArrayList<>();
                    __res.put(__type, __bindings);
                }
                __bindings.add(new InvocationPlugins.Binding(__method, __plugin));
            }
        }
        else
        {
            if (__flushDeferrables)
            {
                flushDeferrables();
            }
            MapCursor<String, InvocationPlugins.ClassPlugins> __classes = this.___registrations.getEntries();
            while (__classes.advance())
            {
                String __type = __classes.getKey();
                InvocationPlugins.ClassPlugins __cp = __classes.getValue();
                collectBindingsTo(__res, __type, __cp);
            }
            for (InvocationPlugins.LateClassPlugins __lcp = this.___lateRegistrations; __lcp != null; __lcp = __lcp.___next)
            {
                String __type = __lcp.___className;
                collectBindingsTo(__res, __type, __lcp);
            }
            if (this.___testExtensions != null)
            {
                // Avoid the synchronization in the common case that there are no test extensions.
                synchronized (this)
                {
                    if (this.___testExtensions != null)
                    {
                        MapCursor<String, List<InvocationPlugins.Binding>> __c = this.___testExtensions.getEntries();
                        while (__c.advance())
                        {
                            String __name = __c.getKey();
                            List<InvocationPlugins.Binding> __bindings = __res.get(__name);
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

    private static void collectBindingsTo(EconomicMap<String, List<InvocationPlugins.Binding>> __res, String __type, InvocationPlugins.ClassPlugins __cp)
    {
        MapCursor<String, InvocationPlugins.Binding> __methods = __cp.___bindings.getEntries();
        while (__methods.advance())
        {
            List<InvocationPlugins.Binding> __bindings = __res.get(__type);
            if (__bindings == null)
            {
                __bindings = new ArrayList<>();
                __res.put(__type, __bindings);
            }
            for (InvocationPlugins.Binding __b = __methods.getValue(); __b != null; __b = __b.___next)
            {
                __bindings.add(__b);
            }
        }
    }

    ///
    // Gets the invocation plugins {@linkplain #lookupInvocation(ResolvedJavaMethod) searched}
    // before searching in this object.
    ///
    public InvocationPlugins getParent()
    {
        return this.___parent;
    }

    ///
    // Resolves a name to a class.
    //
    // @param className the name of the class to resolve
    // @param optional if true, resolution failure returns null
    // @return the resolved class or null if resolution fails and {@code optional} is true
    ///
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

    ///
    // Resolves a {@link Type} to a {@link Class}.
    //
    // @param type the type to resolve
    // @param optional if true, resolution failure returns null
    // @return the resolved class or null if resolution fails and {@code optional} is true
    ///
    public static Class<?> resolveType(Type __type, boolean __optional)
    {
        if (__type instanceof Class)
        {
            return (Class<?>) __type;
        }
        if (__type instanceof InvocationPlugins.OptionalLazySymbol)
        {
            return ((InvocationPlugins.OptionalLazySymbol) __type).resolve();
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

    ///
    // Resolves a given binding to a method in a given class. If more than one method with the
    // parameter types matching {@code binding} is found and the return types of all the matching
    // methods form an inheritance chain, the one with the most specific type is returned; otherwise
    // {@link NoSuchMethodError} is thrown.
    //
    // @param declaringClass the class to search for a method matching {@code binding}
    // @return the method (if any) in {@code declaringClass} matching {@code binding}
    ///
    public static Method resolveMethod(Class<?> __declaringClass, InvocationPlugins.Binding __binding)
    {
        if (__binding.___name.equals("<init>"))
        {
            return null;
        }
        Method[] __methods = __declaringClass.getDeclaredMethods();
        List<String> __parameterTypeNames = parseParameters(__binding.___argumentsDescriptor);
        Method __match = null;
        for (int __i = 0; __i < __methods.length; ++__i)
        {
            Method __m = __methods[__i];
            if (__binding.___isStatic == Modifier.isStatic(__m.getModifiers()) && __m.getName().equals(__binding.___name) && __parameterTypeNames.equals(toInternalTypeNames(__m.getParameterTypes())))
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

    ///
    // Same as {@link #resolveMethod(Class, InvocationPlugins.Binding)} and
    // {@link #resolveConstructor(Class, InvocationPlugins.Binding)} except in terms of
    // {@link ResolvedJavaType} and {@link ResolvedJavaMethod}.
    ///
    public static ResolvedJavaMethod resolveJavaMethod(ResolvedJavaType __declaringClass, InvocationPlugins.Binding __binding)
    {
        ResolvedJavaMethod[] __methods = __declaringClass.getDeclaredMethods();
        if (__binding.___name.equals("<init>"))
        {
            for (ResolvedJavaMethod __m : __methods)
            {
                if (__m.getName().equals("<init>") && __m.getSignature().toMethodDescriptor().startsWith(__binding.___argumentsDescriptor))
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
            if (__binding.___isStatic == __m.isStatic() && __m.getName().equals(__binding.___name) && __m.getSignature().toMethodDescriptor().startsWith(__binding.___argumentsDescriptor))
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

    ///
    // Resolves a given binding to a constructor in a given class.
    //
    // @param declaringClass the class to search for a constructor matching {@code binding}
    // @return the constructor (if any) in {@code declaringClass} matching binding
    ///
    public static Constructor<?> resolveConstructor(Class<?> __declaringClass, InvocationPlugins.Binding __binding)
    {
        if (!__binding.___name.equals("<init>"))
        {
            return null;
        }
        Constructor<?>[] __constructors = __declaringClass.getDeclaredConstructors();
        List<String> __parameterTypeNames = parseParameters(__binding.___argumentsDescriptor);
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
