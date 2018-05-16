package graalvm.compiler.printer;

import java.util.ArrayList;
import java.util.List;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.DebugDumpHandler;
import graalvm.compiler.debug.DebugHandler;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.debug.DebugOptions;
import graalvm.compiler.debug.TTY;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.serviceprovider.ServiceProvider;

@ServiceProvider(DebugHandlersFactory.class)
public class GraalDebugHandlersFactory implements DebugHandlersFactory
{
    private final SnippetReflectionProvider snippetReflection;

    public GraalDebugHandlersFactory()
    {
        this.snippetReflection = null;
    }

    public GraalDebugHandlersFactory(SnippetReflectionProvider snippetReflection)
    {
        this.snippetReflection = snippetReflection;
    }

    @Override
    public List<DebugHandler> createHandlers(OptionValues options)
    {
        List<DebugHandler> handlers = new ArrayList<>();
        handlers.add(new GraphPrinterDumpHandler((debug, graph) -> new BinaryGraphPrinter(debug, snippetReflection)));
        if (DebugOptions.PrintCanonicalGraphStrings.getValue(options))
        {
            handlers.add(new GraphPrinterDumpHandler((debug, graph) -> createStringPrinter(snippetReflection)));
        }
        handlers.add(new NodeDumper());
        if (DebugOptions.PrintCFG.getValue(options) || DebugOptions.PrintBackendCFG.getValue(options))
        {
            if (DebugOptions.PrintCFG.getValue(options))
            {
                TTY.out.println("Complete C1Visualizer dumping slows down PrintBinaryGraphs: use -Dgraal.PrintCFG=false to disable it");
            }
            handlers.add(new CFGPrinterObserver());
        }
        handlers.add(new NoDeadCodeVerifyHandler());
        return handlers;
    }

    private static class NodeDumper implements DebugDumpHandler
    {
        @Override
        public void dump(DebugContext debug, Object object, String format, Object... arguments)
        {
            if (object instanceof Node)
            {
                Node node = (Node) object;
                String location = GraphUtil.approxSourceLocation(node);
                String nodeName = node.toString(Verbosity.Debugger);
                if (location != null)
                {
                    debug.log("Context obj %s (approx. location: %s)", nodeName, location);
                }
                else
                {
                    debug.log("Context obj %s", nodeName);
                }
            }
        }
    }

    private static CanonicalStringGraphPrinter createStringPrinter(SnippetReflectionProvider snippetReflection)
    {
        return new CanonicalStringGraphPrinter(snippetReflection);
    }
}
