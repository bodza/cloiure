package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import giraaff.nodes.ValueNode;

// @iface NodePlugin
public interface NodePlugin extends GraphBuilderPlugin
{
    ///
    // Handle the parsing of a method invocation bytecode to a method that can be bound statically.
    // If the method returns true, it must {@link GraphBuilderContext#push push} a value as the
    // result of the method invocation using the {@link Signature#getReturnKind return kind} of the method.
    //
    // @param b the context
    // @param method the statically bound, invoked method
    // @param args the arguments of the method invocation
    // @return true if the plugin handles the invocation, false otherwise
    ///
    default boolean handleInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        return false;
    }

    ///
    // Handle the parsing of a GETFIELD bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value using the
    // {@link ResolvedJavaField#getJavaKind() kind} of the field.
    //
    // @param b the context
    // @param object the receiver object for the field access
    // @param field the accessed field
    // @return true if the plugin handles the field access, false otherwise
    ///
    default boolean handleLoadField(GraphBuilderContext __b, ValueNode __object, ResolvedJavaField __field)
    {
        return false;
    }

    ///
    // Handle the parsing of a GETSTATIC bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value using the
    // {@link ResolvedJavaField#getJavaKind() kind} of the field.
    //
    // @param b the context
    // @param field the accessed field
    // @return true if the plugin handles the field access, false otherwise
    ///
    default boolean handleLoadStaticField(GraphBuilderContext __b, ResolvedJavaField __field)
    {
        return false;
    }

    ///
    // Handle the parsing of a PUTFIELD bytecode.
    //
    // @param b the context
    // @param object the receiver object for the field access
    // @param field the accessed field
    // @param value the value to be stored into the field
    // @return true if the plugin handles the field access, false otherwise
    ///
    default boolean handleStoreField(GraphBuilderContext __b, ValueNode __object, ResolvedJavaField __field, ValueNode __value)
    {
        return false;
    }

    ///
    // Handle the parsing of a PUTSTATIC bytecode.
    //
    // @param b the context
    // @param field the accessed field
    // @param value the value to be stored into the field
    // @return true if the plugin handles the field access, false otherwise.
    ///
    default boolean handleStoreStaticField(GraphBuilderContext __b, ResolvedJavaField __field, ValueNode __value)
    {
        return false;
    }

    ///
    // Handle the parsing of an array load bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value using the provided elementKind.
    //
    // @param b the context
    // @param array the accessed array
    // @param index the index for the array access
    // @param elementKind the element kind of the accessed array
    // @return true if the plugin handles the array access, false otherwise.
    ///
    default boolean handleLoadIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind)
    {
        return false;
    }

    ///
    // Handle the parsing of an array store bytecode.
    //
    // @param b the context
    // @param array the accessed array
    // @param index the index for the array access
    // @param elementKind the element kind of the accessed array
    // @param value the value to be stored into the array
    // @return true if the plugin handles the array access, false otherwise.
    ///
    default boolean handleStoreIndexed(GraphBuilderContext __b, ValueNode __array, ValueNode __index, JavaKind __elementKind, ValueNode __value)
    {
        return false;
    }

    ///
    // Handle the parsing of a CHECKCAST bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value with the result of the cast using
    // {@link JavaKind#Object}.
    //
    // @param b the context
    // @param object the object to be type checked
    // @param type the type that the object is checked against
    // @param profile the profiling information for the type check, or null if no profiling
    //            information is available
    // @return true if the plugin handles the cast, false otherwise
    ///
    default boolean handleCheckCast(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        return false;
    }

    ///
    // Handle the parsing of a INSTANCEOF bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value with the result of the instanceof using
    // {@link JavaKind#Int}.
    //
    // @param b the context
    // @param object the object to be type checked
    // @param type the type that the object is checked against
    // @param profile the profiling information for the type check, or null if no profiling
    //            information is available
    // @return true if the plugin handles the instanceof, false otherwise
    ///
    default boolean handleInstanceOf(GraphBuilderContext __b, ValueNode __object, ResolvedJavaType __type, JavaTypeProfile __profile)
    {
        return false;
    }

    ///
    // Handle the parsing of a NEW bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value with the result of the allocation using
    // {@link JavaKind#Object}.
    //
    // @param b the context
    // @param type the type to be instantiated
    // @return true if the plugin handles the bytecode, false otherwise
    ///
    default boolean handleNewInstance(GraphBuilderContext __b, ResolvedJavaType __type)
    {
        return false;
    }

    ///
    // Handle the parsing of a NEWARRAY and ANEWARRAY bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value with the result of the allocation using
    // {@link JavaKind#Object}.
    //
    // @param b the context
    // @param elementType the element type of the array to be instantiated
    // @param length the length of the new array
    // @return true if the plugin handles the bytecode, false otherwise
    ///
    default boolean handleNewArray(GraphBuilderContext __b, ResolvedJavaType __elementType, ValueNode __length)
    {
        return false;
    }

    ///
    // Handle the parsing of a MULTIANEWARRAY bytecode. If the method returns true, it must
    // {@link GraphBuilderContext#push push} a value with the result of the allocation using
    // {@link JavaKind#Object}.
    //
    // @param b the context
    // @param type the type of the outermost array to be instantiated
    // @param dimensions the array of lengths for all the dimensions to be instantiated
    // @return true if the plugin handles the bytecode, false otherwise
    ///
    default boolean handleNewMultiArray(GraphBuilderContext __b, ResolvedJavaType __type, ValueNode[] __dimensions)
    {
        return false;
    }

    ///
    // If the plugin {@link GraphBuilderContext#push pushes} a value with a different
    // {@link JavaKind} than specified by the bytecode, it must override this method and return
    // {@code true}. This disables assertion checking for value kinds.
    //
    // @param b the context
    ///
    default boolean canChangeStackKind(GraphBuilderContext __b)
    {
        return false;
    }
}
