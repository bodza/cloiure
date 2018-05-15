package graalvm.graphio;

/**
 * Representation of methods, fields, their signatures and code locations.
 *
 * @param <M> type representing methods
 * @param <F> type representing fields
 * @param <S> type representing signature
 * @param <P> type representing source code location
 */
public interface GraphElements<M, F, S, P> {
    /**
     * Recognize method. Can the object be seen as a method?
     *
     * @param obj the object to check
     * @return <code>null</code> if the object isn't a method, non-null value otherwise
     */
    M method(Object obj);

    /**
     * Bytecode for a method.
     *
     * @param method the method
     * @return bytecode of the method
     */
    byte[] methodCode(M method);

    /**
     * Method modifiers.
     *
     * @param method the method
     * @return its modifiers
     */
    int methodModifiers(M method);

    /**
     * Method's signature.
     *
     * @param method the method
     * @return signature of the method
     */
    S methodSignature(M method);

    /**
     * Method name.
     *
     * @param method the method
     * @return name of the method
     */
    String methodName(M method);

    /**
     * Method's declaring class. The returned object shall be a {@link Class} or be recognizable by
     * {@link GraphTypes#typeName(java.lang.Object)} method.
     *
     * @param method the method
     * @return object representing class that defined the method
     */
    Object methodDeclaringClass(M method);

    /**
     * Recognizes a field. Can the object be seen as a field?
     *
     * @param object the object to check
     * @return <code>null</code> if the object isn't a field, non-null value otherwise
     */
    F field(Object object);

    /**
     * Field modifiers.
     *
     * @param field the field
     * @return field modifiers
     */
    int fieldModifiers(F field);

    /**
     * Type name of the field.
     *
     * @param field the field
     * @return the name of the field's type
     */
    String fieldTypeName(F field);

    /**
     * Name of a field.
     *
     * @param field the field
     * @return the name of the field
     */
    String fieldName(F field);

    /**
     * Field's declaring class. The returned object shall be a {@link Class} or be recognizable by
     * {@link GraphTypes#typeName(java.lang.Object)} method.
     *
     * @param field the field
     * @return object representing class that defined the field
     */
    Object fieldDeclaringClass(F field);

    /**
     * Recognizes signature. Can the object be seen as a signature?
     *
     * @param object the object to check
     * @return <code>null</code> if the object isn't a signature, non-null value otherwise
     */
    S signature(Object object);

    /**
     * Number of parameters of a signature.
     *
     * @param signature the signature
     * @return number of parameters
     */
    int signatureParameterCount(S signature);

    /**
     * Type name of a signature parameter.
     *
     * @param signature the signature
     * @param index index from 0 to {@link #signatureParameterCount(java.lang.Object)} - 1
     * @return the type name
     */
    String signatureParameterTypeName(S signature, int index);

    /**
     * Type name of a return type.
     *
     * @param signature the signature
     * @return the type name
     */
    String signatureReturnTypeName(S signature);

    /**
     * Recognize a source position. Can the object be seen as a position?
     *
     * @param object the object to check
     * @return <code>null</code> if the object isn't a position, non-null otherwise
     */
    P nodeSourcePosition(Object object);

    /**
     * Method for a position.
     *
     * @param pos the position
     * @return the method at the position
     */
    M nodeSourcePositionMethod(P pos);

    /**
     * Caller of a position.
     *
     * @param pos the position
     * @return <code>null</code> or another position
     */
    P nodeSourcePositionCaller(P pos);

    /**
     * Byte code index of a position.
     *
     * @param pos the position
     * @return the BCI of the position
     */
    int nodeSourcePositionBCI(P pos);

    /**
     * Stack trace element for a method, index and position. This is the basic version of the method
     * that works with {@link StackTraceElement} and is suitable for Java-like languages. Should you
     * need to provide more details about the location of multiple strata, see
     * {@link GraphLocations} interface that gives more control over the provided location data.
     *
     * @param method the method
     * @param bci the index
     * @param pos the position
     * @return stack trace element for the method, index and position
     */
    StackTraceElement methodStackTraceElement(M method, int bci, P pos);

}
