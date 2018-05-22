package graalvm.compiler.nodes.graphbuilderconf;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;

/**
 * {@link GraphBuilderPlugin} interface for static compilation mode, allowing references to dynamic
 * types.
 */
public interface InvokeDynamicPlugin extends GraphBuilderPlugin
{
    /**
     * Checks for a resolved dynamic adapter method at the specified index, resulting from either a
     * resolved invokedynamic or invokevirtual on a signature polymorphic MethodHandle method
     * (HotSpot invokehandle).
     *
     * @param builder context for the invoke
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed
     * @return {@code true} if a signature polymorphic method reference was found, otherwise
     *         {@code false}
     */
    boolean isResolvedDynamicInvoke(GraphBuilderContext builder, int cpi, int opcode);

    /**
     * Checks if this plugin instance supports the specified dynamic invoke.
     *
     * @param builder context for the invoke
     * @param cpi the constant pool index
     * @param opcode the opcode of the invoke instruction
     * @return {@code true} if this dynamic invoke is supported
     */
    boolean supportsDynamicInvoke(GraphBuilderContext builder, int cpi, int opcode);

    /**
     * Notifies this object of the value and context of the dynamic method target (e.g., A HotSpot
     * adapter method) for a resolved dynamic invoke.
     *
     * @param builder context for the invoke
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed
     * @param target dynamic target method to record
     */
    void recordDynamicMethod(GraphBuilderContext builder, int cpi, int opcode, ResolvedJavaMethod target);

    /**
     * Notifies this object of the value and context of the dynamic appendix object for a resolved
     * dynamic invoke.
     *
     * @param builder context for the invoke
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed
     * @return {@link ValueNode} for appendix constant
     */
    ValueNode genAppendixNode(GraphBuilderContext builder, int cpi, int opcode, JavaConstant appendix, FrameState frameState);
}
