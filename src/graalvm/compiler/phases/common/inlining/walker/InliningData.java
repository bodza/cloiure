package graalvm.compiler.phases.common.inlining.walker;

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

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.AbstractNewObjectNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.virtual.AllocatedObjectNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.OptimisticOptimizations;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.info.AssumptionInlineInfo;
import graalvm.compiler.phases.common.inlining.info.ExactInlineInfo;
import graalvm.compiler.phases.common.inlining.info.InlineInfo;
import graalvm.compiler.phases.common.inlining.info.MultiTypeGuardInlineInfo;
import graalvm.compiler.phases.common.inlining.info.TypeGuardInlineInfo;
import graalvm.compiler.phases.common.inlining.info.elem.Inlineable;
import graalvm.compiler.phases.common.inlining.info.elem.InlineableGraph;
import graalvm.compiler.phases.common.inlining.policy.InliningPolicy;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.util.Providers;

/**
 * The space of inlining decisions is explored depth-first with the help of a stack realized by
 * {@link InliningData}. At any point in time, the topmost element of that stack consists of:
 *
 * <li>the callsite under consideration is tracked as a {@link MethodInvocation}.</li>
 * <li>one or more {@link CallsiteHolder}s, all of them associated to the callsite above. Why more
 * than one? Depending on the type-profile for the receiver more than one concrete method may be
 * feasible target.</li>
 *
 * The bottom element in the stack consists of:
 *
 * <li>a single {@link MethodInvocation} (the
 * {@link graalvm.compiler.phases.common.inlining.walker.MethodInvocation#isRoot root} one, ie
 * the unknown caller of the root graph)</li>
 * <li>a single {@link CallsiteHolder} (the root one, for the method on which inlining was called)</li>
 *
 * @see #moveForward()
 */
public class InliningData
{
    /**
     * Call hierarchy from outer most call (i.e., compilation unit) to inner most callee.
     */
    private final ArrayDeque<CallsiteHolder> graphQueue = new ArrayDeque<>();
    private final ArrayDeque<MethodInvocation> invocationQueue = new ArrayDeque<>();

    private final HighTierContext context;
    private final int maxMethodPerInlining;
    private final CanonicalizerPhase canonicalizer;
    private final InliningPolicy inliningPolicy;
    private final StructuredGraph rootGraph;

    private int maxGraphs;

    public InliningData(StructuredGraph rootGraph, HighTierContext context, int maxMethodPerInlining, CanonicalizerPhase canonicalizer, InliningPolicy inliningPolicy, LinkedList<Invoke> rootInvokes)
    {
        this.context = context;
        this.maxMethodPerInlining = maxMethodPerInlining;
        this.canonicalizer = canonicalizer;
        this.inliningPolicy = inliningPolicy;
        this.maxGraphs = 1;
        this.rootGraph = rootGraph;

        invocationQueue.push(new MethodInvocation(null, 1.0, 1.0, null));
        graphQueue.push(new CallsiteHolderExplorable(rootGraph, 1.0, 1.0, null, rootInvokes));
    }

    public static boolean isFreshInstantiation(ValueNode arg)
    {
        return (arg instanceof AbstractNewObjectNode) || (arg instanceof AllocatedObjectNode) || (arg instanceof VirtualObjectNode);
    }

    private String checkTargetConditionsHelper(ResolvedJavaMethod method, int invokeBci)
    {
        OptionValues options = rootGraph.getOptions();
        if (method == null)
        {
            return "the method is not resolved";
        }
        else if (method.isNative() && (!GraalOptions.Intrinsify.getValue(options) || !InliningUtil.canIntrinsify(context.getReplacements(), method, invokeBci)))
        {
            return "it is a non-intrinsic native method";
        }
        else if (method.isAbstract())
        {
            return "it is an abstract method";
        }
        else if (!method.getDeclaringClass().isInitialized())
        {
            return "the method's class is not initialized";
        }
        else if (!method.canBeInlined())
        {
            return "it is marked non-inlinable";
        }
        else if (countRecursiveInlining(method) > GraalOptions.MaximumRecursiveInlining.getValue(options))
        {
            return "it exceeds the maximum recursive inlining depth";
        }
        else
        {
            if (new OptimisticOptimizations(rootGraph.getProfilingInfo(method), options).lessOptimisticThan(context.getOptimisticOptimizations()))
            {
                return "the callee uses less optimistic optimizations than caller";
            }
            else
            {
                return null;
            }
        }
    }

    private boolean checkTargetConditions(Invoke invoke, ResolvedJavaMethod method)
    {
        final String failureMessage = checkTargetConditionsHelper(method, invoke.bci());
        if (failureMessage == null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Determines if inlining is possible at the given invoke node.
     *
     * @param invoke the invoke that should be inlined
     * @return an instance of InlineInfo, or null if no inlining is possible at the given invoke
     */
    private InlineInfo getInlineInfo(Invoke invoke)
    {
        final String failureMessage = InliningUtil.checkInvokeConditions(invoke);
        if (failureMessage != null)
        {
            return null;
        }
        MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
        ResolvedJavaMethod targetMethod = callTarget.targetMethod();

        if (callTarget.invokeKind() == CallTargetNode.InvokeKind.Special || targetMethod.canBeStaticallyBound())
        {
            return getExactInlineInfo(invoke, targetMethod);
        }

        ResolvedJavaType holder = targetMethod.getDeclaringClass();
        if (!(callTarget.receiver().stamp(NodeView.DEFAULT) instanceof ObjectStamp))
        {
            return null;
        }
        ObjectStamp receiverStamp = (ObjectStamp) callTarget.receiver().stamp(NodeView.DEFAULT);
        if (receiverStamp.alwaysNull())
        {
            // Don't inline if receiver is known to be null
            return null;
        }
        ResolvedJavaType contextType = invoke.getContextType();
        if (receiverStamp.type() != null)
        {
            // the invoke target might be more specific than the holder (happens after inlining:
            // parameters lose their declared type...)
            ResolvedJavaType receiverType = receiverStamp.type();
            if (receiverType != null && holder.isAssignableFrom(receiverType))
            {
                holder = receiverType;
                if (receiverStamp.isExactType())
                {
                    ResolvedJavaMethod resolvedMethod = holder.resolveConcreteMethod(targetMethod, contextType);
                    if (resolvedMethod != null)
                    {
                        return getExactInlineInfo(invoke, resolvedMethod);
                    }
                }
            }
        }

        if (holder.isArray())
        {
            // arrays can be treated as Objects
            ResolvedJavaMethod resolvedMethod = holder.resolveConcreteMethod(targetMethod, contextType);
            if (resolvedMethod != null)
            {
                return getExactInlineInfo(invoke, resolvedMethod);
            }
        }

        AssumptionResult<ResolvedJavaType> leafConcreteSubtype = holder.findLeafConcreteSubtype();
        if (leafConcreteSubtype != null)
        {
            ResolvedJavaMethod resolvedMethod = leafConcreteSubtype.getResult().resolveConcreteMethod(targetMethod, contextType);
            if (resolvedMethod != null)
            {
                if (leafConcreteSubtype.canRecordTo(callTarget.graph().getAssumptions()))
                {
                    return getAssumptionInlineInfo(invoke, resolvedMethod, leafConcreteSubtype);
                }
                else
                {
                    return getTypeCheckedAssumptionInfo(invoke, resolvedMethod, leafConcreteSubtype.getResult());
                }
            }
        }

        AssumptionResult<ResolvedJavaMethod> concrete = holder.findUniqueConcreteMethod(targetMethod);
        if (concrete != null && concrete.canRecordTo(callTarget.graph().getAssumptions()))
        {
            return getAssumptionInlineInfo(invoke, concrete.getResult(), concrete);
        }

        // type check based inlining
        return getTypeCheckedInlineInfo(invoke, targetMethod);
    }

    private InlineInfo getTypeCheckedAssumptionInfo(Invoke invoke, ResolvedJavaMethod method, ResolvedJavaType type)
    {
        if (!checkTargetConditions(invoke, method))
        {
            return null;
        }
        return new TypeGuardInlineInfo(invoke, method, type);
    }

    private InlineInfo getTypeCheckedInlineInfo(Invoke invoke, ResolvedJavaMethod targetMethod)
    {
        JavaTypeProfile typeProfile = ((MethodCallTargetNode) invoke.callTarget()).getProfile();
        if (typeProfile == null)
        {
            return null;
        }

        JavaTypeProfile.ProfiledType[] ptypes = typeProfile.getTypes();
        if (ptypes == null || ptypes.length <= 0)
        {
            return null;
        }
        ResolvedJavaType contextType = invoke.getContextType();
        double notRecordedTypeProbability = typeProfile.getNotRecordedProbability();
        final OptimisticOptimizations optimisticOpts = context.getOptimisticOptimizations();
        OptionValues options = invoke.asNode().getOptions();
        if (ptypes.length == 1 && notRecordedTypeProbability == 0)
        {
            if (!optimisticOpts.inlineMonomorphicCalls(options))
            {
                return null;
            }

            ResolvedJavaType type = ptypes[0].getType();
            ResolvedJavaMethod concrete = type.resolveConcreteMethod(targetMethod, contextType);
            if (!checkTargetConditions(invoke, concrete))
            {
                return null;
            }
            return new TypeGuardInlineInfo(invoke, concrete, type);
        }
        else
        {
            invoke.setPolymorphic(true);

            if (!optimisticOpts.inlinePolymorphicCalls(options) && notRecordedTypeProbability == 0)
            {
                return null;
            }
            if (!optimisticOpts.inlineMegamorphicCalls(options) && notRecordedTypeProbability > 0)
            {
                // due to filtering impossible types, notRecordedTypeProbability can be > 0 although
                // the number of types is lower than what can be recorded in a type profile
                return null;
            }

            // Find unique methods and their probabilities.
            ArrayList<ResolvedJavaMethod> concreteMethods = new ArrayList<>();
            ArrayList<Double> concreteMethodsProbabilities = new ArrayList<>();
            for (int i = 0; i < ptypes.length; i++)
            {
                ResolvedJavaMethod concrete = ptypes[i].getType().resolveConcreteMethod(targetMethod, contextType);
                if (concrete == null)
                {
                    return null;
                }
                int index = concreteMethods.indexOf(concrete);
                double curProbability = ptypes[i].getProbability();
                if (index < 0)
                {
                    index = concreteMethods.size();
                    concreteMethods.add(concrete);
                    concreteMethodsProbabilities.add(curProbability);
                }
                else
                {
                    concreteMethodsProbabilities.set(index, concreteMethodsProbabilities.get(index) + curProbability);
                }
            }

            // Clear methods that fall below the threshold.
            if (notRecordedTypeProbability > 0)
            {
                ArrayList<ResolvedJavaMethod> newConcreteMethods = new ArrayList<>();
                ArrayList<Double> newConcreteMethodsProbabilities = new ArrayList<>();
                for (int i = 0; i < concreteMethods.size(); ++i)
                {
                    if (concreteMethodsProbabilities.get(i) >= GraalOptions.MegamorphicInliningMinMethodProbability.getValue(options))
                    {
                        newConcreteMethods.add(concreteMethods.get(i));
                        newConcreteMethodsProbabilities.add(concreteMethodsProbabilities.get(i));
                    }
                }

                if (newConcreteMethods.isEmpty())
                {
                    // No method left that is worth inlining.
                    return null;
                }

                concreteMethods = newConcreteMethods;
                concreteMethodsProbabilities = newConcreteMethodsProbabilities;
            }

            if (concreteMethods.size() > maxMethodPerInlining)
            {
                return null;
            }

            // Clean out types whose methods are no longer available.
            ArrayList<JavaTypeProfile.ProfiledType> usedTypes = new ArrayList<>();
            ArrayList<Integer> typesToConcretes = new ArrayList<>();
            for (JavaTypeProfile.ProfiledType type : ptypes)
            {
                ResolvedJavaMethod concrete = type.getType().resolveConcreteMethod(targetMethod, contextType);
                int index = concreteMethods.indexOf(concrete);
                if (index == -1)
                {
                    notRecordedTypeProbability += type.getProbability();
                }
                else
                {
                    usedTypes.add(type);
                    typesToConcretes.add(index);
                }
            }

            if (usedTypes.isEmpty())
            {
                // No type left that is worth checking for.
                return null;
            }

            for (ResolvedJavaMethod concrete : concreteMethods)
            {
                if (!checkTargetConditions(invoke, concrete))
                {
                    return null;
                }
            }
            return new MultiTypeGuardInlineInfo(invoke, concreteMethods, usedTypes, typesToConcretes, notRecordedTypeProbability);
        }
    }

    private InlineInfo getAssumptionInlineInfo(Invoke invoke, ResolvedJavaMethod concrete, AssumptionResult<?> takenAssumption)
    {
        if (checkTargetConditions(invoke, concrete))
        {
            return new AssumptionInlineInfo(invoke, concrete, takenAssumption);
        }
        return null;
    }

    private InlineInfo getExactInlineInfo(Invoke invoke, ResolvedJavaMethod targetMethod)
    {
        if (checkTargetConditions(invoke, targetMethod))
        {
            return new ExactInlineInfo(invoke, targetMethod);
        }
        return null;
    }

    private void doInline(CallsiteHolderExplorable callerCallsiteHolder, MethodInvocation calleeInvocation, String reason)
    {
        StructuredGraph callerGraph = callerCallsiteHolder.graph();
        InlineInfo calleeInfo = calleeInvocation.callee();
        try
        {
            EconomicSet<Node> canonicalizedNodes = EconomicSet.create(Equivalence.IDENTITY);
            canonicalizedNodes.addAll(calleeInfo.invoke().asNode().usages());
            EconomicSet<Node> parameterUsages = calleeInfo.inline(new Providers(context), reason);
            canonicalizedNodes.addAll(parameterUsages);

            Graph.Mark markBeforeCanonicalization = callerGraph.getMark();

            canonicalizer.applyIncremental(callerGraph, context, canonicalizedNodes);

            // process invokes that are possibly created during canonicalization
            for (Node newNode : callerGraph.getNewNodes(markBeforeCanonicalization))
            {
                if (newNode instanceof Invoke)
                {
                    callerCallsiteHolder.pushInvoke((Invoke) newNode);
                }
            }

            callerCallsiteHolder.computeProbabilities();
        }
        catch (BailoutException bailout)
        {
            throw bailout;
        }
        catch (AssertionError | RuntimeException e)
        {
            throw new GraalError(e).addContext(calleeInfo.toString());
        }
        catch (GraalError e)
        {
            throw e.addContext(calleeInfo.toString());
        }
    }

    /**
     *
     * This method attempts:
     * <ol>
     * <li>to inline at the callsite given by <code>calleeInvocation</code>, where that callsite
     * belongs to the {@link CallsiteHolderExplorable} at the top of the {@link #graphQueue}
     * maintained in this class.</li>
     * <li>otherwise, to devirtualize the callsite in question.</li>
     * </ol>
     *
     * @return true iff inlining was actually performed
     */
    private boolean tryToInline(MethodInvocation calleeInvocation, int inliningDepth)
    {
        CallsiteHolderExplorable callerCallsiteHolder = (CallsiteHolderExplorable) currentGraph();
        InlineInfo calleeInfo = calleeInvocation.callee();

        InliningPolicy.Decision decision = inliningPolicy.isWorthInlining(context.getReplacements(), calleeInvocation, inliningDepth, true);
        if (decision.shouldInline())
        {
            doInline(callerCallsiteHolder, calleeInvocation, decision.getReason());
            return true;
        }

        if (context.getOptimisticOptimizations().devirtualizeInvokes(calleeInfo.graph().getOptions()))
        {
            calleeInfo.tryToDevirtualizeInvoke(new Providers(context));
        }

        return false;
    }

    /**
     * This method picks one of the callsites belonging to the current
     * {@link CallsiteHolderExplorable}. Provided the callsite qualifies to be analyzed for
     * inlining, this method prepares a new stack top in {@link InliningData} for such callsite,
     * which comprises:
     *
     * <li>preparing a summary of feasible targets, ie preparing an {@link InlineInfo}</li>
     * <li>based on it, preparing the stack top proper which consists of:</li>
     *
     * <li>one {@link MethodInvocation}</li>
     * <li>a {@link CallsiteHolder} for each feasible target</li>
     *
     * The thus prepared "stack top" is needed by {@link #moveForward()} to explore the space of
     * inlining decisions (each decision one of: backtracking, delving, inlining).
     *
     * The {@link InlineInfo} used to get things rolling is kept around in the
     * {@link MethodInvocation}, it will be needed in case of inlining, see
     * {@link InlineInfo#inline(Providers, String)}
     */
    private void processNextInvoke()
    {
        CallsiteHolderExplorable callsiteHolder = (CallsiteHolderExplorable) currentGraph();
        Invoke invoke = callsiteHolder.popInvoke();
        InlineInfo info = getInlineInfo(invoke);

        if (info != null)
        {
            info.populateInlinableElements(context, currentGraph().graph(), canonicalizer, rootGraph.getOptions());
            double invokeProbability = callsiteHolder.invokeProbability(invoke);
            double invokeRelevance = callsiteHolder.invokeRelevance(invoke);
            MethodInvocation methodInvocation = new MethodInvocation(info, invokeProbability, invokeRelevance, freshlyInstantiatedArguments(invoke, callsiteHolder.getFixedParams()));
            pushInvocationAndGraphs(methodInvocation);
        }
    }

    /**
     * Gets the freshly instantiated arguments.
     *
     * A freshly instantiated argument is either:
     *
     * <li>an {@link InliningData#isFreshInstantiation(graalvm.compiler.nodes.ValueNode)}</li>
     * <li>a fixed-param, ie a {@link ParameterNode} receiving a freshly instantiated argument</li>
     *
     * @return the positions of freshly instantiated arguments in the argument list of the
     *         <code>invoke</code>, or null if no such positions exist.
     */
    public static BitSet freshlyInstantiatedArguments(Invoke invoke, EconomicSet<ParameterNode> fixedParams)
    {
        BitSet result = null;
        int argIdx = 0;
        for (ValueNode arg : invoke.callTarget().arguments())
        {
            if (isFreshInstantiation(arg) || (arg instanceof ParameterNode && fixedParams.contains((ParameterNode) arg)))
            {
                if (result == null)
                {
                    result = new BitSet();
                }
                result.set(argIdx);
            }
            argIdx++;
        }
        return result;
    }

    private static boolean paramsAndInvokeAreInSameGraph(Invoke invoke, EconomicSet<ParameterNode> fixedParams)
    {
        if (fixedParams.isEmpty())
        {
            return true;
        }
        for (ParameterNode p : fixedParams)
        {
            if (p.graph() != invoke.asNode().graph())
            {
                return false;
            }
        }
        return true;
    }

    public int graphCount()
    {
        return graphQueue.size();
    }

    public boolean hasUnprocessedGraphs()
    {
        return !graphQueue.isEmpty();
    }

    private CallsiteHolder currentGraph()
    {
        return graphQueue.peek();
    }

    private void popGraph()
    {
        graphQueue.pop();
    }

    private void popGraphs(int count)
    {
        for (int i = 0; i < count; i++)
        {
            graphQueue.pop();
        }
    }

    private static final Object[] NO_CONTEXT = {};

    private MethodInvocation currentInvocation()
    {
        return invocationQueue.peekFirst();
    }

    private void pushInvocationAndGraphs(MethodInvocation methodInvocation)
    {
        invocationQueue.addFirst(methodInvocation);
        InlineInfo info = methodInvocation.callee();
        maxGraphs += info.numberOfMethods();
        for (int i = 0; i < info.numberOfMethods(); i++)
        {
            CallsiteHolder ch = methodInvocation.buildCallsiteHolderForElement(i);
            graphQueue.push(ch);
        }
    }

    private void popInvocation()
    {
        maxGraphs -= invocationQueue.peekFirst().callee().numberOfMethods();
        invocationQueue.removeFirst();
    }

    public int countRecursiveInlining(ResolvedJavaMethod method)
    {
        int count = 0;
        for (CallsiteHolder callsiteHolder : graphQueue)
        {
            if (method.equals(callsiteHolder.method()))
            {
                count++;
            }
        }
        return count;
    }

    public int inliningDepth()
    {
        return invocationQueue.size() - 1;
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder("Invocations: ");

        for (MethodInvocation invocation : invocationQueue)
        {
            if (invocation.callee() != null)
            {
                result.append(invocation.callee().numberOfMethods());
                result.append("x ");
                result.append(invocation.callee().invoke());
                result.append("; ");
            }
        }

        result.append("\nGraphs: ");
        for (CallsiteHolder graph : graphQueue)
        {
            result.append(graph.graph());
            result.append("; ");
        }

        return result.toString();
    }

    /**
     * Gets a stack trace representing the current inlining stack represented by this object.
     */
    public Collection<StackTraceElement> getInvocationStackTrace()
    {
        List<StackTraceElement> result = new ArrayList<>();
        for (CallsiteHolder graph : graphQueue)
        {
            result.add(graph.method().asStackTraceElement(0));
        }

        return result;
    }

    private boolean contains(StructuredGraph graph)
    {
        for (CallsiteHolder info : graphQueue)
        {
            if (info.graph() == graph)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * The stack realized by {@link InliningData} grows and shrinks as choices are made among the
     * alternatives below:
     * <ol>
     * <li>not worth inlining: pop stack top, which comprises:
     * <ul>
     * <li>pop any remaining graphs not yet delved into</li>
     * <li>pop the current invocation</li>
     * </ul>
     * </li>
     * <li>{@link #processNextInvoke() delve} into one of the callsites hosted in the current graph,
     * such callsite is explored next by {@link #moveForward()}</li>
     * <li>{@link #tryToInline(MethodInvocation, int) try to inline}: move past the current graph
     * (remove it from the topmost element).
     * <ul>
     * <li>If that was the last one then {@link #tryToInline(MethodInvocation, int) try to inline}
     * the callsite under consideration (ie, the "current invocation").</li>
     * <li>Whether inlining occurs or not, that callsite is removed from the top of
     * {@link InliningData} .</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     *
     * <p>
     * Some facts about the alternatives above:
     * <ul>
     * <li>the first step amounts to backtracking, the 2nd one to depth-search, and the 3rd one also
     * involves backtracking (however possibly after inlining).</li>
     * <li>the choice of abandon-and-backtrack or delve-into depends on
     * {@link InliningPolicy#isWorthInlining} and {@link InliningPolicy#continueInlining}.</li>
     * <li>the 3rd choice is picked whenever none of the previous choices are made</li>
     * </ul>
     * </p>
     *
     * @return true iff inlining was actually performed
     */
    public boolean moveForward()
    {
        final MethodInvocation currentInvocation = currentInvocation();

        final boolean backtrack = (!currentInvocation.isRoot() && !inliningPolicy.isWorthInlining(context.getReplacements(), currentInvocation, inliningDepth(), false).shouldInline());
        if (backtrack)
        {
            int remainingGraphs = currentInvocation.totalGraphs() - currentInvocation.processedGraphs();
            popGraphs(remainingGraphs);
            popInvocation();
            return false;
        }

        final boolean delve = currentGraph().hasRemainingInvokes() && inliningPolicy.continueInlining(currentGraph().graph());
        if (delve)
        {
            processNextInvoke();
            return false;
        }

        popGraph();
        if (currentInvocation.isRoot())
        {
            return false;
        }

        // try to inline
        currentInvocation.incrementProcessedGraphs();
        if (currentInvocation.processedGraphs() == currentInvocation.totalGraphs())
        {
            /*
             * "all of currentInvocation's graphs processed" amounts to
             * "all concrete methods that come into question already had the callees they contain analyzed for inlining"
             */
            popInvocation();
            if (tryToInline(currentInvocation, inliningDepth() + 1))
            {
                // Report real progress only if we inline into the root graph
                return currentGraph().graph() == rootGraph;
            }
            return false;
        }

        return false;
    }

    /**
     * Checks an invariant that {@link #moveForward()} must maintain: "the top invocation records
     * how many concrete target methods (for it) remain on the {@link #graphQueue}; those targets
     * 'belong' to the current invocation in question.
     */
    private boolean topGraphsForTopInvocation()
    {
        if (invocationQueue.isEmpty())
        {
            return true;
        }
        if (currentInvocation().isRoot())
        {
            return true;
        }
        final int remainingGraphs = currentInvocation().totalGraphs() - currentInvocation().processedGraphs();
        final Iterator<CallsiteHolder> iter = graphQueue.iterator();
        for (int i = (remainingGraphs - 1); i >= 0; i--)
        {
            if (!iter.hasNext())
            {
                return false;
            }
            CallsiteHolder queuedTargetCH = iter.next();
            Inlineable targetIE = currentInvocation().callee().inlineableElementAt(i);
            InlineableGraph targetIG = (InlineableGraph) targetIE;
        }
        return true;
    }

    /**
     * This method checks invariants for this class. Named after shorthand for "internal
     * representation is ok".
     */
    public boolean repOK()
    {
        return true;
    }
}
