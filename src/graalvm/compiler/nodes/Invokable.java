package graalvm.compiler.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;

/**
 * A marker interface for nodes that represent calls to other methods.
 */
public interface Invokable
{
    ResolvedJavaMethod getTargetMethod();

    int bci();

    default boolean isAlive()
    {
        return asFixedNode().isAlive();
    }

    FixedNode asFixedNode();
}
