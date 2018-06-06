package giraaff.phases.common.inlining.walker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.ObjectStamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.AbstractNewObjectNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.AssumptionInlineInfo;
import giraaff.phases.common.inlining.info.ExactInlineInfo;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.info.MultiTypeGuardInlineInfo;
import giraaff.phases.common.inlining.info.TypeGuardInlineInfo;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.common.inlining.info.elem.InlineableGraph;
import giraaff.phases.common.inlining.policy.InliningPolicy;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.util.Providers;
import giraaff.util.GraalError;

///
// The space of inlining decisions is explored depth-first with the help of a stack realized by
// {@link InliningData}. At any point in time, the topmost element of that stack consists of:
//
// <li>the callsite under consideration is tracked as a {@link MethodInvocation}.</li>
// <li>one or more {@link CallsiteHolder}s, all of them associated to the callsite above. Why more
// than one? Depending on the type-profile for the receiver more than one concrete method may be
// feasible target.</li>
//
// The bottom element in the stack consists of:
//
// <li>a single {@link MethodInvocation} (the
// {@link giraaff.phases.common.inlining.walker.MethodInvocation#isRoot root} one, ie
// the unknown caller of the root graph)</li>
// <li>a single {@link CallsiteHolder} (the root one, for the method on which inlining was called)</li>
//
// @see #moveForward()
///
// @class InliningData
public final class InliningData
{
    ///
    // Call hierarchy from outer most call (i.e., compilation unit) to inner most callee.
    ///
    // @field
    private final ArrayDeque<CallsiteHolder> ___graphQueue = new ArrayDeque<>();
    // @field
    private final ArrayDeque<MethodInvocation> ___invocationQueue = new ArrayDeque<>();

    // @field
    private final HighTierContext ___context;
    // @field
    private final int ___maxMethodPerInlining;
    // @field
    private final CanonicalizerPhase ___canonicalizer;
    // @field
    private final InliningPolicy ___inliningPolicy;
    // @field
    private final StructuredGraph ___rootGraph;

    // @field
    private int ___maxGraphs;

    // @cons InliningData
    public InliningData(StructuredGraph __rootGraph, HighTierContext __context, int __maxMethodPerInlining, CanonicalizerPhase __canonicalizer, InliningPolicy __inliningPolicy, LinkedList<Invoke> __rootInvokes)
    {
        super();
        this.___context = __context;
        this.___maxMethodPerInlining = __maxMethodPerInlining;
        this.___canonicalizer = __canonicalizer;
        this.___inliningPolicy = __inliningPolicy;
        this.___maxGraphs = 1;
        this.___rootGraph = __rootGraph;

        this.___invocationQueue.push(new MethodInvocation(null, 1.0, 1.0, null));
        this.___graphQueue.push(new CallsiteHolderExplorable(__rootGraph, 1.0, 1.0, null, __rootInvokes));
    }

    public static boolean isFreshInstantiation(ValueNode __arg)
    {
        return (__arg instanceof AbstractNewObjectNode) || (__arg instanceof AllocatedObjectNode) || (__arg instanceof VirtualObjectNode);
    }

    private String checkTargetConditionsHelper(ResolvedJavaMethod __method, int __invokeBci)
    {
        if (__method == null)
        {
            return "the method is not resolved";
        }
        if (__method.isNative() && (!GraalOptions.intrinsify || !InliningUtil.canIntrinsify(this.___context.getReplacements(), __method, __invokeBci)))
        {
            return "it is a non-intrinsic native method";
        }
        if (__method.isAbstract())
        {
            return "it is an abstract method";
        }
        if (!__method.getDeclaringClass().isInitialized())
        {
            return "the method's class is not initialized";
        }
        if (!__method.canBeInlined())
        {
            return "it is marked non-inlinable";
        }
        if (countRecursiveInlining(__method) > GraalOptions.maximumRecursiveInlining)
        {
            return "it exceeds the maximum recursive inlining depth";
        }
        if (new OptimisticOptimizations(this.___rootGraph.getProfilingInfo(__method)).lessOptimisticThan(this.___context.getOptimisticOptimizations()))
        {
            return "the callee uses less optimistic optimizations than caller";
        }
        return null;
    }

    private boolean checkTargetConditions(Invoke __invoke, ResolvedJavaMethod __method)
    {
        final String __failureMessage = checkTargetConditionsHelper(__method, __invoke.bci());
        if (__failureMessage == null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    ///
    // Determines if inlining is possible at the given invoke node.
    //
    // @param invoke the invoke that should be inlined
    // @return an instance of InlineInfo, or null if no inlining is possible at the given invoke
    ///
    private InlineInfo getInlineInfo(Invoke __invoke)
    {
        final String __failureMessage = InliningUtil.checkInvokeConditions(__invoke);
        if (__failureMessage != null)
        {
            return null;
        }
        MethodCallTargetNode __callTarget = (MethodCallTargetNode) __invoke.callTarget();
        ResolvedJavaMethod __targetMethod = __callTarget.targetMethod();

        if (__callTarget.invokeKind() == CallTargetNode.InvokeKind.Special || __targetMethod.canBeStaticallyBound())
        {
            return getExactInlineInfo(__invoke, __targetMethod);
        }

        ResolvedJavaType __holder = __targetMethod.getDeclaringClass();
        if (!(__callTarget.receiver().stamp(NodeView.DEFAULT) instanceof ObjectStamp))
        {
            return null;
        }
        ObjectStamp __receiverStamp = (ObjectStamp) __callTarget.receiver().stamp(NodeView.DEFAULT);
        if (__receiverStamp.alwaysNull())
        {
            // don't inline if receiver is known to be null
            return null;
        }
        ResolvedJavaType __contextType = __invoke.getContextType();
        if (__receiverStamp.type() != null)
        {
            // the invoke target might be more specific than the holder (happens after inlining:
            // parameters lose their declared type...)
            ResolvedJavaType __receiverType = __receiverStamp.type();
            if (__receiverType != null && __holder.isAssignableFrom(__receiverType))
            {
                __holder = __receiverType;
                if (__receiverStamp.isExactType())
                {
                    ResolvedJavaMethod __resolvedMethod = __holder.resolveConcreteMethod(__targetMethod, __contextType);
                    if (__resolvedMethod != null)
                    {
                        return getExactInlineInfo(__invoke, __resolvedMethod);
                    }
                }
            }
        }

        if (__holder.isArray())
        {
            // arrays can be treated as Objects
            ResolvedJavaMethod __resolvedMethod = __holder.resolveConcreteMethod(__targetMethod, __contextType);
            if (__resolvedMethod != null)
            {
                return getExactInlineInfo(__invoke, __resolvedMethod);
            }
        }

        AssumptionResult<ResolvedJavaType> __leafConcreteSubtype = __holder.findLeafConcreteSubtype();
        if (__leafConcreteSubtype != null)
        {
            ResolvedJavaMethod __resolvedMethod = __leafConcreteSubtype.getResult().resolveConcreteMethod(__targetMethod, __contextType);
            if (__resolvedMethod != null)
            {
                if (__leafConcreteSubtype.canRecordTo(__callTarget.graph().getAssumptions()))
                {
                    return getAssumptionInlineInfo(__invoke, __resolvedMethod, __leafConcreteSubtype);
                }
                else
                {
                    return getTypeCheckedAssumptionInfo(__invoke, __resolvedMethod, __leafConcreteSubtype.getResult());
                }
            }
        }

        AssumptionResult<ResolvedJavaMethod> __concrete = __holder.findUniqueConcreteMethod(__targetMethod);
        if (__concrete != null && __concrete.canRecordTo(__callTarget.graph().getAssumptions()))
        {
            return getAssumptionInlineInfo(__invoke, __concrete.getResult(), __concrete);
        }

        // type check based inlining
        return getTypeCheckedInlineInfo(__invoke, __targetMethod);
    }

    private InlineInfo getTypeCheckedAssumptionInfo(Invoke __invoke, ResolvedJavaMethod __method, ResolvedJavaType __type)
    {
        if (!checkTargetConditions(__invoke, __method))
        {
            return null;
        }
        return new TypeGuardInlineInfo(__invoke, __method, __type);
    }

    private InlineInfo getTypeCheckedInlineInfo(Invoke __invoke, ResolvedJavaMethod __targetMethod)
    {
        JavaTypeProfile __typeProfile = ((MethodCallTargetNode) __invoke.callTarget()).getProfile();
        if (__typeProfile == null)
        {
            return null;
        }

        JavaTypeProfile.ProfiledType[] __ptypes = __typeProfile.getTypes();
        if (__ptypes == null || __ptypes.length <= 0)
        {
            return null;
        }
        ResolvedJavaType __contextType = __invoke.getContextType();
        double __notRecordedTypeProbability = __typeProfile.getNotRecordedProbability();
        final OptimisticOptimizations __optimisticOpts = this.___context.getOptimisticOptimizations();
        if (__ptypes.length == 1 && __notRecordedTypeProbability == 0)
        {
            if (!__optimisticOpts.inlineMonomorphicCalls())
            {
                return null;
            }

            ResolvedJavaType __type = __ptypes[0].getType();
            ResolvedJavaMethod __concrete = __type.resolveConcreteMethod(__targetMethod, __contextType);
            if (!checkTargetConditions(__invoke, __concrete))
            {
                return null;
            }
            return new TypeGuardInlineInfo(__invoke, __concrete, __type);
        }
        else
        {
            __invoke.setPolymorphic(true);

            if (!__optimisticOpts.inlinePolymorphicCalls() && __notRecordedTypeProbability == 0)
            {
                return null;
            }
            if (!__optimisticOpts.inlineMegamorphicCalls() && __notRecordedTypeProbability > 0)
            {
                // due to filtering impossible types, notRecordedTypeProbability can be > 0 although
                // the number of types is lower than what can be recorded in a type profile
                return null;
            }

            // Find unique methods and their probabilities.
            ArrayList<ResolvedJavaMethod> __concreteMethods = new ArrayList<>();
            ArrayList<Double> __concreteMethodsProbabilities = new ArrayList<>();
            for (int __i = 0; __i < __ptypes.length; __i++)
            {
                ResolvedJavaMethod __concrete = __ptypes[__i].getType().resolveConcreteMethod(__targetMethod, __contextType);
                if (__concrete == null)
                {
                    return null;
                }
                int __index = __concreteMethods.indexOf(__concrete);
                double __curProbability = __ptypes[__i].getProbability();
                if (__index < 0)
                {
                    __index = __concreteMethods.size();
                    __concreteMethods.add(__concrete);
                    __concreteMethodsProbabilities.add(__curProbability);
                }
                else
                {
                    __concreteMethodsProbabilities.set(__index, __concreteMethodsProbabilities.get(__index) + __curProbability);
                }
            }

            // Clear methods that fall below the threshold.
            if (__notRecordedTypeProbability > 0)
            {
                ArrayList<ResolvedJavaMethod> __newConcreteMethods = new ArrayList<>();
                ArrayList<Double> __newConcreteMethodsProbabilities = new ArrayList<>();
                for (int __i = 0; __i < __concreteMethods.size(); ++__i)
                {
                    if (__concreteMethodsProbabilities.get(__i) >= GraalOptions.megamorphicInliningMinMethodProbability)
                    {
                        __newConcreteMethods.add(__concreteMethods.get(__i));
                        __newConcreteMethodsProbabilities.add(__concreteMethodsProbabilities.get(__i));
                    }
                }

                if (__newConcreteMethods.isEmpty())
                {
                    // No method left that is worth inlining.
                    return null;
                }

                __concreteMethods = __newConcreteMethods;
                __concreteMethodsProbabilities = __newConcreteMethodsProbabilities;
            }

            if (__concreteMethods.size() > this.___maxMethodPerInlining)
            {
                return null;
            }

            // Clean out types whose methods are no longer available.
            ArrayList<JavaTypeProfile.ProfiledType> __usedTypes = new ArrayList<>();
            ArrayList<Integer> __typesToConcretes = new ArrayList<>();
            for (JavaTypeProfile.ProfiledType __type : __ptypes)
            {
                ResolvedJavaMethod __concrete = __type.getType().resolveConcreteMethod(__targetMethod, __contextType);
                int __index = __concreteMethods.indexOf(__concrete);
                if (__index == -1)
                {
                    __notRecordedTypeProbability += __type.getProbability();
                }
                else
                {
                    __usedTypes.add(__type);
                    __typesToConcretes.add(__index);
                }
            }

            if (__usedTypes.isEmpty())
            {
                // No type left that is worth checking for.
                return null;
            }

            for (ResolvedJavaMethod __concrete : __concreteMethods)
            {
                if (!checkTargetConditions(__invoke, __concrete))
                {
                    return null;
                }
            }
            return new MultiTypeGuardInlineInfo(__invoke, __concreteMethods, __usedTypes, __typesToConcretes, __notRecordedTypeProbability);
        }
    }

    private InlineInfo getAssumptionInlineInfo(Invoke __invoke, ResolvedJavaMethod __concrete, AssumptionResult<?> __takenAssumption)
    {
        if (checkTargetConditions(__invoke, __concrete))
        {
            return new AssumptionInlineInfo(__invoke, __concrete, __takenAssumption);
        }
        return null;
    }

    private InlineInfo getExactInlineInfo(Invoke __invoke, ResolvedJavaMethod __targetMethod)
    {
        if (checkTargetConditions(__invoke, __targetMethod))
        {
            return new ExactInlineInfo(__invoke, __targetMethod);
        }
        return null;
    }

    private void doInline(CallsiteHolderExplorable __callerCallsiteHolder, MethodInvocation __calleeInvocation, String __reason)
    {
        StructuredGraph __callerGraph = __callerCallsiteHolder.graph();
        InlineInfo __calleeInfo = __calleeInvocation.callee();
        try
        {
            EconomicSet<Node> __canonicalizedNodes = EconomicSet.create(Equivalence.IDENTITY);
            __canonicalizedNodes.addAll(__calleeInfo.invoke().asNode().usages());
            EconomicSet<Node> __parameterUsages = __calleeInfo.inline(new Providers(this.___context), __reason);
            __canonicalizedNodes.addAll(__parameterUsages);

            Graph.NodeMark __markBeforeCanonicalization = __callerGraph.getMark();

            this.___canonicalizer.applyIncremental(__callerGraph, this.___context, __canonicalizedNodes);

            // process invokes that are possibly created during canonicalization
            for (Node __newNode : __callerGraph.getNewNodes(__markBeforeCanonicalization))
            {
                if (__newNode instanceof Invoke)
                {
                    __callerCallsiteHolder.pushInvoke((Invoke) __newNode);
                }
            }

            __callerCallsiteHolder.computeProbabilities();
        }
        catch (BailoutException __bailout)
        {
            throw __bailout;
        }
        catch (AssertionError | RuntimeException __e)
        {
            throw new GraalError(__e);
        }
    }

    ///
    // This method attempts:
    //
    // <li>to inline at the callsite given by <code>calleeInvocation</code>, where that callsite
    // belongs to the {@link CallsiteHolderExplorable} at the top of the {@link #graphQueue}
    // maintained in this class.</li>
    // <li>otherwise, to devirtualize the callsite in question.</li>
    //
    // @return true iff inlining was actually performed
    ///
    private boolean tryToInline(MethodInvocation __calleeInvocation, int __inliningDepth)
    {
        CallsiteHolderExplorable __callerCallsiteHolder = (CallsiteHolderExplorable) currentGraph();
        InlineInfo __calleeInfo = __calleeInvocation.callee();

        InliningPolicy.Decision __decision = this.___inliningPolicy.isWorthInlining(this.___context.getReplacements(), __calleeInvocation, __inliningDepth, true);
        if (__decision.___shouldInline)
        {
            doInline(__callerCallsiteHolder, __calleeInvocation, __decision.___reason);
            return true;
        }

        if (this.___context.getOptimisticOptimizations().devirtualizeInvokes())
        {
            __calleeInfo.tryToDevirtualizeInvoke(new Providers(this.___context));
        }

        return false;
    }

    ///
    // This method picks one of the callsites belonging to the current
    // {@link CallsiteHolderExplorable}. Provided the callsite qualifies to be analyzed for
    // inlining, this method prepares a new stack top in {@link InliningData} for such callsite,
    // which comprises:
    //
    // <li>preparing a summary of feasible targets, ie preparing an {@link InlineInfo}</li>
    // <li>based on it, preparing the stack top proper which consists of:</li>
    //
    // <li>one {@link MethodInvocation}</li>
    // <li>a {@link CallsiteHolder} for each feasible target</li>
    //
    // The thus prepared "stack top" is needed by {@link #moveForward()} to explore the space of
    // inlining decisions (each decision one of: backtracking, delving, inlining).
    //
    // The {@link InlineInfo} used to get things rolling is kept around in the
    // {@link MethodInvocation}, it will be needed in case of inlining, see
    // {@link InlineInfo#inline(Providers, String)}
    ///
    private void processNextInvoke()
    {
        CallsiteHolderExplorable __callsiteHolder = (CallsiteHolderExplorable) currentGraph();
        Invoke __invoke = __callsiteHolder.popInvoke();
        InlineInfo __info = getInlineInfo(__invoke);

        if (__info != null)
        {
            __info.populateInlinableElements(this.___context, currentGraph().graph(), this.___canonicalizer);
            double __invokeProbability = __callsiteHolder.invokeProbability(__invoke);
            double __invokeRelevance = __callsiteHolder.invokeRelevance(__invoke);
            MethodInvocation __methodInvocation = new MethodInvocation(__info, __invokeProbability, __invokeRelevance, freshlyInstantiatedArguments(__invoke, __callsiteHolder.getFixedParams()));
            pushInvocationAndGraphs(__methodInvocation);
        }
    }

    ///
    // Gets the freshly instantiated arguments.
    //
    // A freshly instantiated argument is either:
    //
    // <li>an {@link InliningData#isFreshInstantiation(giraaff.nodes.ValueNode)}</li>
    // <li>a fixed-param, ie a {@link ParameterNode} receiving a freshly instantiated argument</li>
    //
    // @return the positions of freshly instantiated arguments in the argument list of the
    //         <code>invoke</code>, or null if no such positions exist.
    ///
    public static BitSet freshlyInstantiatedArguments(Invoke __invoke, EconomicSet<ParameterNode> __fixedParams)
    {
        BitSet __result = null;
        int __argIdx = 0;
        for (ValueNode __arg : __invoke.callTarget().arguments())
        {
            if (isFreshInstantiation(__arg) || (__arg instanceof ParameterNode && __fixedParams.contains((ParameterNode) __arg)))
            {
                if (__result == null)
                {
                    __result = new BitSet();
                }
                __result.set(__argIdx);
            }
            __argIdx++;
        }
        return __result;
    }

    private static boolean paramsAndInvokeAreInSameGraph(Invoke __invoke, EconomicSet<ParameterNode> __fixedParams)
    {
        if (__fixedParams.isEmpty())
        {
            return true;
        }
        for (ParameterNode __p : __fixedParams)
        {
            if (__p.graph() != __invoke.asNode().graph())
            {
                return false;
            }
        }
        return true;
    }

    public int graphCount()
    {
        return this.___graphQueue.size();
    }

    public boolean hasUnprocessedGraphs()
    {
        return !this.___graphQueue.isEmpty();
    }

    private CallsiteHolder currentGraph()
    {
        return this.___graphQueue.peek();
    }

    private void popGraph()
    {
        this.___graphQueue.pop();
    }

    private void popGraphs(int __count)
    {
        for (int __i = 0; __i < __count; __i++)
        {
            this.___graphQueue.pop();
        }
    }

    // @def
    private static final Object[] NO_CONTEXT = {};

    private MethodInvocation currentInvocation()
    {
        return this.___invocationQueue.peekFirst();
    }

    private void pushInvocationAndGraphs(MethodInvocation __methodInvocation)
    {
        this.___invocationQueue.addFirst(__methodInvocation);
        InlineInfo __info = __methodInvocation.callee();
        this.___maxGraphs += __info.numberOfMethods();
        for (int __i = 0; __i < __info.numberOfMethods(); __i++)
        {
            CallsiteHolder __ch = __methodInvocation.buildCallsiteHolderForElement(__i);
            this.___graphQueue.push(__ch);
        }
    }

    private void popInvocation()
    {
        this.___maxGraphs -= this.___invocationQueue.peekFirst().callee().numberOfMethods();
        this.___invocationQueue.removeFirst();
    }

    public int countRecursiveInlining(ResolvedJavaMethod __method)
    {
        int __count = 0;
        for (CallsiteHolder __callsiteHolder : this.___graphQueue)
        {
            if (__method.equals(__callsiteHolder.method()))
            {
                __count++;
            }
        }
        return __count;
    }

    public int inliningDepth()
    {
        return this.___invocationQueue.size() - 1;
    }

    private boolean contains(StructuredGraph __graph)
    {
        for (CallsiteHolder __info : this.___graphQueue)
        {
            if (__info.graph() == __graph)
            {
                return true;
            }
        }
        return false;
    }

    ///
    // The stack realized by {@link InliningData} grows and shrinks as choices are made among the
    // alternatives below:
    // <ol>
    // <li>not worth inlining: pop stack top, which comprises:
    // <ul>
    // <li>pop any remaining graphs not yet delved into</li>
    // <li>pop the current invocation</li>
    // </ul>
    // </li>
    // <li>{@link #processNextInvoke() delve} into one of the callsites hosted in the current graph,
    // such callsite is explored next by {@link #moveForward()}</li>
    // <li>{@link #tryToInline(MethodInvocation, int) try to inline}: move past the current graph
    // (remove it from the topmost element).
    // <ul>
    // <li>If that was the last one then {@link #tryToInline(MethodInvocation, int) try to inline}
    // the callsite under consideration (ie, the "current invocation").</li>
    // <li>Whether inlining occurs or not, that callsite is removed from the top of
    // {@link InliningData} .</li>
    // </ul>
    // </li>
    // </ol>
    //
    // Some facts about the alternatives above:
    //
    // <li>the first step amounts to backtracking, the 2nd one to depth-search, and the 3rd one also
    // involves backtracking (however possibly after inlining).</li>
    // <li>the choice of abandon-and-backtrack or delve-into depends on
    // {@link InliningPolicy#isWorthInlining} and {@link InliningPolicy#continueInlining}.</li>
    // <li>the 3rd choice is picked whenever none of the previous choices are made</li>
    //
    // @return true iff inlining was actually performed
    ///
    public boolean moveForward()
    {
        final MethodInvocation __currentInvocation = currentInvocation();

        final boolean __backtrack = (!__currentInvocation.isRoot() && !this.___inliningPolicy.isWorthInlining(this.___context.getReplacements(), __currentInvocation, inliningDepth(), false).___shouldInline);
        if (__backtrack)
        {
            int __remainingGraphs = __currentInvocation.totalGraphs() - __currentInvocation.processedGraphs();
            popGraphs(__remainingGraphs);
            popInvocation();
            return false;
        }

        final boolean __delve = currentGraph().hasRemainingInvokes() && this.___inliningPolicy.continueInlining(currentGraph().graph());
        if (__delve)
        {
            processNextInvoke();
            return false;
        }

        popGraph();
        if (__currentInvocation.isRoot())
        {
            return false;
        }

        // try to inline
        __currentInvocation.incrementProcessedGraphs();
        if (__currentInvocation.processedGraphs() == __currentInvocation.totalGraphs())
        {
            // "all of currentInvocation's graphs processed" amounts to
            // "all concrete methods that come into question already had the callees they contain analyzed for inlining"
            popInvocation();
            if (tryToInline(__currentInvocation, inliningDepth() + 1))
            {
                // report real progress only if we inline into the root graph
                return currentGraph().graph() == this.___rootGraph;
            }
            return false;
        }

        return false;
    }

    ///
    // Checks an invariant that {@link #moveForward()} must maintain: "the top invocation records
    // how many concrete target methods (for it) remain on the {@link #graphQueue}; those targets
    // 'belong' to the current invocation in question.
    ///
    private boolean topGraphsForTopInvocation()
    {
        if (this.___invocationQueue.isEmpty())
        {
            return true;
        }
        if (currentInvocation().isRoot())
        {
            return true;
        }
        final int __remainingGraphs = currentInvocation().totalGraphs() - currentInvocation().processedGraphs();
        final Iterator<CallsiteHolder> __iter = this.___graphQueue.iterator();
        for (int __i = (__remainingGraphs - 1); __i >= 0; __i--)
        {
            if (!__iter.hasNext())
            {
                return false;
            }
            CallsiteHolder __queuedTargetCH = __iter.next();
            Inlineable __targetIE = currentInvocation().callee().inlineableElementAt(__i);
            InlineableGraph __targetIG = (InlineableGraph) __targetIE;
        }
        return true;
    }

    ///
    // This method checks invariants for this class. Named after shorthand for "internal
    // representation is ok".
    ///
    public boolean repOK()
    {
        return true;
    }
}
