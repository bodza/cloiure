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
        private final GraphBuilderContext parser;
        private ValueNode[] args;
        private ValueNode value;

        // @cons
        public InvocationPluginReceiver(GraphBuilderContext parser)
        {
            super();
            this.parser = parser;
        }

        @Override
        public ValueNode get(boolean performNullCheck)
        {
            if (!performNullCheck)
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

        public InvocationPluginReceiver init(ResolvedJavaMethod targetMethod, ValueNode[] newArgs)
        {
            if (!targetMethod.isStatic())
            {
                this.args = newArgs;
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
        private final ResolvedJavaType resolved;

        // @cons
        public ResolvedJavaSymbol(ResolvedJavaType type)
        {
            super();
            this.resolved = type;
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
        private static final Class<?> MASK_NULL = OptionalLazySymbol.class;
        private final String name;
        private Class<?> resolved;

        // @cons
        OptionalLazySymbol(String name)
        {
            super();
            this.name = name;
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
                Class<?> resolvedOrNull = resolveClass(name, true);
                resolved = resolvedOrNull == null ? MASK_NULL : resolvedOrNull;
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
        private final InvocationPlugins plugins;
        private final Type declaringType;
        private final BytecodeProvider methodSubstitutionBytecodeProvider;
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
        public Registration(InvocationPlugins plugins, Type declaringType)
        {
            super();
            this.plugins = plugins;
            this.declaringType = declaringType;
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
        public Registration(InvocationPlugins plugins, Type declaringType, BytecodeProvider methodSubstitutionBytecodeProvider)
        {
            super();
            this.plugins = plugins;
            this.declaringType = declaringType;
            this.methodSubstitutionBytecodeProvider = methodSubstitutionBytecodeProvider;
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
        public Registration(InvocationPlugins plugins, String declaringClassName, BytecodeProvider methodSubstitutionBytecodeProvider)
        {
            super();
            this.plugins = plugins;
            this.declaringType = new OptionalLazySymbol(declaringClassName);
            this.methodSubstitutionBytecodeProvider = methodSubstitutionBytecodeProvider;
        }

        /**
         * Configures this registration to allow or disallow overwriting of invocation plugins.
         */
        public Registration setAllowOverwrite(boolean allowOverwrite)
        {
            this.allowOverwrite = allowOverwrite;
            return this;
        }

        /**
         * Registers a plugin for a method with no arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register0(String name, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name);
        }

        /**
         * Registers a plugin for a method with 1 argument.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register1(String name, Type arg, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg);
        }

        /**
         * Registers a plugin for a method with 2 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register2(String name, Type arg1, Type arg2, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2);
        }

        /**
         * Registers a plugin for a method with 3 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register3(String name, Type arg1, Type arg2, Type arg3, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2, arg3);
        }

        /**
         * Registers a plugin for a method with 4 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register4(String name, Type arg1, Type arg2, Type arg3, Type arg4, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2, arg3, arg4);
        }

        /**
         * Registers a plugin for a method with 5 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register5(String name, Type arg1, Type arg2, Type arg3, Type arg4, Type arg5, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2, arg3, arg4, arg5);
        }

        /**
         * Registers a plugin for a method with 6 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register6(String name, Type arg1, Type arg2, Type arg3, Type arg4, Type arg5, Type arg6, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2, arg3, arg4, arg5, arg6);
        }

        /**
         * Registers a plugin for a method with 7 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void register7(String name, Type arg1, Type arg2, Type arg3, Type arg4, Type arg5, Type arg6, Type arg7, InvocationPlugin plugin)
        {
            plugins.register(plugin, false, allowOverwrite, declaringType, name, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }

        /**
         * Registers a plugin for an optional method with no arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional0(String name, InvocationPlugin plugin)
        {
            plugins.register(plugin, true, allowOverwrite, declaringType, name);
        }

        /**
         * Registers a plugin for an optional method with 1 argument.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional1(String name, Type arg, InvocationPlugin plugin)
        {
            plugins.register(plugin, true, allowOverwrite, declaringType, name, arg);
        }

        /**
         * Registers a plugin for an optional method with 2 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional2(String name, Type arg1, Type arg2, InvocationPlugin plugin)
        {
            plugins.register(plugin, true, allowOverwrite, declaringType, name, arg1, arg2);
        }

        /**
         * Registers a plugin for an optional method with 3 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional3(String name, Type arg1, Type arg2, Type arg3, InvocationPlugin plugin)
        {
            plugins.register(plugin, true, allowOverwrite, declaringType, name, arg1, arg2, arg3);
        }

        /**
         * Registers a plugin for an optional method with 4 arguments.
         *
         * @param name the name of the method
         * @param plugin the plugin to be registered
         */
        public void registerOptional4(String name, Type arg1, Type arg2, Type arg3, Type arg4, InvocationPlugin plugin)
        {
            plugins.register(plugin, true, allowOverwrite, declaringType, name, arg1, arg2, arg3, arg4);
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
        public void registerMethodSubstitution(Class<?> substituteDeclaringClass, String name, Type... argumentTypes)
        {
            registerMethodSubstitution(substituteDeclaringClass, name, name, argumentTypes);
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
        public void registerMethodSubstitution(Class<?> substituteDeclaringClass, String name, String substituteName, Type... argumentTypes)
        {
            MethodSubstitutionPlugin plugin = createMethodSubstitution(substituteDeclaringClass, substituteName, argumentTypes);
            plugins.register(plugin, false, allowOverwrite, declaringType, name, argumentTypes);
        }

        public MethodSubstitutionPlugin createMethodSubstitution(Class<?> substituteDeclaringClass, String substituteName, Type... argumentTypes)
        {
            return new MethodSubstitutionPlugin(methodSubstitutionBytecodeProvider, substituteDeclaringClass, substituteName, argumentTypes);
        }
    }

    /**
     * Utility for registering plugins after Graal may have been initialized. Registrations made via
     * this class are not finalized until {@link #close} is called.
     */
    // @class InvocationPlugins.LateRegistration
    public static final class LateRegistration implements AutoCloseable
    {
        private InvocationPlugins plugins;
        private final List<Binding> bindings = new ArrayList<>();
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
        public LateRegistration(InvocationPlugins plugins, Type declaringType)
        {
            super();
            this.plugins = plugins;
            this.declaringType = declaringType;
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
        public void register(InvocationPlugin plugin, String name, Type... argumentTypes)
        {
            boolean isStatic = argumentTypes.length == 0 || argumentTypes[0] != InvocationPlugin.Receiver.class;
            if (!isStatic)
            {
                argumentTypes[0] = declaringType;
            }

            Binding binding = new Binding(plugin, isStatic, name, argumentTypes);
            bindings.add(binding);
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
        public final InvocationPlugin plugin;

        /**
         * Specifies if the associated method is static.
         */
        public final boolean isStatic;

        /**
         * The name of the associated method.
         */
        public final String name;

        /**
         * A partial
         * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">method
         * descriptor</a> for the associated method. The descriptor includes enclosing {@code '('}
         * and {@code ')'} characters but omits the return type suffix.
         */
        public final String argumentsDescriptor;

        /**
         * Link in a list of bindings.
         */
        private Binding next;

        // @cons
        Binding(InvocationPlugin data, boolean isStatic, String name, Type... argumentTypes)
        {
            super();
            this.plugin = data;
            this.isStatic = isStatic;
            this.name = name;
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (int i = isStatic ? 0 : 1; i < argumentTypes.length; i++)
            {
                sb.append(MetaUtil.toInternalName(argumentTypes[i].getTypeName()));
            }
            sb.append(')');
            this.argumentsDescriptor = sb.toString();
        }

        // @cons
        Binding(ResolvedJavaMethod resolved, InvocationPlugin data)
        {
            super();
            this.plugin = data;
            this.isStatic = resolved.isStatic();
            this.name = resolved.getName();
            Signature sig = resolved.getSignature();
            String desc = sig.toMethodDescriptor();
            this.argumentsDescriptor = desc.substring(0, desc.indexOf(')') + 1);
        }
    }

    /**
     * Plugin registrations for already resolved methods. If non-null, then {@link #registrations}
     * is null and no further registrations can be made.
     */
    private final UnmodifiableEconomicMap<ResolvedJavaMethod, InvocationPlugin> resolvedRegistrations;

    /**
     * Map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form to the
     * invocation plugin bindings for the class. Tf non-null, then {@link #resolvedRegistrations}
     * will be null.
     */
    private final EconomicMap<String, ClassPlugins> registrations;

    /**
     * Deferred registrations as well as the guard for delimiting the initial registration phase.
     * The guard uses double-checked locking which is why this field is {@code volatile}.
     */
    private volatile List<Runnable> deferredRegistrations = new ArrayList<>();

    /**
     * Adds a {@link Runnable} for doing registration deferred until the first time
     * {@link #get(ResolvedJavaMethod)} or {@link #closeRegistration()} is called on this object.
     */
    public void defer(Runnable deferrable)
    {
        deferredRegistrations.add(deferrable);
    }

    /**
     * Support for registering plugins once this object may be accessed by multiple threads.
     */
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
        private final EconomicMap<String, Binding> bindings = EconomicMap.create(Equivalence.DEFAULT);

        /**
         * Gets the invocation plugin for a given method.
         *
         * @return the invocation plugin for {@code method} or {@code null}
         */
        InvocationPlugin get(ResolvedJavaMethod method)
        {
            Binding binding = bindings.get(method.getName());
            while (binding != null)
            {
                if (method.isStatic() == binding.isStatic)
                {
                    if (method.getSignature().toMethodDescriptor().startsWith(binding.argumentsDescriptor))
                    {
                        return binding.plugin;
                    }
                }
                binding = binding.next;
            }
            return null;
        }

        public void register(Binding binding, boolean allowOverwrite)
        {
            if (allowOverwrite)
            {
                if (lookup(binding) != null)
                {
                    register(binding);
                    return;
                }
            }
            register(binding);
        }

        InvocationPlugin lookup(Binding binding)
        {
            Binding b = bindings.get(binding.name);
            while (b != null)
            {
                if (b.isStatic == binding.isStatic && b.argumentsDescriptor.equals(binding.argumentsDescriptor))
                {
                    return b.plugin;
                }
                b = b.next;
            }
            return null;
        }

        /**
         * Registers {@code binding}.
         */
        void register(Binding binding)
        {
            Binding head = bindings.get(binding.name);
            binding.next = head;
            bindings.put(binding.name, binding);
        }
    }

    // @class InvocationPlugins.LateClassPlugins
    static final class LateClassPlugins extends ClassPlugins
    {
        static final String CLOSED_LATE_CLASS_PLUGIN = "-----";

        private final String className;
        private final LateClassPlugins next;

        // @cons
        LateClassPlugins(LateClassPlugins next, String className)
        {
            super();
            this.next = next;
            this.className = className;
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
    Binding put(InvocationPlugin plugin, boolean isStatic, boolean allowOverwrite, Type declaringClass, String name, Type... argumentTypes)
    {
        String internalName = MetaUtil.toInternalName(declaringClass.getTypeName());

        ClassPlugins classPlugins = registrations.get(internalName);
        if (classPlugins == null)
        {
            classPlugins = new ClassPlugins();
            registrations.put(internalName, classPlugins);
        }
        Binding binding = new Binding(plugin, isStatic, name, argumentTypes);
        classPlugins.register(binding, allowOverwrite);
        return binding;
    }

    InvocationPlugin get(ResolvedJavaMethod method)
    {
        if (resolvedRegistrations != null)
        {
            return resolvedRegistrations.get(method);
        }
        else
        {
            if (!method.isBridge())
            {
                ResolvedJavaType declaringClass = method.getDeclaringClass();
                flushDeferrables();
                String internalName = declaringClass.getName();
                ClassPlugins classPlugins = registrations.get(internalName);
                InvocationPlugin res = null;
                if (classPlugins != null)
                {
                    res = classPlugins.get(method);
                }
                if (res == null)
                {
                    LateClassPlugins lcp = findLateClassPlugins(internalName);
                    if (lcp != null)
                    {
                        res = lcp.get(method);
                    }
                }
                if (res != null)
                {
                    // A decorator plugin is trusted since it does not replace
                    // the method it intrinsifies.
                    if (res.isDecorator() || canBeIntrinsified(declaringClass))
                    {
                        return res;
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
                            List<Binding> bindings = testExtensions.get(internalName);
                            if (bindings != null)
                            {
                                String name = method.getName();
                                String descriptor = method.getSignature().toMethodDescriptor();
                                for (Binding b : bindings)
                                {
                                    if (b.isStatic == method.isStatic() && b.name.equals(name) && descriptor.startsWith(b.argumentsDescriptor))
                                    {
                                        return b.plugin;
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
    public boolean canBeIntrinsified(ResolvedJavaType declaringClass)
    {
        return true;
    }

    LateClassPlugins findLateClassPlugins(String internalClassName)
    {
        for (LateClassPlugins lcp = lateRegistrations; lcp != null; lcp = lcp.next)
        {
            if (lcp.className.equals(internalClassName))
            {
                return lcp;
            }
        }
        return null;
    }

    // @class InvocationPlugins.InvocationPlugRegistrationError
    static final class InvocationPlugRegistrationError extends GraalError
    {
        // @cons
        InvocationPlugRegistrationError(Throwable cause)
        {
            super(cause);
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
                        for (Runnable deferrable : deferredRegistrations)
                        {
                            deferrable.run();
                        }
                        deferredRegistrations = null;
                    }
                    catch (InvocationPlugRegistrationError t)
                    {
                        throw t;
                    }
                    catch (Throwable t)
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
                                throw new InvocationPlugRegistrationError(t);
                            }
                        };
                        deferredRegistrations.add(rethrow);
                        rethrow.run();
                    }
                }
            }
        }
    }

    private volatile EconomicMap<String, List<Binding>> testExtensions;

    private static int findBinding(List<Binding> list, Binding key)
    {
        for (int i = 0; i < list.size(); i++)
        {
            Binding b = list.get(i);
            if (b.isStatic == key.isStatic && b.name.equals(key.name) && b.argumentsDescriptor.equals(key.argumentsDescriptor))
            {
                return i;
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
    public synchronized void addTestPlugins(InvocationPlugins other, List<Pair<String, Binding>> ignored)
    {
        EconomicMap<String, List<Binding>> otherBindings = other.getBindings(true, false);
        if (otherBindings.isEmpty())
        {
            return;
        }
        if (testExtensions == null)
        {
            testExtensions = EconomicMap.create();
        }
        MapCursor<String, List<Binding>> c = otherBindings.getEntries();
        while (c.advance())
        {
            String declaringClass = c.getKey();
            List<Binding> bindings = testExtensions.get(declaringClass);
            if (bindings == null)
            {
                bindings = new ArrayList<>();
                testExtensions.put(declaringClass, bindings);
            }
            for (Binding b : c.getValue())
            {
                int index = findBinding(bindings, b);
                if (index != -1)
                {
                    if (ignored != null)
                    {
                        ignored.add(Pair.create(declaringClass, b));
                    }
                }
                else
                {
                    bindings.add(b);
                }
            }
        }
    }

    /**
     * Removes the plugins from {@code other} in this object that were added by
     * {@link #addTestPlugins}.
     */
    public synchronized void removeTestPlugins(InvocationPlugins other)
    {
        if (testExtensions != null)
        {
            MapCursor<String, List<Binding>> c = other.getBindings(false).getEntries();
            while (c.advance())
            {
                String declaringClass = c.getKey();
                List<Binding> bindings = testExtensions.get(declaringClass);
                if (bindings != null)
                {
                    for (Binding b : c.getValue())
                    {
                        int index = findBinding(bindings, b);
                        if (index != -1)
                        {
                            bindings.remove(index);
                        }
                    }
                    if (bindings.isEmpty())
                    {
                        testExtensions.removeKey(declaringClass);
                    }
                }
            }
            if (testExtensions.isEmpty())
            {
                testExtensions = null;
            }
        }
    }

    synchronized void registerLate(Type declaringType, List<Binding> bindings)
    {
        String internalName = MetaUtil.toInternalName(declaringType.getTypeName());
        LateClassPlugins lateClassPlugins = new LateClassPlugins(lateRegistrations, internalName);
        for (Binding b : bindings)
        {
            lateClassPlugins.register(b);
        }
        lateRegistrations = lateClassPlugins;
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
    public InvocationPlugins(InvocationPlugins parent)
    {
        super();
        InvocationPlugins p = parent;
        this.parent = p;
        this.registrations = EconomicMap.create();
        this.resolvedRegistrations = null;
    }

    /**
     * Creates a closed set of invocation plugins for a set of resolved methods. Such an object
     * cannot have further plugins registered.
     */
    // @cons
    public InvocationPlugins(Map<ResolvedJavaMethod, InvocationPlugin> plugins, InvocationPlugins parent)
    {
        super();
        this.parent = parent;
        this.registrations = null;
        this.deferredRegistrations = null;
        EconomicMap<ResolvedJavaMethod, InvocationPlugin> map = EconomicMap.create(plugins.size());

        for (Map.Entry<ResolvedJavaMethod, InvocationPlugin> entry : plugins.entrySet())
        {
            map.put(entry.getKey(), entry.getValue());
        }
        this.resolvedRegistrations = map;
    }

    protected void register(InvocationPlugin plugin, boolean isOptional, boolean allowOverwrite, Type declaringClass, String name, Type... argumentTypes)
    {
        boolean isStatic = argumentTypes.length == 0 || argumentTypes[0] != InvocationPlugin.Receiver.class;
        if (!isStatic)
        {
            argumentTypes[0] = declaringClass;
        }
        Binding binding = put(plugin, isStatic, allowOverwrite, declaringClass, name, argumentTypes);
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
    public void register(InvocationPlugin plugin, Type declaringClass, String name, Type... argumentTypes)
    {
        register(plugin, false, false, declaringClass, name, argumentTypes);
    }

    public void register(InvocationPlugin plugin, String declaringClass, String name, Type... argumentTypes)
    {
        register(plugin, false, false, new OptionalLazySymbol(declaringClass), name, argumentTypes);
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
    public void registerOptional(InvocationPlugin plugin, Type declaringClass, String name, Type... argumentTypes)
    {
        register(plugin, true, false, declaringClass, name, argumentTypes);
    }

    /**
     * Gets the plugin for a given method.
     *
     * @param method the method to lookup
     * @return the plugin associated with {@code method} or {@code null} if none exists
     */
    public InvocationPlugin lookupInvocation(ResolvedJavaMethod method)
    {
        if (parent != null)
        {
            InvocationPlugin plugin = parent.lookupInvocation(method);
            if (plugin != null)
            {
                return plugin;
            }
        }
        return get(method);
    }

    /**
     * Gets the set of registered invocation plugins.
     *
     * @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
     *         to the invocation plugin bindings for methods in the class
     */
    public EconomicMap<String, List<Binding>> getBindings(boolean includeParents)
    {
        return getBindings(includeParents, true);
    }

    /**
     * Gets the set of registered invocation plugins.
     *
     * @return a map from class names in {@linkplain MetaUtil#toInternalName(String) internal} form
     *         to the invocation plugin bindings for methods in the class
     */
    private EconomicMap<String, List<Binding>> getBindings(boolean includeParents, boolean flushDeferrables)
    {
        EconomicMap<String, List<Binding>> res = EconomicMap.create(Equivalence.DEFAULT);
        if (parent != null && includeParents)
        {
            res.putAll(parent.getBindings(true, flushDeferrables));
        }
        if (resolvedRegistrations != null)
        {
            UnmodifiableMapCursor<ResolvedJavaMethod, InvocationPlugin> cursor = resolvedRegistrations.getEntries();
            while (cursor.advance())
            {
                ResolvedJavaMethod method = cursor.getKey();
                InvocationPlugin plugin = cursor.getValue();
                String type = method.getDeclaringClass().getName();
                List<Binding> bindings = res.get(type);
                if (bindings == null)
                {
                    bindings = new ArrayList<>();
                    res.put(type, bindings);
                }
                bindings.add(new Binding(method, plugin));
            }
        }
        else
        {
            if (flushDeferrables)
            {
                flushDeferrables();
            }
            MapCursor<String, ClassPlugins> classes = registrations.getEntries();
            while (classes.advance())
            {
                String type = classes.getKey();
                ClassPlugins cp = classes.getValue();
                collectBindingsTo(res, type, cp);
            }
            for (LateClassPlugins lcp = lateRegistrations; lcp != null; lcp = lcp.next)
            {
                String type = lcp.className;
                collectBindingsTo(res, type, lcp);
            }
            if (testExtensions != null)
            {
                // Avoid the synchronization in the common case that there are no test extensions.
                synchronized (this)
                {
                    if (testExtensions != null)
                    {
                        MapCursor<String, List<Binding>> c = testExtensions.getEntries();
                        while (c.advance())
                        {
                            String name = c.getKey();
                            List<Binding> bindings = res.get(name);
                            if (bindings == null)
                            {
                                bindings = new ArrayList<>();
                                res.put(name, bindings);
                            }
                            bindings.addAll(c.getValue());
                        }
                    }
                }
            }
        }
        return res;
    }

    private static void collectBindingsTo(EconomicMap<String, List<Binding>> res, String type, ClassPlugins cp)
    {
        MapCursor<String, Binding> methods = cp.bindings.getEntries();
        while (methods.advance())
        {
            List<Binding> bindings = res.get(type);
            if (bindings == null)
            {
                bindings = new ArrayList<>();
                res.put(type, bindings);
            }
            for (Binding b = methods.getValue(); b != null; b = b.next)
            {
                bindings.add(b);
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
    public static Class<?> resolveClass(String className, boolean optional)
    {
        try
        {
            // Need to use the system class loader to handle classes loaded by the application
            // class loader, which is not delegated to by the JVMCI class loader.
            return Class.forName(className, false, ClassLoader.getSystemClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            if (optional)
            {
                return null;
            }
            throw new GraalError("Could not resolve type " + className);
        }
    }

    /**
     * Resolves a {@link Type} to a {@link Class}.
     *
     * @param type the type to resolve
     * @param optional if true, resolution failure returns null
     * @return the resolved class or null if resolution fails and {@code optional} is true
     */
    public static Class<?> resolveType(Type type, boolean optional)
    {
        if (type instanceof Class)
        {
            return (Class<?>) type;
        }
        if (type instanceof OptionalLazySymbol)
        {
            return ((OptionalLazySymbol) type).resolve();
        }
        return resolveClass(type.getTypeName(), optional);
    }

    private static List<String> toInternalTypeNames(Class<?>[] types)
    {
        String[] res = new String[types.length];
        for (int i = 0; i < types.length; i++)
        {
            res[i] = MetaUtil.toInternalName(types[i].getTypeName());
        }
        return Arrays.asList(res);
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
    public static Method resolveMethod(Class<?> declaringClass, Binding binding)
    {
        if (binding.name.equals("<init>"))
        {
            return null;
        }
        Method[] methods = declaringClass.getDeclaredMethods();
        List<String> parameterTypeNames = parseParameters(binding.argumentsDescriptor);
        Method match = null;
        for (int i = 0; i < methods.length; ++i)
        {
            Method m = methods[i];
            if (binding.isStatic == Modifier.isStatic(m.getModifiers()) && m.getName().equals(binding.name) && parameterTypeNames.equals(toInternalTypeNames(m.getParameterTypes())))
            {
                if (match == null)
                {
                    match = m;
                }
                else if (match.getReturnType().isAssignableFrom(m.getReturnType()))
                {
                    // 'm' has a more specific return type - choose it
                    // ('match' is most likely a bridge method)
                    match = m;
                }
                else
                {
                    if (!m.getReturnType().isAssignableFrom(match.getReturnType()))
                    {
                        throw new NoSuchMethodError(String.format("Found 2 methods with same name and parameter types but unrelated return types:%n %s%n %s", match, m));
                    }
                }
            }
        }
        return match;
    }

    /**
     * Same as {@link #resolveMethod(Class, Binding)} and
     * {@link #resolveConstructor(Class, Binding)} except in terms of {@link ResolvedJavaType} and
     * {@link ResolvedJavaMethod}.
     */
    public static ResolvedJavaMethod resolveJavaMethod(ResolvedJavaType declaringClass, Binding binding)
    {
        ResolvedJavaMethod[] methods = declaringClass.getDeclaredMethods();
        if (binding.name.equals("<init>"))
        {
            for (ResolvedJavaMethod m : methods)
            {
                if (m.getName().equals("<init>") && m.getSignature().toMethodDescriptor().startsWith(binding.argumentsDescriptor))
                {
                    return m;
                }
            }
            return null;
        }

        ResolvedJavaMethod match = null;
        for (int i = 0; i < methods.length; ++i)
        {
            ResolvedJavaMethod m = methods[i];
            if (binding.isStatic == m.isStatic() && m.getName().equals(binding.name) && m.getSignature().toMethodDescriptor().startsWith(binding.argumentsDescriptor))
            {
                if (match == null)
                {
                    match = m;
                }
                else
                {
                    final ResolvedJavaType matchReturnType = (ResolvedJavaType) match.getSignature().getReturnType(declaringClass);
                    final ResolvedJavaType mReturnType = (ResolvedJavaType) m.getSignature().getReturnType(declaringClass);
                    if (matchReturnType.isAssignableFrom(mReturnType))
                    {
                        // 'm' has a more specific return type - choose it
                        // ('match' is most likely a bridge method)
                        match = m;
                    }
                    else
                    {
                        if (!mReturnType.isAssignableFrom(matchReturnType))
                        {
                            throw new NoSuchMethodError(String.format("Found 2 methods with same name and parameter types but unrelated return types:%n %s%n %s", match, m));
                        }
                    }
                }
            }
        }
        return match;
    }

    /**
     * Resolves a given binding to a constructor in a given class.
     *
     * @param declaringClass the class to search for a constructor matching {@code binding}
     * @return the constructor (if any) in {@code declaringClass} matching binding
     */
    public static Constructor<?> resolveConstructor(Class<?> declaringClass, Binding binding)
    {
        if (!binding.name.equals("<init>"))
        {
            return null;
        }
        Constructor<?>[] constructors = declaringClass.getDeclaredConstructors();
        List<String> parameterTypeNames = parseParameters(binding.argumentsDescriptor);
        for (int i = 0; i < constructors.length; ++i)
        {
            Constructor<?> c = constructors[i];
            if (parameterTypeNames.equals(toInternalTypeNames(c.getParameterTypes())))
            {
                return c;
            }
        }
        return null;
    }

    private static List<String> parseParameters(String argumentsDescriptor)
    {
        List<String> res = new ArrayList<>();
        int cur = 1;
        int end = argumentsDescriptor.length() - 1;
        while (cur != end)
        {
            char first;
            int start = cur;
            do
            {
                first = argumentsDescriptor.charAt(cur++);
            } while (first == '[');

            switch (first)
            {
                case 'L':
                    int endObject = argumentsDescriptor.indexOf(';', cur);
                    if (endObject == -1)
                    {
                        throw new GraalError("Invalid object type at index %d in signature: %s", cur, argumentsDescriptor);
                    }
                    cur = endObject + 1;
                    break;
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
                    throw new GraalError("Invalid character at index %d in signature: %s", cur, argumentsDescriptor);
            }
            res.add(argumentsDescriptor.substring(start, cur));
        }
        return res;
    }
}
