package graalvm.compiler.replacements.verifier;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;
import graalvm.compiler.replacements.verifier.InjectedDependencies.WellKnownDependency;

/**
 * Create graph builder plugins for {@link Fold} methods.
 */
public class GeneratedFoldPlugin extends GeneratedPlugin
{
    public GeneratedFoldPlugin(ExecutableElement intrinsicMethod)
    {
        super(intrinsicMethod);
    }

    @Override
    protected Class<? extends Annotation> getAnnotationClass()
    {
        return Fold.class;
    }

    private static TypeMirror stringType(ProcessingEnvironment env)
    {
        return env.getElementUtils().getTypeElement("java.lang.String").asType();
    }

    @Override
    public void extraImports(Set<String> imports)
    {
        imports.add("jdk.vm.ci.meta.JavaConstant");
        imports.add("jdk.vm.ci.meta.JavaKind");
        imports.add("graalvm.compiler.nodes.ConstantNode");
    }

    @Override
    protected InjectedDependencies createExecute(ProcessingEnvironment env, PrintWriter out)
    {
        InjectedDependencies deps = new InjectedDependencies();
        List<? extends VariableElement> params = intrinsicMethod.getParameters();

        int argCount = 0;
        Object receiver;
        if (intrinsicMethod.getModifiers().contains(Modifier.STATIC))
        {
            receiver = intrinsicMethod.getEnclosingElement();
        }
        else
        {
            receiver = "arg0";
            TypeElement type = (TypeElement) intrinsicMethod.getEnclosingElement();
            constantArgument(env, out, deps, argCount, type.asType(), argCount);
            argCount++;
        }

        int firstArg = argCount;
        for (VariableElement param : params)
        {
            if (param.getAnnotation(InjectedParameter.class) == null)
            {
                constantArgument(env, out, deps, argCount, param.asType(), argCount);
            }
            else
            {
                out.printf("            assert checkInjectedArgument(b, args[%d], targetMethod);\n", argCount);
                out.printf("            %s arg%d = %s;\n", param.asType(), argCount, deps.use(env, (DeclaredType) param.asType()));
            }
            argCount++;
        }

        Set<String> suppressWarnings = new TreeSet<>();
        if (intrinsicMethod.getAnnotation(Deprecated.class) != null)
        {
            suppressWarnings.add("deprecation");
        }
        if (hasRawtypeWarning(intrinsicMethod.getReturnType()))
        {
            suppressWarnings.add("rawtypes");
        }
        for (VariableElement param : params)
        {
            if (hasUncheckedWarning(param.asType()))
            {
                suppressWarnings.add("unchecked");
            }
        }
        if (suppressWarnings.size() > 0)
        {
            out.printf("            @SuppressWarnings({");
            String sep = "";
            for (String suppressWarning : suppressWarnings)
            {
                out.printf("%s\"%s\"", sep, suppressWarning);
                sep = ", ";
            }
            out.printf("})\n");
        }

        out.printf("            %s result = %s.%s(", getErasedType(intrinsicMethod.getReturnType()), receiver, intrinsicMethod.getSimpleName());
        if (argCount > firstArg)
        {
            out.printf("arg%d", firstArg);
            for (int i = firstArg + 1; i < argCount; i++)
            {
                out.printf(", arg%d", i);
            }
        }
        out.printf(");\n");

        TypeMirror returnType = intrinsicMethod.getReturnType();
        switch (returnType.getKind())
        {
            case BOOLEAN:
                out.printf("            JavaConstant constant = JavaConstant.forInt(result ? 1 : 0);\n");
                break;
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
                out.printf("            JavaConstant constant = JavaConstant.forInt(result);\n");
                break;
            case LONG:
                out.printf("            JavaConstant constant = JavaConstant.forLong(result);\n");
                break;
            case FLOAT:
                out.printf("            JavaConstant constant = JavaConstant.forFloat(result);\n");
                break;
            case DOUBLE:
                out.printf("            JavaConstant constant = JavaConstant.forDouble(result);\n");
                break;
            case ARRAY:
            case TYPEVAR:
            case DECLARED:
                if (returnType.equals(stringType(env)))
                {
                    out.printf("            JavaConstant constant = %s.forString(result);\n", deps.use(WellKnownDependency.CONSTANT_REFLECTION));
                }
                else
                {
                    out.printf("            JavaConstant constant = %s.forObject(result);\n", deps.use(WellKnownDependency.SNIPPET_REFLECTION));
                }
                break;
            default:
                throw new IllegalArgumentException(returnType.toString());
        }

        out.printf("            ConstantNode node = ConstantNode.forConstant(constant, %s, %s);\n", deps.use(WellKnownDependency.META_ACCESS), deps.use(WellKnownDependency.STRUCTURED_GRAPH));
        out.printf("            b.push(JavaKind.%s, node);\n", getReturnKind(intrinsicMethod));
        out.printf("            b.notifyReplacedCall(targetMethod, node);\n");
        out.printf("            return true;\n");

        return deps;
    }
}
