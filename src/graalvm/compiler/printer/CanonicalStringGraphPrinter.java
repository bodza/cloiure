package graalvm.compiler.printer;

import static graalvm.compiler.debug.DebugOptions.CanonicalGraphStringsCheckConstants;
import static graalvm.compiler.debug.DebugOptions.CanonicalGraphStringsExcludeVirtuals;
import static graalvm.compiler.debug.DebugOptions.CanonicalGraphStringsRemoveIdentities;
import static graalvm.compiler.debug.DebugOptions.PrintCanonicalGraphStringFlavor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.core.common.Fields;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.PathUtilities;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeMap;
import graalvm.compiler.graph.Position;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.FullInfopointNode;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.ProxyNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public class CanonicalStringGraphPrinter implements GraphPrinter
{
    private static final Pattern IDENTITY_PATTERN = Pattern.compile("([A-Za-z0-9$_]+)@[0-9a-f]+");
    private final SnippetReflectionProvider snippetReflection;

    public CanonicalStringGraphPrinter(SnippetReflectionProvider snippetReflection)
    {
        this.snippetReflection = snippetReflection;
    }

    @Override
    public SnippetReflectionProvider getSnippetReflectionProvider()
    {
        return snippetReflection;
    }

    private static String removeIdentities(String str)
    {
        return IDENTITY_PATTERN.matcher(str).replaceAll("$1");
    }

    protected static void writeCanonicalGraphExpressionString(ValueNode node, boolean checkConstants, boolean removeIdentities, PrintWriter writer)
    {
        writer.print(node.getClass().getSimpleName());
        writer.print("(");
        Fields properties = node.getNodeClass().getData();
        for (int i = 0; i < properties.getCount(); i++)
        {
            String dataStr = String.valueOf(properties.get(node, i));
            if (removeIdentities)
            {
                dataStr = removeIdentities(dataStr);
            }
            writer.print(dataStr);
            if (i + 1 < properties.getCount() || node.inputPositions().iterator().hasNext())
            {
                writer.print(", ");
            }
        }
        Iterator<Position> iterator = node.inputPositions().iterator();
        while (iterator.hasNext())
        {
            Position position = iterator.next();
            Node input = position.get(node);
            if (checkConstants && input instanceof ConstantNode)
            {
                ConstantNode constantNode = (ConstantNode) input;
                String valueString = constantNode.getValue().toValueString();
                if (removeIdentities)
                {
                    valueString = removeIdentities(valueString);
                }
                writer.print(valueString);
            }
            else if (input instanceof ValueNode && !(input instanceof PhiNode) && !(input instanceof FixedNode))
            {
                writeCanonicalGraphExpressionString((ValueNode) input, checkConstants, removeIdentities, writer);
            }
            else if (input == null)
            {
                writer.print("null");
            }
            else
            {
                writer.print(input.getClass().getSimpleName());
            }
            if (iterator.hasNext())
            {
                writer.print(", ");
            }
        }
        writer.print(")");
    }

    protected static void writeCanonicalExpressionCFGString(StructuredGraph graph, boolean checkConstants, boolean removeIdentities, PrintWriter writer)
    {
        ControlFlowGraph controlFlowGraph = getControlFlowGraph(graph);
        if (controlFlowGraph == null)
        {
            return;
        }
        try
        {
            for (Block block : controlFlowGraph.getBlocks())
            {
                writer.print("Block ");
                writer.print(block);
                writer.print(" ");
                if (block == controlFlowGraph.getStartBlock())
                {
                    writer.print("* ");
                }
                writer.print("-> ");
                for (Block successor : block.getSuccessors())
                {
                    writer.print(successor);
                    writer.print(" ");
                }
                writer.println();
                FixedNode node = block.getBeginNode();
                while (node != null)
                {
                    writeCanonicalGraphExpressionString(node, checkConstants, removeIdentities, writer);
                    writer.println();
                    if (node instanceof FixedWithNextNode)
                    {
                        node = ((FixedWithNextNode) node).next();
                    }
                    else
                    {
                        node = null;
                    }
                }
            }
        }
        catch (Throwable e)
        {
            writer.println();
            e.printStackTrace(writer);
        }
    }

    protected static ControlFlowGraph getControlFlowGraph(StructuredGraph graph)
    {
        try
        {
            return ControlFlowGraph.compute(graph, true, true, false, false);
        }
        catch (Throwable e)
        {
            // Ignore a non-well formed graph
            return null;
        }
    }

    protected static void writeCanonicalGraphString(StructuredGraph graph, boolean excludeVirtual, boolean checkConstants, PrintWriter writer)
    {
        StructuredGraph.ScheduleResult scheduleResult = GraphPrinter.getScheduleOrNull(graph);
        if (scheduleResult == null)
        {
            return;
        }
        try
        {
            NodeMap<Integer> canonicalId = graph.createNodeMap();
            int nextId = 0;

            List<String> constantsLines = null;
            if (checkConstants)
            {
                constantsLines = new ArrayList<>();
            }

            for (Block block : scheduleResult.getCFG().getBlocks())
            {
                writer.print("Block ");
                writer.print(block);
                writer.print(" ");
                if (block == scheduleResult.getCFG().getStartBlock())
                {
                    writer.print("* ");
                }
                writer.print("-> ");
                for (Block successor : block.getSuccessors())
                {
                    writer.print(successor);
                    writer.print(" ");
                }
                writer.println();
                for (Node node : scheduleResult.getBlockToNodesMap().get(block))
                {
                    if (node instanceof ValueNode && node.isAlive())
                    {
                        if (!excludeVirtual || !(node instanceof VirtualObjectNode || node instanceof ProxyNode || node instanceof FullInfopointNode))
                        {
                            if (node instanceof ConstantNode)
                            {
                                if (constantsLines != null)
                                {
                                    String name = node.toString(Verbosity.Name);
                                    String str = name + (excludeVirtual ? "" : "    (" + filteredUsageCount(node) + ")");
                                    constantsLines.add(str);
                                }
                            }
                            else
                            {
                                int id;
                                if (canonicalId.get(node) != null)
                                {
                                    id = canonicalId.get(node);
                                }
                                else
                                {
                                    id = nextId++;
                                    canonicalId.set(node, id);
                                }
                                String name = node.getClass().getSimpleName();
                                writer.print("  ");
                                writer.print(id);
                                writer.print("|");
                                writer.print(name);
                                if (!excludeVirtual)
                                {
                                    writer.print("    (");
                                    writer.print(filteredUsageCount(node));
                                    writer.print(")");
                                }
                                writer.println();
                            }
                        }
                    }
                }
            }
            if (constantsLines != null)
            {
                writer.print(constantsLines.size());
                writer.println(" constants:");
                Collections.sort(constantsLines);
                for (String s : constantsLines)
                {
                    writer.println(s);
                }
            }
        }
        catch (Throwable t)
        {
            writer.println();
            t.printStackTrace(writer);
        }
    }

    public static String getCanonicalGraphString(StructuredGraph graph, boolean excludeVirtual, boolean checkConstants)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writeCanonicalGraphString(graph, excludeVirtual, checkConstants, writer);
        writer.flush();
        return stringWriter.toString();
    }

    private static int filteredUsageCount(Node node)
    {
        return node.usages().filter(n -> !(n instanceof FrameState)).count();
    }

    @Override
    public void beginGroup(DebugContext debug, String name, String shortName, ResolvedJavaMethod method, int bci, Map<Object, Object> properties) throws IOException
    {
    }

    private StructuredGraph currentGraph;
    private Path currentDirectory;

    private Path getDirectory(DebugContext debug, StructuredGraph graph)
    {
        if (graph == currentGraph)
        {
            return currentDirectory;
        }
        currentDirectory = debug.getDumpPath(".graph-strings", true);
        currentGraph = graph;
        return currentDirectory;
    }

    @Override
    public void print(DebugContext debug, Graph graph, Map<Object, Object> properties, int id, String format, Object... args) throws IOException
    {
        if (graph instanceof StructuredGraph)
        {
            OptionValues options = graph.getOptions();
            StructuredGraph structuredGraph = (StructuredGraph) graph;
            Path outDirectory = getDirectory(debug, structuredGraph);
            String title = String.format("%03d-%s.txt", id, String.format(format, simplifyClassArgs(args)));
            Path filePath = outDirectory.resolve(PathUtilities.sanitizeFileName(title));
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toFile()))))
            {
                switch (PrintCanonicalGraphStringFlavor.getValue(options))
                {
                    case 1:
                        writeCanonicalExpressionCFGString(structuredGraph, CanonicalGraphStringsCheckConstants.getValue(options), CanonicalGraphStringsRemoveIdentities.getValue(options), writer);
                        break;
                    case 0:
                    default:
                        writeCanonicalGraphString(structuredGraph, CanonicalGraphStringsExcludeVirtuals.getValue(options), CanonicalGraphStringsCheckConstants.getValue(options), writer);
                        break;
                }
            }
        }
    }

    @Override
    public void endGroup() throws IOException
    {
    }

    @Override
    public void close()
    {
    }
}
