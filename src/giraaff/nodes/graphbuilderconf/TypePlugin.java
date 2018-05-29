package giraaff.nodes.graphbuilderconf;

import jdk.vm.ci.meta.JavaType;

import giraaff.core.common.type.StampPair;

/**
 * Plugin for overriding types in the bytecode parser. This can be used to modify the standard
 * behavior of Java type resolution, e.g. to introduce trusted interface types with special semantics.
 */
// @iface TypePlugin
public interface TypePlugin extends GraphBuilderPlugin
{
    /**
     * Intercept the type of arguments or return values.
     */
    StampPair interceptType(GraphBuilderTool b, JavaType declaredType, boolean nonNull);
}
