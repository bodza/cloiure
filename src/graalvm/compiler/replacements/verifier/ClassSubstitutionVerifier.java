package graalvm.compiler.replacements.verifier;

import java.lang.annotation.Annotation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import graalvm.compiler.api.replacements.ClassSubstitution;

public final class ClassSubstitutionVerifier extends AbstractVerifier
{
    private static final String TYPE_VALUE = "value";
    private static final String STRING_VALUE = "className";
    private static final String OPTIONAL = "optional";

    public ClassSubstitutionVerifier(ProcessingEnvironment env)
    {
        super(env);
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass()
    {
        return ClassSubstitution.class;
    }

    @Override
    public void verify(Element element, AnnotationMirror classSubstitution, PluginGenerator generator)
    {
        if (!element.getKind().isClass())
        {
            return;
        }
        TypeElement type = (TypeElement) element;

        TypeElement substitutionType = resolveOriginalType(env, type, classSubstitution);
        if (substitutionType == null)
        {
            return;
        }
    }

    static TypeElement resolveOriginalType(ProcessingEnvironment env, Element sourceElement, AnnotationMirror classSubstition)
    {
        AnnotationValue typeValue = findAnnotationValue(classSubstition, TYPE_VALUE);
        AnnotationValue stringValue = findAnnotationValue(classSubstition, STRING_VALUE);
        AnnotationValue optionalValue = findAnnotationValue(classSubstition, OPTIONAL);

        TypeMirror type = resolveAnnotationValue(TypeMirror.class, typeValue);
        String[] classNames = resolveAnnotationValue(String[].class, stringValue);
        boolean optional = resolveAnnotationValue(Boolean.class, optionalValue);

        if (type.getKind() != TypeKind.DECLARED)
        {
            env.getMessager().printMessage(Kind.ERROR, "The provided class must be a declared type.", sourceElement, classSubstition, typeValue);
            return null;
        }

        if (!classSubstition.getAnnotationType().asElement().equals(((DeclaredType) type).asElement()))
        {
            if (classNames.length != 0)
            {
                String msg = "The usage of value and className is exclusive.";
                env.getMessager().printMessage(Kind.ERROR, msg, sourceElement, classSubstition, stringValue);
                env.getMessager().printMessage(Kind.ERROR, msg, sourceElement, classSubstition, typeValue);
            }

            return (TypeElement) ((DeclaredType) type).asElement();
        }

        if (classNames.length != 0)
        {
            TypeElement typeElement = null;
            for (String className : classNames)
            {
                typeElement = env.getElementUtils().getTypeElement(className);
                if (typeElement != null)
                {
                    break;
                }
            }
            if (typeElement == null && !optional)
            {
                env.getMessager().printMessage(Kind.ERROR, String.format("The class '%s' was not found on the classpath.", stringValue), sourceElement, classSubstition, stringValue);
            }

            return typeElement;
        }

        if (!optional)
        {
            env.getMessager().printMessage(Kind.ERROR, String.format("No value for '%s' or '%s' provided but required.", TYPE_VALUE, STRING_VALUE), sourceElement, classSubstition);
        }

        return null;
    }
}
