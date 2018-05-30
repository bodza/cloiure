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
    private ResolvedJavaMethod cachedSubstitute;

    /**
     * The class in which the substitute method is declared.
     */
    private final Class<?> declaringClass;

    /**
     * The name of the original and substitute method.
     */
    private final String name;

    /**
     * The parameter types of the substitute method.
     */
    private final Type[] parameters;

    private final boolean originalIsStatic;

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
    public MethodSubstitutionPlugin(BytecodeProvider bytecodeProvider, Class<?> declaringClass, String name, Type... parameters)
    {
        super();
        this.bytecodeProvider = bytecodeProvider;
        this.declaringClass = declaringClass;
        this.name = name;
        this.parameters = parameters;
        this.originalIsStatic = parameters.length == 0 || parameters[0] != InvocationPlugin.Receiver.class;
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
    public ResolvedJavaMethod getSubstitute(MetaAccessProvider metaAccess)
    {
        if (cachedSubstitute == null)
        {
            cachedSubstitute = metaAccess.lookupJavaMethod(getJavaSubstitute());
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
        Method substituteMethod = lookupSubstitute();
        int modifiers = substituteMethod.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers))
        {
            throw new GraalError("Substitution method must not be abstract or native: " + substituteMethod);
        }
        if (!Modifier.isStatic(modifiers))
        {
            throw new GraalError("Substitution method must be static: " + substituteMethod);
        }
        return substituteMethod;
    }

    /**
     * Determines if a given method is the substitute method of this plugin.
     */
    private boolean isSubstitute(Method m)
    {
        if (Modifier.isStatic(m.getModifiers()) && m.getName().equals(name))
        {
            if (parameters.length == m.getParameterCount())
            {
                Class<?>[] mparams = m.getParameterTypes();
                int start = 0;
                if (!originalIsStatic)
                {
                    start = 1;
                    if (!mparams[0].isAssignableFrom(InvocationPlugins.resolveType(parameters[0], false)))
                    {
                        return false;
                    }
                }
                for (int i = start; i < mparams.length; i++)
                {
                    if (mparams[i] != InvocationPlugins.resolveType(parameters[i], false))
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private Method lookupSubstitute(Method excluding)
    {
        for (Method m : declaringClass.getDeclaredMethods())
        {
            if (!m.equals(excluding) && isSubstitute(m))
            {
                return m;
            }
        }
        return null;
    }

    /**
     * Gets the substitute method of this plugin.
     */
    private Method lookupSubstitute()
    {
        Method m = lookupSubstitute(null);
        if (m != null)
        {
            return m;
        }
        throw new GraalError("No method found specified by %s", this);
    }

    @Override
    public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] argsIncludingReceiver)
    {
        ResolvedJavaMethod subst = getSubstitute(b.getMetaAccess());
        return b.intrinsify(bytecodeProvider, targetMethod, subst, receiver, argsIncludingReceiver);
    }
}
