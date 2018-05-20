package graalvm.compiler.virtual.phases.ea;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.nodes.StructuredGraph;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public final class VirtualUtil
{
    private VirtualUtil()
    {
        GraalError.shouldNotReachHere();
    }

    public static boolean matches(StructuredGraph graph, String filter)
    {
        if (filter != null)
        {
            return matchesHelper(graph, filter);
        }
        return true;
    }

    private static boolean matchesHelper(StructuredGraph graph, String filter)
    {
        if (filter.startsWith("~"))
        {
            ResolvedJavaMethod method = graph.method();
            return method == null || !method.format("%H.%n").contains(filter.substring(1));
        }
        else
        {
            ResolvedJavaMethod method = graph.method();
            return method != null && method.format("%H.%n").contains(filter);
        }
    }
}
