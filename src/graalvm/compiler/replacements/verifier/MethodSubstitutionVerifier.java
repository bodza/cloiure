package graalvm.compiler.replacements.verifier;

import java.lang.annotation.Annotation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;

public final class MethodSubstitutionVerifier extends AbstractVerifier
{
    public MethodSubstitutionVerifier(ProcessingEnvironment env)
    {
        super(env);
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass()
    {
        return MethodSubstitution.class;
    }

    @SuppressWarnings("unused")
    @Override
    public void verify(Element element, AnnotationMirror annotation, PluginGenerator generator)
    {
        if (element.getKind() != ElementKind.METHOD)
        {
            return;
        }
        ExecutableElement substitutionMethod = (ExecutableElement) element;
        TypeElement substitutionType = findEnclosingClass(substitutionMethod);

        AnnotationMirror substitutionClassAnnotation = VerifierAnnotationProcessor.findAnnotationMirror(env, substitutionType.getAnnotationMirrors(), ClassSubstitution.class);
        if (substitutionClassAnnotation == null)
        {
            env.getMessager().printMessage(Kind.ERROR, String.format("A @%s annotation is required on the enclosing class.", ClassSubstitution.class.getSimpleName()), element, annotation);
            return;
        }
        boolean optional = resolveAnnotationValue(Boolean.class, findAnnotationValue(substitutionClassAnnotation, "optional"));
        if (optional)
        {
            return;
        }

        TypeElement originalType = ClassSubstitutionVerifier.resolveOriginalType(env, substitutionType, substitutionClassAnnotation);
        if (originalType == null)
        {
            env.getMessager().printMessage(Kind.ERROR, String.format("The @%s annotation is invalid on the enclosing class.", ClassSubstitution.class.getSimpleName()), element, annotation);
            return;
        }

        if (!substitutionMethod.getModifiers().contains(Modifier.STATIC))
        {
            env.getMessager().printMessage(Kind.ERROR, String.format("A @%s method must be static.", MethodSubstitution.class.getSimpleName()), element, annotation);
        }

        if (substitutionMethod.getModifiers().contains(Modifier.ABSTRACT) || substitutionMethod.getModifiers().contains(Modifier.NATIVE))
        {
            env.getMessager().printMessage(Kind.ERROR, String.format("A @%s method must not be native or abstract.", MethodSubstitution.class.getSimpleName()), element, annotation);
        }
    }

    private boolean isTypeCompatible(TypeMirror originalType, TypeMirror substitutionType)
    {
        TypeMirror original = originalType;
        TypeMirror substitution = substitutionType;
        if (needsErasure(original))
        {
            original = env.getTypeUtils().erasure(original);
        }
        if (needsErasure(substitution))
        {
            substitution = env.getTypeUtils().erasure(substitution);
        }
        return env.getTypeUtils().isSameType(original, substitution);
    }

    /**
     * Tests whether one type is a subtype of another. Any type is considered to be a subtype of
     * itself.
     *
     * @param t1 the first type
     * @param t2 the second type
     * @return {@code true} if and only if the first type is a subtype of the second
     */
    private boolean isSubtype(TypeMirror t1, TypeMirror t2)
    {
        TypeMirror t1Erased = t1;
        TypeMirror t2Erased = t2;
        if (needsErasure(t1Erased))
        {
            t1Erased = env.getTypeUtils().erasure(t1Erased);
        }
        if (needsErasure(t2Erased))
        {
            t2Erased = env.getTypeUtils().erasure(t2Erased);
        }
        return env.getTypeUtils().isSubtype(t1Erased, t2Erased);
    }

    private static boolean needsErasure(TypeMirror typeMirror)
    {
        return typeMirror.getKind() != TypeKind.NONE && typeMirror.getKind() != TypeKind.VOID && !typeMirror.getKind().isPrimitive() && typeMirror.getKind() != TypeKind.OTHER && typeMirror.getKind() != TypeKind.NULL;
    }

    private static TypeElement findEnclosingClass(Element element)
    {
        if (element.getKind().isClass())
        {
            return (TypeElement) element;
        }

        Element enclosing = element.getEnclosingElement();
        while (enclosing != null && enclosing.getKind() != ElementKind.PACKAGE)
        {
            if (enclosing.getKind().isClass())
            {
                return (TypeElement) enclosing;
            }
            enclosing = enclosing.getEnclosingElement();
        }
        return null;
    }
}
