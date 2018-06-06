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
import giraaff.nodes.ValueNode;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.graph.FixedNodeProbabilityCache;
import giraaff.phases.tiers.HighTierContext;

///
// Represents a feasible concrete target for inlining, whose graph has been copied already and thus
// can be modified without affecting the original (usually cached) version.
//
// Instances of this class don't make sense in isolation but as part of an
// {@link giraaff.phases.common.inlining.info.InlineInfo InlineInfo}.
//
// @see giraaff.phases.common.inlining.walker.InliningData#moveForward()
// @see giraaff.phases.common.inlining.walker.CallsiteHolderExplorable
///
// @class InlineableGraph
public final class InlineableGraph implements Inlineable
{
    // @field
    private final StructuredGraph ___graph;

    // @field
    private FixedNodeProbabilityCache ___probabilites = new FixedNodeProbabilityCache();

    // @cons InlineableGraph
    public InlineableGraph(final ResolvedJavaMethod __method, final Invoke __invoke, final HighTierContext __context, CanonicalizerPhase __canonicalizer)
    {
        super();
        StructuredGraph __original = getOriginalGraph(__method, __context, __canonicalizer, __invoke.asNode().graph(), __invoke.bci());
        // TODO copying the graph is only necessary if it is modified or if it contains any invokes
        this.___graph = (StructuredGraph) __original.copy();
        specializeGraphToArguments(__invoke, __context, __canonicalizer);
    }

    ///
    // This method looks up in a cache the graph for the argument, if not found bytecode is parsed.
    // The graph thus obtained is returned, ie the caller is responsible for cloning before modification.
    ///
    private static StructuredGraph getOriginalGraph(final ResolvedJavaMethod __method, final HighTierContext __context, CanonicalizerPhase __canonicalizer, StructuredGraph __caller, int __callerBci)
    {
        StructuredGraph __result = InliningUtil.getIntrinsicGraph(__context.getReplacements(), __method, __callerBci);
        if (__result != null)
        {
            return __result;
        }
        return parseBytecodes(__method, __context, __canonicalizer, __caller);
    }

    ///
    // @return true iff one or more parameters <code>newGraph</code> were specialized to account for
    //         a constant argument, or an argument with a more specific stamp.
    ///
    private boolean specializeGraphToArguments(final Invoke __invoke, final HighTierContext __context, CanonicalizerPhase __canonicalizer)
    {
        ArrayList<Node> __parameterUsages = replaceParamsWithMoreInformativeArguments(__invoke, __context);
        if (__parameterUsages != null)
        {
            __canonicalizer.applyIncremental(this.___graph, __context, __parameterUsages);
            return true;
        }
        else
        {
            // TODO if args are not more concrete, inlining should be avoided in most cases or we
            // could at least use the previous graph size + invoke probability to check the inlining
            return false;
        }
    }

    private static boolean isArgMoreInformativeThanParam(ValueNode __arg, ParameterNode __param)
    {
        return __arg.isConstant() || canStampBeImproved(__arg, __param);
    }

    private static boolean canStampBeImproved(ValueNode __arg, ParameterNode __param)
    {
        return improvedStamp(__arg, __param) != null;
    }

    private static Stamp improvedStamp(ValueNode __arg, ParameterNode __param)
    {
        Stamp __joinedStamp = __param.stamp(NodeView.DEFAULT).join(__arg.stamp(NodeView.DEFAULT));
        if (__joinedStamp == null || __joinedStamp.equals(__param.stamp(NodeView.DEFAULT)))
        {
            return null;
        }
        return __joinedStamp;
    }

    ///
    // This method detects:
    //
    // <li>constants among the arguments to the <code>invoke</code></li>
    // <li>arguments with more precise type than that declared by the corresponding parameter</li>
    //
    // The corresponding parameters are updated to reflect the above information. Before doing so,
    // their usages are added to <code>parameterUsages</code> for later incremental canonicalization.
    //
    // @return null if no incremental canonicalization is need, a list of nodes for such
    //         canonicalization otherwise.
    ///
    private ArrayList<Node> replaceParamsWithMoreInformativeArguments(final Invoke __invoke, final HighTierContext __context)
    {
        NodeInputList<ValueNode> __args = __invoke.callTarget().arguments();
        ArrayList<Node> __parameterUsages = null;
        List<ParameterNode> __params = this.___graph.getNodes(ParameterNode.TYPE).snapshot();
        // param-nodes that aren't used (eg, as a result of canonicalization) don't occur in
        // 'params'. Thus, in general, the sizes of 'params' and 'args' don't always match. Still,
        // it's always possible to pair a param-node with its corresponding arg-node using
        // param.index() as index into 'args'.
        for (ParameterNode __param : __params)
        {
            if (__param.usages().isNotEmpty())
            {
                ValueNode __arg = __args.get(__param.index());
                if (__arg.isConstant())
                {
                    ConstantNode __constant = (ConstantNode) __arg;
                    __parameterUsages = trackParameterUsages(__param, __parameterUsages);
                    // collect param usages before replacing the param
                    __param.replaceAtUsagesAndDelete(this.___graph.unique(ConstantNode.forConstant(__arg.stamp(NodeView.DEFAULT), __constant.getValue(), __constant.getStableDimension(), __constant.isDefaultStable(), __context.getMetaAccess())));
                    // param-node gone, leaving a gap in the sequence given by param.index()
                }
                else
                {
                    Stamp __impro = improvedStamp(__arg, __param);
                    if (__impro != null)
                    {
                        __param.setStamp(__impro);
                        __parameterUsages = trackParameterUsages(__param, __parameterUsages);
                    }
                }
            }
        }
        return __parameterUsages;
    }

    private static ArrayList<Node> trackParameterUsages(ParameterNode __param, ArrayList<Node> __parameterUsages)
    {
        ArrayList<Node> __result = (__parameterUsages == null) ? new ArrayList<>() : __parameterUsages;
        __param.usages().snapshotTo(__result);
        return __result;
    }

    ///
    // This method builds the IR nodes for the given <code>method</code> and canonicalizes them.
    // Provided profiling info is mature, the resulting graph is cached. The caller is responsible
    // for cloning before modification.
    ///
    private static StructuredGraph parseBytecodes(ResolvedJavaMethod __method, HighTierContext __context, CanonicalizerPhase __canonicalizer, StructuredGraph __caller)
    {
        StructuredGraph __newGraph = new StructuredGraph.GraphBuilder(StructuredGraph.AllowAssumptions.ifNonNull(__caller.getAssumptions())).method(__method).build();
        if (!__caller.isUnsafeAccessTrackingEnabled())
        {
            __newGraph.disableUnsafeAccessTracking();
        }
        if (__context.getGraphBuilderSuite() != null)
        {
            __context.getGraphBuilderSuite().apply(__newGraph, __context);
        }

        new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Optional).apply(__newGraph);

        __canonicalizer.apply(__newGraph, __context);

        return __newGraph;
    }

    @Override
    public int getNodeCount()
    {
        return InliningUtil.getNodeCount(this.___graph);
    }

    @Override
    public Iterable<Invoke> getInvokes()
    {
        return this.___graph.getInvokes();
    }

    @Override
    public double getProbability(Invoke __invoke)
    {
        return this.___probabilites.applyAsDouble(__invoke.asNode());
    }

    public StructuredGraph getGraph()
    {
        return this.___graph;
    }
}
