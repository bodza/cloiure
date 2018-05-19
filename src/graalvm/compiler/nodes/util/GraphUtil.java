package graalvm.compiler.nodes.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import graalvm.compiler.bytecode.Bytecode;
import graalvm.compiler.code.SourceStackTraceBailoutException;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.graph.NodeSourcePosition;
import graalvm.compiler.graph.NodeStack;
import graalvm.compiler.graph.Position;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.GuardNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.ProxyNode;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.LoadIndexedNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;
import graalvm.compiler.nodes.spi.LimitedValueProxy;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.spi.ValueProxy;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualArrayNode;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodePosition;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class GraphUtil
{
    public static class Options
    {
        @Option(help = "Verify that there are no new unused nodes when performing killCFG", type = OptionType.Debug)
        public static final OptionKey<Boolean> VerifyKillCFGUnusedNodes = new OptionKey<>(false);
    }

    private static void killCFGInner(FixedNode node)
    {
        EconomicSet<Node> markedNodes = EconomicSet.create();
        EconomicMap<AbstractMergeNode, List<AbstractEndNode>> unmarkedMerges = EconomicMap.create();

        // Detach this node from CFG
        node.replaceAtPredecessor(null);

        markFixedNodes(node, markedNodes, unmarkedMerges);

        fixSurvivingAffectedMerges(markedNodes, unmarkedMerges);

        // Mark non-fixed nodes
        markUsages(markedNodes);

        // Detach marked nodes from non-marked nodes
        for (Node marked : markedNodes)
        {
            for (Node input : marked.inputs())
            {
                if (!markedNodes.contains(input))
                {
                    marked.replaceFirstInput(input, null);
                    tryKillUnused(input);
                }
            }
        }
        // Kill marked nodes
        for (Node marked : markedNodes)
        {
            if (marked.isAlive())
            {
                marked.markDeleted();
            }
        }
    }

    private static void markFixedNodes(FixedNode node, EconomicSet<Node> markedNodes, EconomicMap<AbstractMergeNode, List<AbstractEndNode>> unmarkedMerges)
    {
        NodeStack workStack = new NodeStack();
        workStack.push(node);
        while (!workStack.isEmpty())
        {
            Node fixedNode = workStack.pop();
            markedNodes.add(fixedNode);
            if (fixedNode instanceof AbstractMergeNode)
            {
                unmarkedMerges.removeKey((AbstractMergeNode) fixedNode);
            }
            while (fixedNode instanceof FixedWithNextNode)
            {
                fixedNode = ((FixedWithNextNode) fixedNode).next();
                if (fixedNode != null)
                {
                    markedNodes.add(fixedNode);
                }
            }
            if (fixedNode instanceof ControlSplitNode)
            {
                for (Node successor : fixedNode.successors())
                {
                    workStack.push(successor);
                }
            }
            else if (fixedNode instanceof AbstractEndNode)
            {
                AbstractEndNode end = (AbstractEndNode) fixedNode;
                AbstractMergeNode merge = end.merge();
                if (merge != null)
                {
                    if (merge instanceof LoopBeginNode)
                    {
                        if (end == ((LoopBeginNode) merge).forwardEnd())
                        {
                            workStack.push(merge);
                            continue;
                        }
                        if (markedNodes.contains(merge))
                        {
                            continue;
                        }
                    }
                    List<AbstractEndNode> endsSeen = unmarkedMerges.get(merge);
                    if (endsSeen == null)
                    {
                        endsSeen = new ArrayList<>(merge.forwardEndCount());
                        unmarkedMerges.put(merge, endsSeen);
                    }
                    endsSeen.add(end);
                    if (!(end instanceof LoopEndNode) && endsSeen.size() == merge.forwardEndCount())
                    {
                        // all this merge's forward ends are marked: it needs to be killed
                        workStack.push(merge);
                    }
                }
            }
        }
    }

    private static void fixSurvivingAffectedMerges(EconomicSet<Node> markedNodes, EconomicMap<AbstractMergeNode, List<AbstractEndNode>> unmarkedMerges)
    {
        MapCursor<AbstractMergeNode, List<AbstractEndNode>> cursor = unmarkedMerges.getEntries();
        while (cursor.advance())
        {
            AbstractMergeNode merge = cursor.getKey();
            for (AbstractEndNode end : cursor.getValue())
            {
                merge.removeEnd(end);
            }
            if (merge.phiPredecessorCount() == 1)
            {
                if (merge instanceof LoopBeginNode)
                {
                    LoopBeginNode loopBegin = (LoopBeginNode) merge;
                    for (LoopExitNode loopExit : loopBegin.loopExits().snapshot())
                    {
                        if (markedNodes.contains(loopExit))
                        {
                            /*
                             * disconnect from loop begin so that reduceDegenerateLoopBegin doesn't
                             * transform it into a new beginNode
                             */
                            loopExit.replaceFirstInput(loopBegin, null);
                        }
                    }
                    merge.graph().reduceDegenerateLoopBegin(loopBegin);
                }
                else
                {
                    merge.graph().reduceTrivialMerge(merge);
                }
            }
        }
    }

    private static void markUsages(EconomicSet<Node> markedNodes)
    {
        NodeStack workStack = new NodeStack(markedNodes.size() + 4);
        for (Node marked : markedNodes)
        {
            workStack.push(marked);
        }
        while (!workStack.isEmpty())
        {
            Node marked = workStack.pop();
            for (Node usage : marked.usages())
            {
                if (!markedNodes.contains(usage))
                {
                    workStack.push(usage);
                    markedNodes.add(usage);
                }
            }
        }
    }

    public static void killCFG(FixedNode node)
    {
        EconomicSet<Node> unusedNodes = null;
        EconomicSet<Node> unsafeNodes = null;
        Graph.NodeEventScope nodeEventScope = null;
        OptionValues options = node.getOptions();
        if (GraphUtil.Options.VerifyKillCFGUnusedNodes.getValue(options))
        {
            EconomicSet<Node> collectedUnusedNodes = unusedNodes = EconomicSet.create(Equivalence.IDENTITY);
            nodeEventScope = node.graph().trackNodeEvents(new Graph.NodeEventListener()
            {
                @Override
                public void changed(Graph.NodeEvent e, Node n)
                {
                    if (e == Graph.NodeEvent.ZERO_USAGES && isFloatingNode(n) && !(n instanceof GuardNode))
                    {
                        collectedUnusedNodes.add(n);
                    }
                }
            });
        }
        killCFGInner(node);
        if (GraphUtil.Options.VerifyKillCFGUnusedNodes.getValue(options))
        {
            nodeEventScope.close();
            Iterator<Node> iterator = unusedNodes.iterator();
            while (iterator.hasNext())
            {
                Node curNode = iterator.next();
                if (curNode.isDeleted())
                {
                    iterator.remove();
                }
            }
        }
    }

    public static boolean isFloatingNode(Node n)
    {
        return !(n instanceof FixedNode);
    }

    private static boolean checkKill(Node node, boolean mayKillGuard)
    {
        node.assertTrue(mayKillGuard || !(node instanceof GuardNode), "must not be a guard node %s", node);
        node.assertTrue(node.isAlive(), "must be alive");
        node.assertTrue(node.hasNoUsages(), "cannot kill node %s because of usages: %s", node, node.usages());
        node.assertTrue(node.predecessor() == null, "cannot kill node %s because of predecessor: %s", node, node.predecessor());
        return true;
    }

    public static void killWithUnusedFloatingInputs(Node node)
    {
        killWithUnusedFloatingInputs(node, false);
    }

    public static void killWithUnusedFloatingInputs(Node node, boolean mayKillGuard)
    {
        node.markDeleted();
        outer: for (Node in : node.inputs())
        {
            if (in.isAlive())
            {
                in.removeUsage(node);
                if (in.hasNoUsages())
                {
                    node.maybeNotifyZeroUsages(in);
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
                        for (Node use : in.usages())
                        {
                            if (use != in)
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
     * Removes all nodes created after the {@code mark}, assuming no "old" nodes point to "new"
     * nodes.
     */
    public static void removeNewNodes(Graph graph, Graph.Mark mark)
    {
        for (Node n : graph.getNewNodes(mark))
        {
            n.markDeleted();
            for (Node in : n.inputs())
            {
                in.removeUsage(n);
            }
        }
    }

    public static void removeFixedWithUnusedInputs(FixedWithNextNode fixed)
    {
        if (fixed instanceof StateSplit)
        {
            FrameState stateAfter = ((StateSplit) fixed).stateAfter();
            if (stateAfter != null)
            {
                ((StateSplit) fixed).setStateAfter(null);
                if (stateAfter.hasNoUsages())
                {
                    killWithUnusedFloatingInputs(stateAfter);
                }
            }
        }
        unlinkFixedNode(fixed);
        killWithUnusedFloatingInputs(fixed);
    }

    public static void unlinkFixedNode(FixedWithNextNode fixed)
    {
        FixedNode next = fixed.next();
        fixed.setNext(null);
        fixed.replaceAtPredecessor(next);
    }

    public static void checkRedundantPhi(PhiNode phiNode)
    {
        if (phiNode.isDeleted() || phiNode.valueCount() == 1)
        {
            return;
        }

        ValueNode singleValue = phiNode.singleValueOrThis();
        if (singleValue != phiNode)
        {
            Collection<PhiNode> phiUsages = phiNode.usages().filter(PhiNode.class).snapshot();
            Collection<ProxyNode> proxyUsages = phiNode.usages().filter(ProxyNode.class).snapshot();
            phiNode.replaceAtUsagesAndDelete(singleValue);
            for (PhiNode phi : phiUsages)
            {
                checkRedundantPhi(phi);
            }
            for (ProxyNode proxy : proxyUsages)
            {
                checkRedundantProxy(proxy);
            }
        }
    }

    public static void checkRedundantProxy(ProxyNode vpn)
    {
        if (vpn.isDeleted())
        {
            return;
        }
        AbstractBeginNode proxyPoint = vpn.proxyPoint();
        if (proxyPoint instanceof LoopExitNode)
        {
            LoopExitNode exit = (LoopExitNode) proxyPoint;
            LoopBeginNode loopBegin = exit.loopBegin();
            Node vpnValue = vpn.value();
            for (ValueNode v : loopBegin.stateAfter().values())
            {
                ValueNode v2 = v;
                if (loopBegin.isPhiAtMerge(v2))
                {
                    v2 = ((PhiNode) v2).valueAt(loopBegin.forwardEnd());
                }
                if (vpnValue == v2)
                {
                    Collection<PhiNode> phiUsages = vpn.usages().filter(PhiNode.class).snapshot();
                    Collection<ProxyNode> proxyUsages = vpn.usages().filter(ProxyNode.class).snapshot();
                    vpn.replaceAtUsagesAndDelete(vpnValue);
                    for (PhiNode phi : phiUsages)
                    {
                        checkRedundantPhi(phi);
                    }
                    for (ProxyNode proxy : proxyUsages)
                    {
                        checkRedundantProxy(proxy);
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
     * for (;;) {
     *     try {
     *         break;
     *     } catch (UnresolvedException iioe) {
     *     }
     * }
     * </pre>
     */
    public static void normalizeLoops(StructuredGraph graph)
    {
        boolean loopRemoved = false;
        for (LoopBeginNode begin : graph.getNodes(LoopBeginNode.TYPE))
        {
            if (begin.loopEnds().isEmpty())
            {
                graph.reduceDegenerateLoopBegin(begin);
                loopRemoved = true;
            }
            else
            {
                normalizeLoopBegin(begin);
            }
        }

        if (loopRemoved)
        {
            /*
             * Removing a degenerated loop can make non-loop phi functions unnecessary. Therefore,
             * we re-check all phi functions and remove redundant ones.
             */
            for (Node node : graph.getNodes())
            {
                if (node instanceof PhiNode)
                {
                    checkRedundantPhi((PhiNode) node);
                }
            }
        }
    }

    private static void normalizeLoopBegin(LoopBeginNode begin)
    {
        // Delete unnecessary loop phi functions, i.e., phi functions where all inputs are either
        // the same or the phi itself.
        for (PhiNode phi : begin.phis().snapshot())
        {
            GraphUtil.checkRedundantPhi(phi);
        }
        for (LoopExitNode exit : begin.loopExits())
        {
            for (ProxyNode vpn : exit.proxies().snapshot())
            {
                GraphUtil.checkRedundantProxy(vpn);
            }
        }
    }

    /**
     * Gets an approximate source code location for a node if possible.
     *
     * @return the StackTraceElements if an approximate source location is found, null otherwise
     */
    public static StackTraceElement[] approxSourceStackTraceElement(Node node)
    {
        NodeSourcePosition position = node.getNodeSourcePosition();
        if (position != null)
        {
            // use GraphBuilderConfiguration and enable trackNodeSourcePosition to get better source
            // positions.
            return approxSourceStackTraceElement(position);
        }
        ArrayList<StackTraceElement> elements = new ArrayList<>();
        Node n = node;
        while (n != null)
        {
            if (n instanceof MethodCallTargetNode)
            {
                elements.add(((MethodCallTargetNode) n).targetMethod().asStackTraceElement(-1));
                n = ((MethodCallTargetNode) n).invoke().asNode();
            }

            if (n instanceof StateSplit)
            {
                FrameState state = ((StateSplit) n).stateAfter();
                elements.addAll(Arrays.asList(approxSourceStackTraceElement(state)));
                break;
            }
            n = n.predecessor();
        }
        return elements.toArray(new StackTraceElement[elements.size()]);
    }

    /**
     * Gets an approximate source code location for frame state.
     *
     * @return the StackTraceElements if an approximate source location is found, null otherwise
     */
    public static StackTraceElement[] approxSourceStackTraceElement(FrameState frameState)
    {
        ArrayList<StackTraceElement> elements = new ArrayList<>();
        FrameState state = frameState;
        while (state != null)
        {
            Bytecode code = state.getCode();
            if (code != null)
            {
                elements.add(code.asStackTraceElement(state.bci - 1));
            }
            state = state.outerFrameState();
        }
        return elements.toArray(new StackTraceElement[0]);
    }

    /**
     * Gets approximate stack trace elements for a bytecode position.
     */
    public static StackTraceElement[] approxSourceStackTraceElement(BytecodePosition bytecodePosition)
    {
        ArrayList<StackTraceElement> elements = new ArrayList<>();
        BytecodePosition position = bytecodePosition;
        while (position != null)
        {
            ResolvedJavaMethod method = position.getMethod();
            if (method != null)
            {
                elements.add(method.asStackTraceElement(position.getBCI()));
            }
            position = position.getCaller();
        }
        return elements.toArray(new StackTraceElement[0]);
    }

    /**
     * Gets an approximate source code location for a node, encoded as an exception, if possible.
     *
     * @return the exception with the location
     */
    public static RuntimeException approxSourceException(Node node, Throwable cause)
    {
        final StackTraceElement[] elements = approxSourceStackTraceElement(node);
        return createBailoutException(cause == null ? "" : cause.getMessage(), cause, elements);
    }

    /**
     * Creates a bailout exception with the given stack trace elements and message.
     *
     * @param message the message of the exception
     * @param elements the stack trace elements
     * @return the exception
     */
    public static BailoutException createBailoutException(String message, Throwable cause, StackTraceElement[] elements)
    {
        return SourceStackTraceBailoutException.create(cause, message, elements);
    }

    /**
     * Gets an approximate source code location for a node if possible.
     *
     * @return a file name and source line number in stack trace format (e.g. "String.java:32") if
     *         an approximate source location is found, null otherwise
     */
    public static String approxSourceLocation(Node node)
    {
        StackTraceElement[] stackTraceElements = approxSourceStackTraceElement(node);
        if (stackTraceElements != null && stackTraceElements.length > 0)
        {
            StackTraceElement top = stackTraceElements[0];
            if (top.getFileName() != null && top.getLineNumber() >= 0)
            {
                return top.getFileName() + ":" + top.getLineNumber();
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the given collection of objects.
     *
     * @param objects The {@link Iterable} that will be used to iterate over the objects.
     * @return A string of the format "[a, b, ...]".
     */
    public static String toString(Iterable<?> objects)
    {
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (Object o : objects)
        {
            str.append(o).append(", ");
        }
        if (str.length() > 1)
        {
            str.setLength(str.length() - 2);
        }
        str.append("]");
        return str.toString();
    }

    /**
     * Gets the original value by iterating through all {@link ValueProxy ValueProxies}.
     *
     * @param value the start value.
     * @return the first non-proxy value encountered
     */
    public static ValueNode unproxify(ValueNode value)
    {
        if (value instanceof ValueProxy)
        {
            return unproxify((ValueProxy) value);
        }
        else
        {
            return value;
        }
    }

    /**
     * Gets the original value by iterating through all {@link ValueProxy ValueProxies}.
     *
     * @param value the start value proxy.
     * @return the first non-proxy value encountered
     */
    public static ValueNode unproxify(ValueProxy value)
    {
        if (value != null)
        {
            ValueNode result = value.getOriginalNode();
            while (result instanceof ValueProxy)
            {
                result = ((ValueProxy) result).getOriginalNode();
            }
            return result;
        }
        else
        {
            return null;
        }
    }

    public static ValueNode skipPi(ValueNode node)
    {
        ValueNode n = node;
        while (n instanceof PiNode)
        {
            PiNode piNode = (PiNode) n;
            n = piNode.getOriginalNode();
        }
        return n;
    }

    public static ValueNode skipPiWhileNonNull(ValueNode node)
    {
        ValueNode n = node;
        while (n instanceof PiNode)
        {
            PiNode piNode = (PiNode) n;
            ObjectStamp originalStamp = (ObjectStamp) piNode.getOriginalNode().stamp(NodeView.DEFAULT);
            if (originalStamp.nonNull())
            {
                n = piNode.getOriginalNode();
            }
            else
            {
                break;
            }
        }
        return n;
    }

    /**
     * Looks for an {@link ArrayLengthProvider} while iterating through all {@link ValueProxy
     * ValueProxies}.
     *
     * @param value The start value.
     * @return The array length if one was found, or null otherwise.
     */
    public static ValueNode arrayLength(ValueNode value)
    {
        ValueNode current = value;
        do
        {
            if (current instanceof ArrayLengthProvider)
            {
                ValueNode length = ((ArrayLengthProvider) current).length();
                if (length != null)
                {
                    return length;
                }
            }
            if (current instanceof ValueProxy)
            {
                current = ((ValueProxy) current).getOriginalNode();
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
    public static ValueNode originalValue(ValueNode value)
    {
        ValueNode result = originalValueSimple(value);
        return result;
    }

    private static ValueNode originalValueSimple(ValueNode value)
    {
        /* The very simple case: look through proxies. */
        ValueNode cur = originalValueForProxy(value);

        while (cur instanceof PhiNode)
        {
            /*
             * We found a phi function. Check if we can analyze it without allocating temporary data
             * structures.
             */
            PhiNode phi = (PhiNode) cur;

            ValueNode phiSingleValue = null;
            int count = phi.valueCount();
            for (int i = 0; i < count; ++i)
            {
                ValueNode phiCurValue = originalValueForProxy(phi.valueAt(i));
                if (phiCurValue == phi)
                {
                    /* Simple cycle, we can ignore the input value. */
                }
                else if (phiSingleValue == null)
                {
                    /* The first input. */
                    phiSingleValue = phiCurValue;
                }
                else if (phiSingleValue != phiCurValue)
                {
                    /* Another input that is different from the first input. */

                    if (phiSingleValue instanceof PhiNode || phiCurValue instanceof PhiNode)
                    {
                        /*
                         * We have two different input values for the phi function, and at least one
                         * of the inputs is another phi function. We need to do a complicated
                         * exhaustive check.
                         */
                        return originalValueForComplicatedPhi(phi, new NodeBitMap(value.graph()));
                    }
                    else
                    {
                        /*
                         * We have two different input values for the phi function, but none of them
                         * is another phi function. This phi function cannot be reduce any further,
                         * so the phi function is the original value.
                         */
                        return phi;
                    }
                }
            }

            /*
             * Successfully reduced the phi function to a single input value. The single input value
             * can itself be a phi function again, so we might take another loop iteration.
             */
            cur = phiSingleValue;
        }

        /* We reached a "normal" node, which is the original value. */
        return cur;
    }

    private static ValueNode originalValueForProxy(ValueNode value)
    {
        ValueNode cur = value;
        while (cur instanceof LimitedValueProxy)
        {
            cur = ((LimitedValueProxy) cur).getOriginalNode();
        }
        return cur;
    }

    /**
     * Handling for complicated nestings of phi functions. We need to reduce phi functions
     * recursively, and need a temporary map of visited nodes to avoid endless recursion of cycles.
     */
    private static ValueNode originalValueForComplicatedPhi(PhiNode phi, NodeBitMap visited)
    {
        if (visited.isMarked(phi))
        {
            /*
             * Found a phi function that was already seen. Either a cycle, or just a second phi
             * input to a path we have already processed.
             */
            return null;
        }
        visited.mark(phi);

        ValueNode phiSingleValue = null;
        int count = phi.valueCount();
        for (int i = 0; i < count; ++i)
        {
            ValueNode phiCurValue = originalValueForProxy(phi.valueAt(i));
            if (phiCurValue instanceof PhiNode)
            {
                /* Recursively process a phi function input. */
                phiCurValue = originalValueForComplicatedPhi((PhiNode) phiCurValue, visited);
            }

            if (phiCurValue == null)
            {
                /* Cycle to a phi function that was already seen. We can ignore this input. */
            }
            else if (phiSingleValue == null)
            {
                /* The first input. */
                phiSingleValue = phiCurValue;
            }
            else if (phiCurValue != phiSingleValue)
            {
                /*
                 * Another input that is different from the first input. Since we already
                 * recursively looked through other phi functions, we now know that this phi
                 * function cannot be reduce any further, so the phi function is the original value.
                 */
                return phi;
            }
        }
        return phiSingleValue;
    }

    public static boolean tryKillUnused(Node node)
    {
        if (node.isAlive() && isFloatingNode(node) && node.hasNoUsages() && !(node instanceof GuardNode))
        {
            killWithUnusedFloatingInputs(node);
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
    public static NodeIterable<FixedNode> predecessorIterable(final FixedNode start)
    {
        return new NodeIterable<FixedNode>()
        {
            @Override
            public Iterator<FixedNode> iterator()
            {
                return new Iterator<FixedNode>()
                {
                    public FixedNode current = start;

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

    private static final class DefaultSimplifierTool implements SimplifierTool
    {
        private final MetaAccessProvider metaAccess;
        private final ConstantReflectionProvider constantReflection;
        private final ConstantFieldProvider constantFieldProvider;
        private final boolean canonicalizeReads;
        private final Assumptions assumptions;
        private final OptionValues options;
        private final LoweringProvider loweringProvider;

        DefaultSimplifierTool(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, boolean canonicalizeReads, Assumptions assumptions, OptionValues options, LoweringProvider loweringProvider)
        {
            this.metaAccess = metaAccess;
            this.constantReflection = constantReflection;
            this.constantFieldProvider = constantFieldProvider;
            this.canonicalizeReads = canonicalizeReads;
            this.assumptions = assumptions;
            this.options = options;
            this.loweringProvider = loweringProvider;
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
        public void deleteBranch(Node branch)
        {
            FixedNode fixedBranch = (FixedNode) branch;
            fixedBranch.predecessor().replaceFirstSuccessor(fixedBranch, null);
            GraphUtil.killCFG(fixedBranch);
        }

        @Override
        public void removeIfUnused(Node node)
        {
            GraphUtil.tryKillUnused(node);
        }

        @Override
        public void addToWorkList(Node node)
        {
        }

        @Override
        public void addToWorkList(Iterable<? extends Node> nodes)
        {
        }

        @Override
        public Assumptions getAssumptions()
        {
            return assumptions;
        }

        @Override
        public OptionValues getOptions()
        {
            return options;
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

    public static SimplifierTool getDefaultSimplifier(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, boolean canonicalizeReads, Assumptions assumptions, OptionValues options)
    {
        return getDefaultSimplifier(metaAccess, constantReflection, constantFieldProvider, canonicalizeReads, assumptions, options, null);
    }

    public static SimplifierTool getDefaultSimplifier(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, boolean canonicalizeReads, Assumptions assumptions, OptionValues options, LoweringProvider loweringProvider)
    {
        return new DefaultSimplifierTool(metaAccess, constantReflection, constantFieldProvider, canonicalizeReads, assumptions, options, loweringProvider);
    }

    public static Constant foldIfConstantAndRemove(ValueNode node, ValueNode constant)
    {
        if (constant.isConstant())
        {
            node.replaceFirstInput(constant, null);
            Constant result = constant.asConstant();
            tryKillUnused(constant);
            return result;
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
    public static void virtualizeArrayCopy(VirtualizerTool tool, ValueNode source, ValueNode sourceLength, ValueNode newLength, ValueNode from, ResolvedJavaType newComponentType, JavaKind elementKind, StructuredGraph graph, BiFunction<ResolvedJavaType, Integer, VirtualArrayNode> virtualArrayProvider)
    {
        ValueNode sourceAlias = tool.getAlias(source);
        ValueNode replacedSourceLength = tool.getAlias(sourceLength);
        ValueNode replacedNewLength = tool.getAlias(newLength);
        ValueNode replacedFrom = tool.getAlias(from);
        if (!replacedNewLength.isConstant() || !replacedFrom.isConstant() || !replacedSourceLength.isConstant())
        {
            return;
        }

        int fromInt = replacedFrom.asJavaConstant().asInt();
        int newLengthInt = replacedNewLength.asJavaConstant().asInt();
        int sourceLengthInt = replacedSourceLength.asJavaConstant().asInt();
        if (sourceAlias instanceof VirtualObjectNode)
        {
            VirtualObjectNode sourceVirtual = (VirtualObjectNode) sourceAlias;
        }

        if (fromInt < 0 || newLengthInt < 0 || fromInt > sourceLengthInt)
        {
            /* Illegal values for either from index, the new length or the source length. */
            return;
        }

        if (newLengthInt >= tool.getMaximumEntryCount())
        {
            /* The new array size is higher than maximum allowed size of virtualized objects. */
            return;
        }

        ValueNode[] newEntryState = new ValueNode[newLengthInt];
        int readLength = Math.min(newLengthInt, sourceLengthInt - fromInt);

        if (sourceAlias instanceof VirtualObjectNode)
        {
            /* The source array is virtualized, just copy over the values. */
            VirtualObjectNode sourceVirtual = (VirtualObjectNode) sourceAlias;
            for (int i = 0; i < readLength; i++)
            {
                newEntryState[i] = tool.getEntry(sourceVirtual, fromInt + i);
            }
        }
        else
        {
            /* The source array is not virtualized, emit index loads. */
            for (int i = 0; i < readLength; i++)
            {
                LoadIndexedNode load = new LoadIndexedNode(null, sourceAlias, ConstantNode.forInt(i + fromInt, graph), elementKind);
                tool.addNode(load);
                newEntryState[i] = load;
            }
        }
        if (readLength < newLengthInt)
        {
            /* Pad the copy with the default value of its elment kind. */
            ValueNode defaultValue = ConstantNode.defaultForKind(elementKind, graph);
            for (int i = readLength; i < newLengthInt; i++)
            {
                newEntryState[i] = defaultValue;
            }
        }
        /* Perform the replacement. */
        VirtualArrayNode newVirtualArray = virtualArrayProvider.apply(newComponentType, newLengthInt);
        tool.createVirtualObject(newVirtualArray, newEntryState, Collections.<MonitorIdNode> emptyList(), false);
        tool.replaceWithVirtual(newVirtualArray);
    }
}
