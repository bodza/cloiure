package giraaff.nodes.graphbuilderconf;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.BytecodeProvider;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.InvocationPlugins;
import giraaff.util.GraalError;

/**
 * An {@link InvocationPlugin} for a method where the implementation of the method is provided by a
 * {@linkplain #getSubstitute(MetaAccessProvider) substitute} method. A substitute method must be
 * static even if the substituted method is not.
 *
 * While performing intrinsification with method substitutions is simpler than writing an
 * {@link InvocationPlugin} that does manual graph weaving, it has a higher compile time cost than
 * the latter; parsing bytecodes to create nodes is slower than simply creating nodes. As such, the
 * recommended practice is to use {@link MethodSubstitutionPlugin} only for complex
 * intrinsifications which is typically those using non-straight-line control flow.
 */
// @class MethodSubstitutionPlugin
public final class MethodSubstitutionPlugin implements InvocationPlugin
{
    // @field
    private ResolvedJavaMethod cachedSubstitute;

    /**
     * The class in which the substitute method is declared.
     */
    // @field
    private final Class<?> declaringClass;

    /**
     * The name of the original and substitute method.
     */
    // @field
    private final String name;

    /**
     * The parameter types of the substitute method.
     */
    // @field
    private final Type[] parameters;

    // @field
    private final boolean originalIsStatic;

    // @field
    private final BytecodeProvider bytecodeProvider;

    /**
     * Creates a method substitution plugin.
     *
     * @param bytecodeProvider used to get the bytecodes to parse for the substitute method
     * @param declaringClass the class in which the substitute method is declared
     * @param name the name of the substitute method
     * @param parameters the parameter types of the substitute method. If the original method is not
     *            static, then {@code parameters[0]} must be the {@link Class} value denoting
     *            {@link InvocationPlugin.Receiver}
     */
    // @cons
    public MethodSubstitutionPlugin(BytecodeProvider __bytecodeProvider, Class<?> __declaringClass, String __name, Type... __parameters)
    {
        super();
        this.bytecodeProvider = __bytecodeProvider;
        this.declaringClass = __declaringClass;
        this.name = __name;
        this.parameters = __parameters;
        this.originalIsStatic = __parameters.length == 0 || __parameters[0] != InvocationPlugin.Receiver.class;
    }

    @Override
    public boolean inlineOnly()
    {
        // conservatively assume MacroNodes may be used in a substitution
        return true;
    }

    /**
     * Gets the substitute method, resolving it first if necessary.
     */
    public ResolvedJavaMethod getSubstitute(MetaAccessProvider __metaAccess)
    {
        if (cachedSubstitute == null)
        {
            cachedSubstitute = __metaAccess.lookupJavaMethod(getJavaSubstitute());
        }
        return cachedSubstitute;
    }

    /**
     * Gets the object used to access the bytecodes of the substitute method.
     */
    public BytecodeProvider getBytecodeProvider()
    {
        return bytecodeProvider;
    }

    /**
     * Gets the reflection API version of the substitution method.
     */
    Method getJavaSubstitute()
    {
        Method __substituteMethod = lookupSubstitute();
        int __modifiers = __substituteMethod.getModifiers();
        if (Modifier.isAbstract(__modifiers) || Modifier.isNative(__modifiers))
        {
            throw new GraalError("Substitution method must not be abstract or native: " + __substituteMethod);
        }
        if (!Modifier.isStatic(__modifiers))
        {
            throw new GraalError("Substitution method must be static: " + __substituteMethod);
        }
        return __substituteMethod;
    }

    /**
     * Determines if a given method is the substitute method of this plugin.
     */
    private boolean isSubstitute(Method __m)
    {
        if (Modifier.isStatic(__m.getModifiers()) && __m.getName().equals(name))
        {
            if (parameters.length == __m.getParameterCount())
            {
                Class<?>[] __mparams = __m.getParameterTypes();
                int __start = 0;
                if (!originalIsStatic)
                {
                    __start = 1;
                    if (!__mparams[0].isAssignableFrom(InvocationPlugins.resolveType(parameters[0], false)))
                    {
                        return false;
                    }
                }
                for (int __i = __start; __i < __mparams.length; __i++)
                {
                    if (__mparams[__i] != InvocationPlugins.resolveType(parameters[__i], false))
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private Method lookupSubstitute(Method __excluding)
    {
        for (Method __m : declaringClass.getDeclaredMethods())
        {
            if (!__m.equals(__excluding) && isSubstitute(__m))
            {
                return __m;
            }
        }
        return null;
    }

    /**
     * Gets the substitute method of this plugin.
     */
    private Method lookupSubstitute()
    {
        Method __m = lookupSubstitute(null);
        if (__m != null)
        {
            return __m;
        }
        throw new GraalError("No method found specified by %s", this);
    }

    @Override
    public boolean execute(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, InvocationPlugin.Receiver __receiver, ValueNode[] __argsIncludingReceiver)
    {
        ResolvedJavaMethod __subst = getSubstitute(__b.getMetaAccess());
        return __b.intrinsify(bytecodeProvider, __targetMethod, __subst, __receiver, __argsIncludingReceiver);
    }
}
