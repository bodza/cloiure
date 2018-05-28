package giraaff.api.replacements;

import java.util.Objects;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Reflection operations on values represented as {@linkplain JavaConstant constants} for the
 * processing of snippets. Snippets need a direct access to the value of object constants, which is
 * not allowed in other parts of Graal to enforce compiler-VM separation.
 *
 * This interface must not be used in Graal code that is not related to snippet processing.
 */
public interface SnippetReflectionProvider
{
    /**
     * Creates a boxed {@link JavaKind#Object object} constant.
     *
     * @param object the object value to box
     * @return a constant containing {@code object}
     */
    JavaConstant forObject(Object object);

    /**
     * Gets the object reference a given constant represents if it is of a given type. The constant
     * must have kind {@link JavaKind#Object}.
     *
     * @param type the expected type of the object represented by {@code constant}. If the object is
     *            required to be of this type, then wrap the call to this method in
     *            {@link Objects#requireNonNull(Object)}.
     * @param constant an object constant
     * @return the object value represented by {@code constant} cast to {@code type} if it is an
     *         {@link Class#isInstance(Object) instance of} {@code type} otherwise {@code null}
     */
    <T> T asObject(Class<T> type, JavaConstant constant);

    /**
     * Gets the object reference a given constant represents if it is of a given type. The constant
     * must have kind {@link JavaKind#Object}.
     *
     * @param type the expected type of the object represented by {@code constant}. If the object is
     *            required to be of this type, then wrap the call to this method in
     *            {@link Objects#requireNonNull(Object)}.
     * @param constant an object constant
     * @return the object value represented by {@code constant} if it is an
     *         {@link ResolvedJavaType#isInstance(JavaConstant) instance of} {@code type} otherwise
     *         {@code null}
     */
    Object asObject(ResolvedJavaType type, JavaConstant constant);

    /**
     * Creates a boxed constant for the given kind from an Object. The object needs to be of the
     * Java boxed type corresponding to the kind.
     *
     * @param kind the kind of the constant to create
     * @param value the Java boxed value: a {@link Byte} instance for {@link JavaKind#Byte}, etc.
     * @return the boxed copy of {@code value}
     */
    JavaConstant forBoxed(JavaKind kind, Object value);
}
