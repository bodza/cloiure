package giraaff.nodes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.MapCursor;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.ObjectStamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.NodeStack;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.GuardNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.LimitedValueProxy;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.ValueProxy;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualObjectNode;

// @class GraphUtil
public final class GraphUtil
{
    // @cons
    private GraphUtil()
    {
        super();
    }

    private static void markFixedNodes(FixedNode __node, EconomicSet<Node> __markedNodes, EconomicMap<AbstractMergeNode, List<AbstractEndNode>> __unmarkedMerges)
    {
        NodeStack __workStack = new NodeStack();
        __workStack.push(__node);
        while (!__workStack.isEmpty())
        {
            Node __fixedNode = __workStack.pop();
            __markedNodes.add(__fixedNode);
            if (__fixedNode instanceof AbstractMergeNode)
            {
                __unmarkedMerges.removeKey((AbstractMergeNode) __fixedNode);
            }
            while (__fixedNode instanceof FixedWithNextNode)
            {
                __fixedNode = ((FixedWithNextNode) __fixedNode).next();
                if (__fixedNode != null)
                {
                    __markedNodes.add(__fixedNode);
                }
            }
            if (__fixedNode instanceof ControlSplitNode)
            {
                for (Node __successor : __fixedNode.successors())
                {
                    __workStack.push(__successor);
                }
            }
            else if (__fixedNode instanceof AbstractEndNode)
            {
                AbstractEndNode __end = (AbstractEndNode) __fixedNode;
                AbstractMergeNode __merge = __end.merge();
                if (__merge != null)
                {
                    if (__merge instanceof LoopBeginNode)
                    {
                        if (__end == ((LoopBeginNode) __merge).forwardEnd())
                        {
                            __workStack.push(__merge);
                            continue;
                        }
                        if (__markedNodes.contains(__merge))
                        {
                            continue;
                        }
                    }
                    List<AbstractEndNode> __endsSeen = __unmarkedMerges.get(__merge);
                    if (__endsSeen == null)
                    {
                        __endsSeen = new ArrayList<>(__merge.forwardEndCount());
                        __unmarkedMerges.put(__merge, __endsSeen);
                    }
                    __endsSeen.add(__end);
                    if (!(__end instanceof LoopEndNode) && __endsSeen.size() == __merge.forwardEndCount())
                    {
                        // all this merge's forward ends are marked: it needs to be killed
                        __workStack.push(__merge);
                    }
                }
            }
        }
    }

    private static void fixSurvivingAffectedMerges(EconomicSet<Node> __markedNodes, EconomicMap<AbstractMergeNode, List<AbstractEndNode>> __unmarkedMerges)
    {
        MapCursor<AbstractMergeNode, List<AbstractEndNode>> __cursor = __unmarkedMerges.getEntries();
        while (__cursor.advance())
        {
            AbstractMergeNode __merge = __cursor.getKey();
            for (AbstractEndNode __end : __cursor.getValue())
            {
                __merge.removeEnd(__end);
            }
            if (__merge.phiPredecessorCount() == 1)
            {
                if (__merge instanceof LoopBeginNode)
                {
                    LoopBeginNode __loopBegin = (LoopBeginNode) __merge;
                    for (LoopExitNode __loopExit : __loopBegin.loopExits().snapshot())
                    {
                        if (__markedNodes.contains(__loopExit))
                        {
                            // disconnect from loop begin so that reduceDegenerateLoopBegin doesn't transform it into a new beginNode
                            __loopExit.replaceFirstInput(__loopBegin, null);
                        }
                    }
                    __merge.graph().reduceDegenerateLoopBegin(__loopBegin);
                }
                else
                {
                    __merge.graph().reduceTrivialMerge(__merge);
                }
            }
        }
    }

    private static void markUsages(EconomicSet<Node> __markedNodes)
    {
        NodeStack __workStack = new NodeStack(__markedNodes.size() + 4);
        for (Node __marked : __markedNodes)
        {
            __workStack.push(__marked);
        }
        while (!__workStack.isEmpty())
        {
            Node __marked = __workStack.pop();
            for (Node __usage : __marked.usages())
            {
                if (!__markedNodes.contains(__usage))
                {
                    __workStack.push(__usage);
                    __markedNodes.add(__usage);
                }
            }
        }
    }

    public static void killCFG(FixedNode __node)
    {
        EconomicSet<Node> __markedNodes = EconomicSet.create();
        EconomicMap<AbstractMergeNode, List<AbstractEndNode>> __unmarkedMerges = EconomicMap.create();

        // detach this node from CFG
        __node.replaceAtPredecessor(null);

        markFixedNodes(__node, __markedNodes, __unmarkedMerges);

        fixSurvivingAffectedMerges(__markedNodes, __unmarkedMerges);

        // mark non-fixed nodes
        markUsages(__markedNodes);

        // detach marked nodes from non-marked nodes
        for (Node __marked : __markedNodes)
        {
            for (Node __input : __marked.inputs())
            {
                if (!__markedNodes.contains(__input))
                {
                    __marked.replaceFirstInput(__input, null);
                    tryKillUnused(__input);
                }
            }
        }
        // kill marked nodes
        for (Node __marked : __markedNodes)
        {
            if (__marked.isAlive())
            {
                __marked.markDeleted();
            }
        }
    }

    public static boolean isFloatingNode(Node __n)
    {
        return !(__n instanceof FixedNode);
    }

    public static void killWithUnusedFloatingInputs(Node __node)
    {
        killWithUnusedFloatingInputs(__node, false);
    }

    public static void killWithUnusedFloatingInputs(Node __node, boolean __mayKillGuard)
    {
        __node.markDeleted();
        outer: for (Node in : __node.inputs())
        {
            if (in.isAlive())
            {
                in.removeUsage(__node);
                if (in.hasNoUsages())
                {
                    __node.maybeNotifyZeroUsages(in);
                }
                if (isFloatingNode(in))
                {
                    if (in.hasNoUsages())
                    {
                        if (in instanceof GuardNode)
                        {
                            // Guard nodes are only killed if their anchor dies.
                        }
                        else
                        {
                            killWithUnusedFloatingInputs(in);
                        }
                    }
                    else if (in instanceof PhiNode)
                    {
                        for (Node __use : in.usages())
                        {
                            if (__use != in)
                            {
                                continue outer;
                            }
                        }
                        in.replaceAtUsages(null);
                        killWithUnusedFloatingInputs(in);
                    }
                }
            }
        }
    }

    /**
     * Removes all nodes created after the {@code mark}, assuming no "old" nodes point to "new" nodes.
     */
    public static void removeNewNodes(Graph __graph, Graph.Mark __mark)
    {
        for (Node __n : __graph.getNewNodes(__mark))
        {
            __n.markDeleted();
            for (Node __in : __n.inputs())
            {
                __in.removeUsage(__n);
            }
        }
    }

    public static void removeFixedWithUnusedInputs(FixedWithNextNode __fixed)
    {
        if (__fixed instanceof StateSplit)
        {
            FrameState __stateAfter = ((StateSplit) __fixed).stateAfter();
            if (__stateAfter != null)
            {
                ((StateSplit) __fixed).setStateAfter(null);
                if (__stateAfter.hasNoUsages())
                {
                    killWithUnusedFloatingInputs(__stateAfter);
                }
            }
        }
        unlinkFixedNode(__fixed);
        killWithUnusedFloatingInputs(__fixed);
    }

    public static void unlinkFixedNode(FixedWithNextNode __fixed)
    {
        FixedNode __next = __fixed.next();
        __fixed.setNext(null);
        __fixed.replaceAtPredecessor(__next);
    }

    public static void checkRedundantPhi(PhiNode __phiNode)
    {
        if (__phiNode.isDeleted() || __phiNode.valueCount() == 1)
        {
            return;
        }

        ValueNode __singleValue = __phiNode.singleValueOrThis();
        if (__singleValue != __phiNode)
        {
            Collection<PhiNode> __phiUsages = __phiNode.usages().filter(PhiNode.class).snapshot();
            Collection<ProxyNode> __proxyUsages = __phiNode.usages().filter(ProxyNode.class).snapshot();
            __phiNode.replaceAtUsagesAndDelete(__singleValue);
            for (PhiNode __phi : __phiUsages)
            {
                checkRedundantPhi(__phi);
            }
            for (ProxyNode __proxy : __proxyUsages)
            {
                checkRedundantProxy(__proxy);
            }
        }
    }

    public static void checkRedundantProxy(ProxyNode __vpn)
    {
        if (__vpn.isDeleted())
        {
            return;
        }
        AbstractBeginNode __proxyPoint = __vpn.proxyPoint();
        if (__proxyPoint instanceof LoopExitNode)
        {
            LoopExitNode __exit = (LoopExitNode) __proxyPoint;
            LoopBeginNode __loopBegin = __exit.loopBegin();
            Node __vpnValue = __vpn.value();
            for (ValueNode __v : __loopBegin.stateAfter().values())
            {
                ValueNode __v2 = __v;
                if (__loopBegin.isPhiAtMerge(__v2))
                {
                    __v2 = ((PhiNode) __v2).valueAt(__loopBegin.forwardEnd());
                }
                if (__vpnValue == __v2)
                {
                    Collection<PhiNode> __phiUsages = __vpn.usages().filter(PhiNode.class).snapshot();
                    Collection<ProxyNode> __proxyUsages = __vpn.usages().filter(ProxyNode.class).snapshot();
                    __vpn.replaceAtUsagesAndDelete(__vpnValue);
                    for (PhiNode __phi : __phiUsages)
                    {
                        checkRedundantPhi(__phi);
                    }
                    for (ProxyNode __proxy : __proxyUsages)
                    {
                        checkRedundantProxy(__proxy);
                    }
                    return;
                }
            }
        }
    }

    /**
     * Remove loop header without loop ends. This can happen with degenerated loops like this one:
     *
     * <pre>
     * for ( ; ; ) {
     *     try {
     *         break;
     *     } catch (UnresolvedException iioe) {
     *     }
     * }
     * </pre>
     */
    public static void normalizeLoops(StructuredGraph __graph)
    {
        boolean __loopRemoved = false;
        for (LoopBeginNode __begin : __graph.getNodes(LoopBeginNode.TYPE))
        {
            if (__begin.loopEnds().isEmpty())
            {
                __graph.reduceDegenerateLoopBegin(__begin);
                __loopRemoved = true;
            }
            else
            {
                normalizeLoopBegin(__begin);
            }
        }

        if (__loopRemoved)
        {
            /*
             * Removing a degenerated loop can make non-loop phi functions unnecessary. Therefore,
             * we re-check all phi functions and remove redundant ones.
             */
            for (Node __node : __graph.getNodes())
            {
                if (__node instanceof PhiNode)
                {
                    checkRedundantPhi((PhiNode) __node);
                }
            }
        }
    }

    private static void normalizeLoopBegin(LoopBeginNode __begin)
    {
        // Delete unnecessary loop phi functions, i.e. phi functions where all inputs are either the same or the phi itself.
        for (PhiNode __phi : __begin.phis().snapshot())
        {
            GraphUtil.checkRedundantPhi(__phi);
        }
        for (LoopExitNode __exit : __begin.loopExits())
        {
            for (ProxyNode __vpn : __exit.proxies().snapshot())
            {
                GraphUtil.checkRedundantProxy(__vpn);
            }
        }
    }

    /**
     * Gets the original value by iterating through all {@link ValueProxy ValueProxies}.
     *
     * @param value the start value.
     * @return the first non-proxy value encountered
     */
    public static ValueNode unproxify(ValueNode __value)
    {
        if (__value instanceof ValueProxy)
        {
            return unproxify((ValueProxy) __value);
        }
        else
        {
            return __value;
        }
    }

    /**
     * Gets the original value by iterating through all {@link ValueProxy ValueProxies}.
     *
     * @param value the start value proxy.
     * @return the first non-proxy value encountered
     */
    public static ValueNode unproxify(ValueProxy __value)
    {
        if (__value != null)
        {
            ValueNode __result = __value.getOriginalNode();
            while (__result instanceof ValueProxy)
            {
                __result = ((ValueProxy) __result).getOriginalNode();
            }
            return __result;
        }
        else
        {
            return null;
        }
    }

    public static ValueNode skipPi(ValueNode __node)
    {
        ValueNode __n = __node;
        while (__n instanceof PiNode)
        {
            PiNode __piNode = (PiNode) __n;
            __n = __piNode.getOriginalNode();
        }
        return __n;
    }

    public static ValueNode skipPiWhileNonNull(ValueNode __node)
    {
        ValueNode __n = __node;
        while (__n instanceof PiNode)
        {
            PiNode __piNode = (PiNode) __n;
            ObjectStamp __originalStamp = (ObjectStamp) __piNode.getOriginalNode().stamp(NodeView.DEFAULT);
            if (__originalStamp.nonNull())
            {
                __n = __piNode.getOriginalNode();
            }
            else
            {
                break;
            }
        }
        return __n;
    }

    /**
     * Looks for an {@link ArrayLengthProvider} while iterating through all {@link ValueProxy ValueProxies}.
     *
     * @param value The start value.
     * @return The array length if one was found, or null otherwise.
     */
    public static ValueNode arrayLength(ValueNode __value)
    {
        ValueNode __current = __value;
        do
        {
            if (__current instanceof ArrayLengthProvider)
            {
                ValueNode __length = ((ArrayLengthProvider) __current).length();
                if (__length != null)
                {
                    return __length;
                }
            }
            if (__current instanceof ValueProxy)
            {
                __current = ((ValueProxy) __current).getOriginalNode();
            }
            else
            {
                break;
            }
        } while (true);
        return null;
    }

    /**
     * Tries to find an original value of the given node by traversing through proxies and
     * unambiguous phis. Note that this method will perform an exhaustive search through phis. It is
     * intended to be used during graph building, when phi nodes aren't yet canonicalized.
     *
     * @param value The node whose original value should be determined.
     * @return The original value (which might be the input value itself).
     */
    public static ValueNode originalValue(ValueNode __value)
    {
        return originalValueSimple(__value);
    }

    private static ValueNode originalValueSimple(ValueNode __value)
    {
        // The very simple case: look through proxies.
        ValueNode __cur = originalValueForProxy(__value);

        while (__cur instanceof PhiNode)
        {
            // We found a phi function. Check if we can analyze it without allocating temporary data structures.
            PhiNode __phi = (PhiNode) __cur;

            ValueNode __phiSingleValue = null;
            int __count = __phi.valueCount();
            for (int __i = 0; __i < __count; ++__i)
            {
                ValueNode __phiCurValue = originalValueForProxy(__phi.valueAt(__i));
                if (__phiCurValue == __phi)
                {
                    // Simple cycle, we can ignore the input value.
                }
                else if (__phiSingleValue == null)
                {
                    // The first input.
                    __phiSingleValue = __phiCurValue;
                }
                else if (__phiSingleValue != __phiCurValue)
                {
                    // Another input that is different from the first input.

                    if (__phiSingleValue instanceof PhiNode || __phiCurValue instanceof PhiNode)
                    {
                        /*
                         * We have two different input values for the phi function, and at least one
                         * of the inputs is another phi function. We need to do a complicated
                         * exhaustive check.
                         */
                        return originalValueForComplicatedPhi(__phi, new NodeBitMap(__value.graph()));
                    }
                    else
                    {
                        /*
                         * We have two different input values for the phi function, but none of them
                         * is another phi function. This phi function cannot be reduce any further,
                         * so the phi function is the original value.
                         */
                        return __phi;
                    }
                }
            }

            /*
             * Successfully reduced the phi function to a single input value. The single input value
             * can itself be a phi function again, so we might take another loop iteration.
             */
            __cur = __phiSingleValue;
        }

        // We reached a "normal" node, which is the original value.
        return __cur;
    }

    private static ValueNode originalValueForProxy(ValueNode __value)
    {
        ValueNode __cur = __value;
        while (__cur instanceof LimitedValueProxy)
        {
            __cur = ((LimitedValueProxy) __cur).getOriginalNode();
        }
        return __cur;
    }

    /**
     * Handling for complicated nestings of phi functions. We need to reduce phi functions
     * recursively, and need a temporary map of visited nodes to avoid endless recursion of cycles.
     */
    private static ValueNode originalValueForComplicatedPhi(PhiNode __phi, NodeBitMap __visited)
    {
        if (__visited.isMarked(__phi))
        {
            /*
             * Found a phi function that was already seen. Either a cycle, or just a second phi
             * input to a path we have already processed.
             */
            return null;
        }
        __visited.mark(__phi);

        ValueNode __phiSingleValue = null;
        int __count = __phi.valueCount();
        for (int __i = 0; __i < __count; ++__i)
        {
            ValueNode __phiCurValue = originalValueForProxy(__phi.valueAt(__i));
            if (__phiCurValue instanceof PhiNode)
            {
                // Recursively process a phi function input.
                __phiCurValue = originalValueForComplicatedPhi((PhiNode) __phiCurValue, __visited);
            }

            if (__phiCurValue == null)
            {
                // Cycle to a phi function that was already seen. We can ignore this input.
            }
            else if (__phiSingleValue == null)
            {
                // The first input.
                __phiSingleValue = __phiCurValue;
            }
            else if (__phiCurValue != __phiSingleValue)
            {
                /*
                 * Another input that is different from the first input. Since we already
                 * recursively looked through other phi functions, we now know that this phi
                 * function cannot be reduce any further, so the phi function is the original value.
                 */
                return __phi;
            }
        }
        return __phiSingleValue;
    }

    public static boolean tryKillUnused(Node __node)
    {
        if (__node.isAlive() && isFloatingNode(__node) && __node.hasNoUsages() && !(__node instanceof GuardNode))
        {
            killWithUnusedFloatingInputs(__node);
            return true;
        }
        return false;
    }

    /**
     * Returns an iterator that will return the given node followed by all its predecessors, up
     * until the point where {@link Node#predecessor()} returns null.
     *
     * @param start the node at which to start iterating
     */
    public static NodeIterable<FixedNode> predecessorIterable(final FixedNode __start)
    {
        // @closure
        return new NodeIterable<FixedNode>()
        {
            @Override
            public Iterator<FixedNode> iterator()
            {
                // @closure
                return new Iterator<FixedNode>()
                {
                    // @field
                    public FixedNode current = __start;

                    @Override
                    public boolean hasNext()
                    {
                        return current != null;
                    }

                    @Override
                    public FixedNode next()
                    {
                        try
                        {
                            return current;
                        }
                        finally
                        {
                            current = (FixedNode) current.predecessor();
                        }
                    }
                };
            }
        };
    }

    // @class GraphUtil.DefaultSimplifierTool
    private static final class DefaultSimplifierTool implements SimplifierTool
    {
        // @field
        private final MetaAccessProvider metaAccess;
        // @field
        private final ConstantReflectionProvider constantReflection;
        // @field
        private final ConstantFieldProvider constantFieldProvider;
        // @field
        private final boolean canonicalizeReads;
        // @field
        private final Assumptions assumptions;
        // @field
        private final LoweringProvider loweringProvider;

        // @cons
        DefaultSimplifierTool(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, boolean __canonicalizeReads, Assumptions __assumptions, LoweringProvider __loweringProvider)
        {
            super();
            this.metaAccess = __metaAccess;
            this.constantReflection = __constantReflection;
            this.constantFieldProvider = __constantFieldProvider;
            this.canonicalizeReads = __canonicalizeReads;
            this.assumptions = __assumptions;
            this.loweringProvider = __loweringProvider;
        }

        @Override
        public MetaAccessProvider getMetaAccess()
        {
            return metaAccess;
        }

        @Override
        public ConstantReflectionProvider getConstantReflection()
        {
            return constantReflection;
        }

        @Override
        public ConstantFieldProvider getConstantFieldProvider()
        {
            return constantFieldProvider;
        }

        @Override
        public boolean canonicalizeReads()
        {
            return canonicalizeReads;
        }

        @Override
        public boolean allUsagesAvailable()
        {
            return true;
        }

        @Override
        public void deleteBranch(Node __branch)
        {
            FixedNode __fixedBranch = (FixedNode) __branch;
            __fixedBranch.predecessor().replaceFirstSuccessor(__fixedBranch, null);
            GraphUtil.killCFG(__fixedBranch);
        }

        @Override
        public void removeIfUnused(Node __node)
        {
            GraphUtil.tryKillUnused(__node);
        }

        @Override
        public void addToWorkList(Node __node)
        {
        }

        @Override
        public void addToWorkList(Iterable<? extends Node> __nodes)
        {
        }

        @Override
        public Assumptions getAssumptions()
        {
            return assumptions;
        }

        @Override
        public Integer smallestCompareWidth()
        {
            if (loweringProvider != null)
            {
                return loweringProvider.smallestCompareWidth();
            }
            else
            {
                return null;
            }
        }
    }

    public static SimplifierTool getDefaultSimplifier(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, boolean __canonicalizeReads, Assumptions __assumptions)
    {
        return getDefaultSimplifier(__metaAccess, __constantReflection, __constantFieldProvider, __canonicalizeReads, __assumptions, null);
    }

    public static SimplifierTool getDefaultSimplifier(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, boolean __canonicalizeReads, Assumptions __assumptions, LoweringProvider __loweringProvider)
    {
        return new DefaultSimplifierTool(__metaAccess, __constantReflection, __constantFieldProvider, __canonicalizeReads, __assumptions, __loweringProvider);
    }

    public static Constant foldIfConstantAndRemove(ValueNode __node, ValueNode __constant)
    {
        if (__constant.isConstant())
        {
            __node.replaceFirstInput(__constant, null);
            Constant __result = __constant.asConstant();
            tryKillUnused(__constant);
            return __result;
        }
        return null;
    }

    /**
     * Virtualize an array copy.
     *
     * @param tool the virtualization tool
     * @param source the source array
     * @param sourceLength the length of the source array
     * @param newLength the length of the new array
     * @param from the start index in the source array
     * @param newComponentType the component type of the new array
     * @param elementKind the kind of the new array elements
     * @param graph the node graph
     * @param virtualArrayProvider a functional provider that returns a new virtual array given the
     *            component type and length
     */
    public static void virtualizeArrayCopy(VirtualizerTool __tool, ValueNode __source, ValueNode __sourceLength, ValueNode __newLength, ValueNode __from, ResolvedJavaType __newComponentType, JavaKind __elementKind, StructuredGraph __graph, BiFunction<ResolvedJavaType, Integer, VirtualArrayNode> __virtualArrayProvider)
    {
        ValueNode __sourceAlias = __tool.getAlias(__source);
        ValueNode __replacedSourceLength = __tool.getAlias(__sourceLength);
        ValueNode __replacedNewLength = __tool.getAlias(__newLength);
        ValueNode __replacedFrom = __tool.getAlias(__from);
        if (!__replacedNewLength.isConstant() || !__replacedFrom.isConstant() || !__replacedSourceLength.isConstant())
        {
            return;
        }

        int __fromInt = __replacedFrom.asJavaConstant().asInt();
        int __newLengthInt = __replacedNewLength.asJavaConstant().asInt();
        int __sourceLengthInt = __replacedSourceLength.asJavaConstant().asInt();
        if (__sourceAlias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __sourceVirtual = (VirtualObjectNode) __sourceAlias;
        }

        if (__fromInt < 0 || __newLengthInt < 0 || __fromInt > __sourceLengthInt)
        {
            // Illegal values for either from index, the new length or the source length.
            return;
        }

        if (__newLengthInt >= __tool.getMaximumEntryCount())
        {
            // The new array size is higher than maximum allowed size of virtualized objects.
            return;
        }

        ValueNode[] __newEntryState = new ValueNode[__newLengthInt];
        int __readLength = Math.min(__newLengthInt, __sourceLengthInt - __fromInt);

        if (__sourceAlias instanceof VirtualObjectNode)
        {
            // The source array is virtualized, just copy over the values.
            VirtualObjectNode __sourceVirtual = (VirtualObjectNode) __sourceAlias;
            for (int __i = 0; __i < __readLength; __i++)
            {
                __newEntryState[__i] = __tool.getEntry(__sourceVirtual, __fromInt + __i);
            }
        }
        else
        {
            // The source array is not virtualized, emit index loads.
            for (int __i = 0; __i < __readLength; __i++)
            {
                LoadIndexedNode __load = new LoadIndexedNode(null, __sourceAlias, ConstantNode.forInt(__i + __fromInt, __graph), __elementKind);
                __tool.addNode(__load);
                __newEntryState[__i] = __load;
            }
        }
        if (__readLength < __newLengthInt)
        {
            // Pad the copy with the default value of its elment kind.
            ValueNode __defaultValue = ConstantNode.defaultForKind(__elementKind, __graph);
            for (int __i = __readLength; __i < __newLengthInt; __i++)
            {
                __newEntryState[__i] = __defaultValue;
            }
        }
        // Perform the replacement.
        VirtualArrayNode __newVirtualArray = __virtualArrayProvider.apply(__newComponentType, __newLengthInt);
        __tool.createVirtualObject(__newVirtualArray, __newEntryState, Collections.<MonitorIdNode> emptyList(), false);
        __tool.replaceWithVirtual(__newVirtualArray);
    }
}
