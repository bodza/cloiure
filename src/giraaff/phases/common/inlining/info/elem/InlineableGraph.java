package giraaff.phases.common.inlining.info.elem;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeInputList;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.AllowAssumptions;
import giraaff.nodes.ValueNode;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.graph.FixedNodeProbabilityCache;
import giraaff.phases.tiers.HighTierContext;

/**
 * Represents a feasible concrete target for inlining, whose graph has been copied already and thus
 * can be modified without affecting the original (usually cached) version.
 *
 * Instances of this class don't make sense in isolation but as part of an
 * {@link giraaff.phases.common.inlining.info.InlineInfo InlineInfo}.
 *
 * @see giraaff.phases.common.inlining.walker.InliningData#moveForward()
 * @see giraaff.phases.common.inlining.walker.CallsiteHolderExplorable
 */
public class InlineableGraph implements Inlineable
{
    private final StructuredGraph graph;

    private FixedNodeProbabilityCache probabilites = new FixedNodeProbabilityCache();

    public InlineableGraph(final ResolvedJavaMethod method, final Invoke invoke, final HighTierContext context, CanonicalizerPhase canonicalizer)
    {
        StructuredGraph original = getOriginalGraph(method, context, canonicalizer, invoke.asNode().graph(), invoke.bci());
        // TODO copying the graph is only necessary if it is modified or if it contains any invokes
        this.graph = (StructuredGraph) original.copy();
        specializeGraphToArguments(invoke, context, canonicalizer);
    }

    /**
     * This method looks up in a cache the graph for the argument, if not found bytecode is parsed.
     * The graph thus obtained is returned, ie the caller is responsible for cloning before modification.
     */
    private static StructuredGraph getOriginalGraph(final ResolvedJavaMethod method, final HighTierContext context, CanonicalizerPhase canonicalizer, StructuredGraph caller, int callerBci)
    {
        StructuredGraph result = InliningUtil.getIntrinsicGraph(context.getReplacements(), method, callerBci);
        if (result != null)
        {
            return result;
        }
        return parseBytecodes(method, context, canonicalizer, caller);
    }

    /**
     * @return true iff one or more parameters <code>newGraph</code> were specialized to account for
     *         a constant argument, or an argument with a more specific stamp.
     */
    private boolean specializeGraphToArguments(final Invoke invoke, final HighTierContext context, CanonicalizerPhase canonicalizer)
    {
        ArrayList<Node> parameterUsages = replaceParamsWithMoreInformativeArguments(invoke, context);
        if (parameterUsages != null)
        {
            canonicalizer.applyIncremental(graph, context, parameterUsages);
            return true;
        }
        else
        {
            // TODO if args are not more concrete, inlining should be avoided in most cases or we
            // could at least use the previous graph size + invoke probability to check the inlining
            return false;
        }
    }

    private static boolean isArgMoreInformativeThanParam(ValueNode arg, ParameterNode param)
    {
        return arg.isConstant() || canStampBeImproved(arg, param);
    }

    private static boolean canStampBeImproved(ValueNode arg, ParameterNode param)
    {
        return improvedStamp(arg, param) != null;
    }

    private static Stamp improvedStamp(ValueNode arg, ParameterNode param)
    {
        Stamp joinedStamp = param.stamp(NodeView.DEFAULT).join(arg.stamp(NodeView.DEFAULT));
        if (joinedStamp == null || joinedStamp.equals(param.stamp(NodeView.DEFAULT)))
        {
            return null;
        }
        return joinedStamp;
    }

    /**
     * This method detects:
     *
     * <li>constants among the arguments to the <code>invoke</code></li>
     * <li>arguments with more precise type than that declared by the corresponding parameter</li>
     *
     * The corresponding parameters are updated to reflect the above information. Before doing so,
     * their usages are added to <code>parameterUsages</code> for later incremental canonicalization.
     *
     * @return null if no incremental canonicalization is need, a list of nodes for such
     *         canonicalization otherwise.
     */
    private ArrayList<Node> replaceParamsWithMoreInformativeArguments(final Invoke invoke, final HighTierContext context)
    {
        NodeInputList<ValueNode> args = invoke.callTarget().arguments();
        ArrayList<Node> parameterUsages = null;
        List<ParameterNode> params = graph.getNodes(ParameterNode.TYPE).snapshot();
        /*
         * param-nodes that aren't used (eg, as a result of canonicalization) don't occur in
         * 'params'. Thus, in general, the sizes of 'params' and 'args' don't always match. Still,
         * it's always possible to pair a param-node with its corresponding arg-node using
         * param.index() as index into 'args'.
         */
        for (ParameterNode param : params)
        {
            if (param.usages().isNotEmpty())
            {
                ValueNode arg = args.get(param.index());
                if (arg.isConstant())
                {
                    ConstantNode constant = (ConstantNode) arg;
                    parameterUsages = trackParameterUsages(param, parameterUsages);
                    // collect param usages before replacing the param
                    param.replaceAtUsagesAndDelete(graph.unique(ConstantNode.forConstant(arg.stamp(NodeView.DEFAULT), constant.getValue(), constant.getStableDimension(), constant.isDefaultStable(), context.getMetaAccess())));
                    // param-node gone, leaving a gap in the sequence given by param.index()
                }
                else
                {
                    Stamp impro = improvedStamp(arg, param);
                    if (impro != null)
                    {
                        param.setStamp(impro);
                        parameterUsages = trackParameterUsages(param, parameterUsages);
                    }
                }
            }
        }
        return parameterUsages;
    }

    private static ArrayList<Node> trackParameterUsages(ParameterNode param, ArrayList<Node> parameterUsages)
    {
        ArrayList<Node> result = (parameterUsages == null) ? new ArrayList<>() : parameterUsages;
        param.usages().snapshotTo(result);
        return result;
    }

    /**
     * This method builds the IR nodes for the given <code>method</code> and canonicalizes them.
     * Provided profiling info is mature, the resulting graph is cached. The caller is responsible
     * for cloning before modification.
     */
    private static StructuredGraph parseBytecodes(ResolvedJavaMethod method, HighTierContext context, CanonicalizerPhase canonicalizer, StructuredGraph caller)
    {
        StructuredGraph newGraph = new StructuredGraph.Builder(caller.getOptions(), AllowAssumptions.ifNonNull(caller.getAssumptions())).method(method).build();
        if (!caller.isUnsafeAccessTrackingEnabled())
        {
            newGraph.disableUnsafeAccessTracking();
        }
        if (context.getGraphBuilderSuite() != null)
        {
            context.getGraphBuilderSuite().apply(newGraph, context);
        }

        new DeadCodeEliminationPhase(Optionality.Optional).apply(newGraph);

        canonicalizer.apply(newGraph, context);

        return newGraph;
    }

    @Override
    public int getNodeCount()
    {
        return InliningUtil.getNodeCount(graph);
    }

    @Override
    public Iterable<Invoke> getInvokes()
    {
        return graph.getInvokes();
    }

    @Override
    public double getProbability(Invoke invoke)
    {
        return probabilites.applyAsDouble(invoke.asNode());
    }

    public StructuredGraph getGraph()
    {
        return graph;
    }
}
