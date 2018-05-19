package graalvm.compiler.core.match;

import java.util.List;

import graalvm.compiler.core.gen.NodeLIRBuilder;
import graalvm.compiler.core.match.MatchPattern.MatchResultCode;
import graalvm.compiler.core.match.MatchPattern.Result;
import graalvm.compiler.graph.GraalGraphError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodeinfo.Verbosity;

import jdk.vm.ci.meta.Value;

/**
 * A named {@link MatchPattern} along with a {@link MatchGenerator} that can be evaluated to replace
 * one or more {@link Node}s with a single {@link Value}.
 */

public class MatchStatement
{
    /**
     * A printable name for this statement. Usually it's just the name of the method doing the
     * emission.
     */
    private final String name;

    /**
     * The actual match pattern.
     */
    private final MatchPattern pattern;

    /**
     * The method in the {@link NodeLIRBuilder} subclass that will actually do the code emission.
     */
    private MatchGenerator generatorMethod;

    /**
     * The name of arguments in the order they are expected to be passed to the generator method.
     */
    private String[] arguments;

    public MatchStatement(String name, MatchPattern pattern, MatchGenerator generator, String[] arguments)
    {
        this.name = name;
        this.pattern = pattern;
        this.generatorMethod = generator;
        this.arguments = arguments;
    }

    /**
     * Attempt to match the current statement against a Node.
     *
     * @param builder the current builder instance.
     * @param node the node to be matched
     * @param nodes the nodes in the current block
     * @return true if the statement matched something and set a {@link ComplexMatchResult} to be
     *         evaluated by the NodeLIRBuilder.
     */
    public boolean generate(NodeLIRBuilder builder, int index, Node node, List<Node> nodes)
    {
        // Check that the basic shape matches
        Result result = pattern.matchShape(node, this);
        if (result != Result.OK)
        {
            return false;
        }
        // Now ensure that the other safety constraints are matched.
        MatchContext context = new MatchContext(builder, this, index, node, nodes);
        result = pattern.matchUsage(node, context);
        if (result == Result.OK)
        {
            // Invoke the generator method and set the result if it's non null.
            ComplexMatchResult value = generatorMethod.match(builder.getNodeMatchRules(), buildArgList(context));
            if (value != null)
            {
                context.setResult(value);
                return true;
            }
        }
        return false;
    }

    /**
     * @param context
     * @return the Nodes captured by the match rule in the order expected by the generatorMethod
     */
    private Object[] buildArgList(MatchContext context)
    {
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++)
        {
            if ("root".equals(arguments[i]))
            {
                result[i] = context.getRoot();
            }
            else
            {
                result[i] = context.namedNode(arguments[i]);
                if (result[i] == null)
                {
                    throw new GraalGraphError("Can't find named node %s", arguments[i]);
                }
            }
        }
        return result;
    }

    public String formatMatch(Node root)
    {
        return pattern.formatMatch(root);
    }

    public MatchPattern getPattern()
    {
        return pattern;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return pattern.toString();
    }
}
