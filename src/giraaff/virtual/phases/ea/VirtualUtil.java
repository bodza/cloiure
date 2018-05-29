package giraaff.virtual.phases.ea;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.StructuredGraph;
import giraaff.util.GraalError;

// @class VirtualUtil
public final class VirtualUtil
{
    // @cons
    private VirtualUtil()
    {
        super();
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
