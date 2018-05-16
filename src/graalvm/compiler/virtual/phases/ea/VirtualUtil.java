package graalvm.compiler.virtual.phases.ea;

import static graalvm.compiler.core.common.GraalOptions.TraceEscapeAnalysis;

import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.TTY;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeFlood;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public final class VirtualUtil
{
    private VirtualUtil()
    {
        GraalError.shouldNotReachHere();
    }

    public static boolean assertNonReachable(StructuredGraph graph, List<Node> obsoleteNodes)
    {
        // Helper code that determines the paths that keep obsolete nodes alive.
        // Nodes with support for GVN can be kept alive by GVN and are therefore not part of the
        // assertion.

        DebugContext debug = graph.getDebug();
        NodeFlood flood = graph.createNodeFlood();
        EconomicMap<Node, Node> path = EconomicMap.create(Equivalence.IDENTITY);
        flood.add(graph.start());
        for (Node current : flood)
        {
            if (current instanceof AbstractEndNode)
            {
                AbstractEndNode end = (AbstractEndNode) current;
                flood.add(end.merge());
                if (!path.containsKey(end.merge()))
                {
                    path.put(end.merge(), end);
                }
            }
            else
            {
                for (Node successor : current.successors())
                {
                    flood.add(successor);
                    if (!path.containsKey(successor))
                    {
                        path.put(successor, current);
                    }
                }
            }
        }

        for (Node node : obsoleteNodes)
        {
            if (node instanceof FixedNode && !node.isDeleted())
            {
                assert !flood.isMarked(node) : node;
            }
        }

        for (Node node : graph.getNodes())
        {
            if (flood.isMarked(node))
            {
                for (Node input : node.inputs())
                {
                    flood.add(input);
                    if (!path.containsKey(input))
                    {
                        path.put(input, node);
                    }
                }
            }
        }
        for (Node current : flood)
        {
            for (Node input : current.inputs())
            {
                flood.add(input);
                if (!path.containsKey(input))
                {
                    path.put(input, current);
                }
            }
        }
        boolean success = true;
        for (Node node : obsoleteNodes)
        {
            if (!node.isDeleted() && flood.isMarked(node) && !node.getNodeClass().valueNumberable())
            {
                TTY.println("offending node path:");
                Node current = node;
                TTY.print(current.toString());
                while (true)
                {
                    current = path.get(current);
                    if (current != null)
                    {
                        TTY.print(" -> " + current.toString());
                        if (current instanceof FixedNode && !obsoleteNodes.contains(current))
                        {
                            break;
                        }
                    }
                }
                success = false;
            }
        }
        if (!success)
        {
            TTY.println();
            debug.forceDump(graph, "assertNonReachable");
        }
        return success;
    }

    public static void trace(OptionValues options, DebugContext debug, String msg)
    {
        if (debug.areScopesEnabled() && TraceEscapeAnalysis.getValue(options) && debug.isLogEnabled())
        {
            debug.log(msg);
        }
    }

    public static void trace(OptionValues options, DebugContext debug, String format, Object obj)
    {
        if (debug.areScopesEnabled() && TraceEscapeAnalysis.getValue(options) && debug.isLogEnabled())
        {
            debug.logv(format, obj);
        }
    }

    public static void trace(OptionValues options, DebugContext debug, String format, Object obj, Object obj2)
    {
        if (debug.areScopesEnabled() && TraceEscapeAnalysis.getValue(options) && debug.isLogEnabled())
        {
            debug.logv(format, obj, obj2);
        }
    }

    public static void trace(OptionValues options, DebugContext debug, String format, Object obj, Object obj2, Object obj3)
    {
        if (debug.areScopesEnabled() && TraceEscapeAnalysis.getValue(options) && debug.isLogEnabled())
        {
            debug.logv(format, obj, obj2, obj3);
        }
    }

    public static void trace(OptionValues options, DebugContext debug, String format, Object obj, Object obj2, Object obj3, Object obj4)
    {
        if (debug.areScopesEnabled() && TraceEscapeAnalysis.getValue(options) && debug.isLogEnabled())
        {
            debug.logv(format, obj, obj2, obj3, obj4);
        }
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
