package giraaff.loop;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Graph.DuplicationReplacement;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.GuardPhiNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.VirtualState.NodeClosure;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.memory.MemoryPhiNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

public class LoopFragmentInside extends LoopFragment
{
    /**
     * mergedInitializers. When an inside fragment's (loop)ends are merged to create a unique exit
     * point, some phis must be created : they phis together all the back-values of the loop-phis
     * These can then be used to update the loop-phis' forward edge value ('initializer') in the
     * peeling case. In the unrolling case they will be used as the value that replace the loop-phis
     * of the duplicated inside fragment
     */
    private EconomicMap<PhiNode, ValueNode> mergedInitializers;
    private final DuplicationReplacement dataFixBefore = new DuplicationReplacement()
    {
        @Override
        public Node replacement(Node oriInput)
        {
            if (!(oriInput instanceof ValueNode))
            {
                return oriInput;
            }
            return prim((ValueNode) oriInput);
        }
    };

    private final DuplicationReplacement dataFixWithinAfter = new DuplicationReplacement()
    {
        @Override
        public Node replacement(Node oriInput)
        {
            if (!(oriInput instanceof ValueNode))
            {
                return oriInput;
            }
            return primAfter((ValueNode) oriInput);
        }
    };

    public LoopFragmentInside(LoopEx loop)
    {
        super(loop);
    }

    public LoopFragmentInside(LoopFragmentInside original)
    {
        super(null, original);
    }

    @Override
    public LoopFragmentInside duplicate()
    {
        return new LoopFragmentInside(this);
    }

    @Override
    public LoopFragmentInside original()
    {
        return (LoopFragmentInside) super.original();
    }

    @SuppressWarnings("unused")
    public void appendInside(LoopEx loop)
    {
        // TODO (gd)
    }

    @Override
    public LoopEx loop()
    {
        return super.loop();
    }

    @Override
    public void insertBefore(LoopEx loop)
    {
        patchNodes(dataFixBefore);

        AbstractBeginNode end = mergeEnds();

        mergeEarlyExits();

        original().patchPeeling(this);

        AbstractBeginNode entry = getDuplicatedNode(loop.loopBegin());
        loop.entryPoint().replaceAtPredecessor(entry);
        end.setNext(loop.entryPoint());
    }

    /**
     * Duplicate the body within the loop after the current copy copy of the body, updating the
     * iteration limit to account for the duplication.
     */
    public void insertWithinAfter(LoopEx loop)
    {
        insertWithinAfter(loop, true);
    }

    /**
     * Duplicate the body within the loop after the current copy copy of the body.
     *
     * @param updateLimit true if the iteration limit should be adjusted.
     */
    public void insertWithinAfter(LoopEx loop, boolean updateLimit)
    {
        patchNodes(dataFixWithinAfter);

        // Collect any new back edges values before updating them since they might reference each other.
        LoopBeginNode mainLoopBegin = loop.loopBegin();
        ArrayList<ValueNode> backedgeValues = new ArrayList<>();
        for (PhiNode mainPhiNode : mainLoopBegin.phis())
        {
            ValueNode duplicatedNode = getDuplicatedNode(mainPhiNode.valueAt(1));
            if (duplicatedNode == null)
            {
                if (mainLoopBegin.isPhiAtMerge(mainPhiNode.valueAt(1)))
                {
                    duplicatedNode = ((PhiNode) (mainPhiNode.valueAt(1))).valueAt(1);
                }
            }
            backedgeValues.add(duplicatedNode);
        }
        int index = 0;
        for (PhiNode mainPhiNode : mainLoopBegin.phis())
        {
            ValueNode duplicatedNode = backedgeValues.get(index++);
            if (duplicatedNode != null)
            {
                mainPhiNode.setValueAt(1, duplicatedNode);
            }
        }

        placeNewSegmentAndCleanup(loop);

        // Remove any safepoints from the original copy leaving only the duplicated one
        for (SafepointNode safepoint : loop.whole().nodes().filter(SafepointNode.class))
        {
            graph().removeFixed(safepoint);
        }

        int unrollFactor = mainLoopBegin.getUnrollFactor();
        StructuredGraph graph = mainLoopBegin.graph();
        if (updateLimit)
        {
            // Now use the previous unrollFactor to update the exit condition to power of two
            InductionVariable iv = loop.counted().getCounter();
            CompareNode compareNode = (CompareNode) loop.counted().getLimitTest().condition();
            ValueNode compareBound;
            if (compareNode.getX() == iv.valueNode())
            {
                compareBound = compareNode.getY();
            }
            else if (compareNode.getY() == iv.valueNode())
            {
                compareBound = compareNode.getX();
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            long originalStride = unrollFactor == 1 ? iv.constantStride() : iv.constantStride() / unrollFactor;
            if (iv.direction() == InductionVariable.Direction.Up)
            {
                ConstantNode aboveVal = graph.unique(ConstantNode.forIntegerStamp(iv.initNode().stamp(NodeView.DEFAULT), unrollFactor * originalStride));
                ValueNode newLimit = graph.addWithoutUnique(new SubNode(compareBound, aboveVal));
                compareNode.replaceFirstInput(compareBound, newLimit);
            }
            else if (iv.direction() == InductionVariable.Direction.Down)
            {
                ConstantNode aboveVal = graph.unique(ConstantNode.forIntegerStamp(iv.initNode().stamp(NodeView.DEFAULT), unrollFactor * -originalStride));
                ValueNode newLimit = graph.addWithoutUnique(new AddNode(compareBound, aboveVal));
                compareNode.replaceFirstInput(compareBound, newLimit);
            }
        }
        mainLoopBegin.setUnrollFactor(unrollFactor * 2);
        mainLoopBegin.setLoopFrequency(mainLoopBegin.loopFrequency() / 2);
    }

    private void placeNewSegmentAndCleanup(LoopEx loop)
    {
        CountedLoopInfo mainCounted = loop.counted();
        LoopBeginNode mainLoopBegin = loop.loopBegin();
        // Discard the segment entry and its flow, after if merging it into the loop
        StructuredGraph graph = mainLoopBegin.graph();
        IfNode loopTest = mainCounted.getLimitTest();
        IfNode newSegmentTest = getDuplicatedNode(loopTest);
        AbstractBeginNode trueSuccessor = loopTest.trueSuccessor();
        AbstractBeginNode falseSuccessor = loopTest.falseSuccessor();
        FixedNode firstNode;
        boolean codeInTrueSide = false;
        if (trueSuccessor == mainCounted.getBody())
        {
            firstNode = trueSuccessor.next();
            codeInTrueSide = true;
        }
        else
        {
            firstNode = falseSuccessor.next();
        }
        trueSuccessor = newSegmentTest.trueSuccessor();
        falseSuccessor = newSegmentTest.falseSuccessor();
        for (Node usage : falseSuccessor.anchored().snapshot())
        {
            usage.replaceFirstInput(falseSuccessor, loopTest.falseSuccessor());
        }
        for (Node usage : trueSuccessor.anchored().snapshot())
        {
            usage.replaceFirstInput(trueSuccessor, loopTest.trueSuccessor());
        }
        AbstractBeginNode startBlockNode;
        if (codeInTrueSide)
        {
            startBlockNode = trueSuccessor;
        }
        else
        {
            startBlockNode = falseSuccessor;
        }
        FixedNode lastNode = getBlockEnd(startBlockNode);
        LoopEndNode loopEndNode = mainLoopBegin.getSingleLoopEnd();
        FixedWithNextNode lastCodeNode = (FixedWithNextNode) loopEndNode.predecessor();
        FixedNode newSegmentFirstNode = getDuplicatedNode(firstNode);
        FixedWithNextNode newSegmentLastNode = getDuplicatedNode(lastCodeNode);
        if (firstNode instanceof LoopEndNode)
        {
            GraphUtil.killCFG(getDuplicatedNode(mainLoopBegin));
        }
        else
        {
            newSegmentLastNode.clearSuccessors();
            startBlockNode.setNext(lastNode);
            lastCodeNode.replaceFirstSuccessor(loopEndNode, newSegmentFirstNode);
            newSegmentLastNode.replaceFirstSuccessor(lastNode, loopEndNode);
            lastCodeNode.setNext(newSegmentFirstNode);
            newSegmentLastNode.setNext(loopEndNode);
            startBlockNode.clearSuccessors();
            lastNode.safeDelete();
            Node newSegmentTestStart = newSegmentTest.predecessor();
            LogicNode newSegmentIfTest = newSegmentTest.condition();
            newSegmentTestStart.clearSuccessors();
            newSegmentTest.safeDelete();
            newSegmentIfTest.safeDelete();
            trueSuccessor.safeDelete();
            falseSuccessor.safeDelete();
            newSegmentTestStart.safeDelete();
        }
    }

    private static EndNode getBlockEnd(FixedNode node)
    {
        FixedNode curNode = node;
        while (curNode instanceof FixedWithNextNode)
        {
            curNode = ((FixedWithNextNode) curNode).next();
        }
        return (EndNode) curNode;
    }

    @Override
    public NodeBitMap nodes()
    {
        if (nodes == null)
        {
            LoopFragmentWhole whole = loop().whole();
            whole.nodes(); // init nodes bitmap in whole
            nodes = whole.nodes.copy();
            // remove the phis
            LoopBeginNode loopBegin = loop().loopBegin();
            for (PhiNode phi : loopBegin.phis())
            {
                nodes.clear(phi);
            }
            clearStateNodes(loopBegin);
            for (LoopExitNode exit : exits())
            {
                clearStateNodes(exit);
                for (ProxyNode proxy : exit.proxies())
                {
                    nodes.clear(proxy);
                }
            }
        }
        return nodes;
    }

    private void clearStateNodes(StateSplit stateSplit)
    {
        FrameState loopState = stateSplit.stateAfter();
        if (loopState != null)
        {
            loopState.applyToVirtual(v ->
            {
                if (v.usages().filter(n -> nodes.isMarked(n) && n != stateSplit).isEmpty())
                {
                    nodes.clear(v);
                }
            });
        }
    }

    public NodeIterable<LoopExitNode> exits()
    {
        return loop().loopBegin().loopExits();
    }

    @Override
    protected DuplicationReplacement getDuplicationReplacement()
    {
        final LoopBeginNode loopBegin = loop().loopBegin();
        final StructuredGraph graph = graph();
        return new DuplicationReplacement()
        {
            private EconomicMap<Node, Node> seenNode = EconomicMap.create(Equivalence.IDENTITY);

            @Override
            public Node replacement(Node original)
            {
                if (original == loopBegin)
                {
                    Node value = seenNode.get(original);
                    if (value != null)
                    {
                        return value;
                    }
                    AbstractBeginNode newValue = graph.add(new BeginNode());
                    seenNode.put(original, newValue);
                    return newValue;
                }
                if (original instanceof LoopExitNode && ((LoopExitNode) original).loopBegin() == loopBegin)
                {
                    Node value = seenNode.get(original);
                    if (value != null)
                    {
                        return value;
                    }
                    AbstractBeginNode newValue = graph.add(new BeginNode());
                    seenNode.put(original, newValue);
                    return newValue;
                }
                if (original instanceof LoopEndNode && ((LoopEndNode) original).loopBegin() == loopBegin)
                {
                    Node value = seenNode.get(original);
                    if (value != null)
                    {
                        return value;
                    }
                    EndNode newValue = graph.add(new EndNode());
                    seenNode.put(original, newValue);
                    return newValue;
                }
                return original;
            }
        };
    }

    @Override
    protected void finishDuplication()
    {
        // TODO (gd) ?
    }

    @Override
    protected void beforeDuplication()
    {
        // Nothing to do
    }

    private static PhiNode patchPhi(StructuredGraph graph, PhiNode phi, AbstractMergeNode merge)
    {
        PhiNode ret;
        if (phi instanceof ValuePhiNode)
        {
            ret = new ValuePhiNode(phi.stamp(NodeView.DEFAULT), merge);
        }
        else if (phi instanceof GuardPhiNode)
        {
            ret = new GuardPhiNode(merge);
        }
        else if (phi instanceof MemoryPhiNode)
        {
            ret = new MemoryPhiNode(merge, ((MemoryPhiNode) phi).getLocationIdentity());
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
        return graph.addWithoutUnique(ret);
    }

    private void patchPeeling(LoopFragmentInside peel)
    {
        LoopBeginNode loopBegin = loop().loopBegin();
        StructuredGraph graph = loopBegin.graph();
        List<PhiNode> newPhis = new LinkedList<>();

        NodeBitMap usagesToPatch = nodes.copy();
        for (LoopExitNode exit : exits())
        {
            markStateNodes(exit, usagesToPatch);
            for (ProxyNode proxy : exit.proxies())
            {
                usagesToPatch.markAndGrow(proxy);
            }
        }
        markStateNodes(loopBegin, usagesToPatch);

        List<PhiNode> oldPhis = loopBegin.phis().snapshot();
        for (PhiNode phi : oldPhis)
        {
            if (phi.hasNoUsages())
            {
                continue;
            }
            ValueNode first;
            if (loopBegin.loopEnds().count() == 1)
            {
                ValueNode b = phi.valueAt(loopBegin.loopEnds().first()); // back edge value
                first = peel.prim(b); // corresponding value in the peel
            }
            else
            {
                first = peel.mergedInitializers.get(phi);
            }
            // create a new phi (we don't patch the old one since some usages of the old one may
            // still be valid)
            PhiNode newPhi = patchPhi(graph, phi, loopBegin);
            newPhi.addInput(first);
            for (LoopEndNode end : loopBegin.orderedLoopEnds())
            {
                newPhi.addInput(phi.valueAt(end));
            }
            peel.putDuplicatedNode(phi, newPhi);
            newPhis.add(newPhi);
            for (Node usage : phi.usages().snapshot())
            {
                // patch only usages that should use the new phi ie usages that were peeled
                if (usagesToPatch.isMarkedAndGrow(usage))
                {
                    usage.replaceFirstInput(phi, newPhi);
                }
            }
        }
        // check new phis to see if they have as input some old phis, replace those inputs with the
        // new corresponding phis
        for (PhiNode phi : newPhis)
        {
            for (int i = 0; i < phi.valueCount(); i++)
            {
                ValueNode v = phi.valueAt(i);
                if (loopBegin.isPhiAtMerge(v))
                {
                    PhiNode newV = peel.getDuplicatedNode((ValuePhiNode) v);
                    if (newV != null)
                    {
                        phi.setValueAt(i, newV);
                    }
                }
            }
        }

        boolean progress = true;
        while (progress)
        {
            progress = false;
            int i = 0;
            outer: while (i < oldPhis.size())
            {
                PhiNode oldPhi = oldPhis.get(i);
                for (Node usage : oldPhi.usages())
                {
                    if (usage instanceof PhiNode && oldPhis.contains(usage))
                    {
                        // Do not mark.
                    }
                    else
                    {
                        // Mark alive by removing from delete set.
                        oldPhis.remove(i);
                        progress = true;
                        continue outer;
                    }
                }
                i++;
            }
        }

        for (PhiNode deadPhi : oldPhis)
        {
            deadPhi.clearInputs();
        }

        for (PhiNode deadPhi : oldPhis)
        {
            if (deadPhi.isAlive())
            {
                GraphUtil.killWithUnusedFloatingInputs(deadPhi);
            }
        }
    }

    private static void markStateNodes(StateSplit stateSplit, NodeBitMap marks)
    {
        FrameState exitState = stateSplit.stateAfter();
        if (exitState != null)
        {
            exitState.applyToVirtual(v -> marks.markAndGrow(v));
        }
    }

    /**
     * Gets the corresponding value in this fragment.
     *
     * @param b original value
     * @return corresponding value in the peel
     */
    @Override
    protected ValueNode prim(ValueNode b)
    {
        LoopBeginNode loopBegin = original().loop().loopBegin();
        if (loopBegin.isPhiAtMerge(b))
        {
            PhiNode phi = (PhiNode) b;
            return phi.valueAt(loopBegin.forwardEnd());
        }
        else if (nodesReady)
        {
            ValueNode v = getDuplicatedNode(b);
            if (v == null)
            {
                return b;
            }
            return v;
        }
        else
        {
            return b;
        }
    }

    protected ValueNode primAfter(ValueNode b)
    {
        LoopBeginNode loopBegin = original().loop().loopBegin();
        if (loopBegin.isPhiAtMerge(b))
        {
            PhiNode phi = (PhiNode) b;
            return phi.valueAt(1);
        }
        else if (nodesReady)
        {
            ValueNode v = getDuplicatedNode(b);
            if (v == null)
            {
                return b;
            }
            return v;
        }
        else
        {
            return b;
        }
    }

    private AbstractBeginNode mergeEnds()
    {
        List<EndNode> endsToMerge = new LinkedList<>();
        // map peel exits to the corresponding loop exits
        EconomicMap<AbstractEndNode, LoopEndNode> reverseEnds = EconomicMap.create(Equivalence.IDENTITY);
        LoopBeginNode loopBegin = original().loop().loopBegin();
        for (LoopEndNode le : loopBegin.loopEnds())
        {
            AbstractEndNode duplicate = getDuplicatedNode(le);
            if (duplicate != null)
            {
                endsToMerge.add((EndNode) duplicate);
                reverseEnds.put(duplicate, le);
            }
        }
        mergedInitializers = EconomicMap.create(Equivalence.IDENTITY);
        AbstractBeginNode newExit;
        StructuredGraph graph = graph();
        if (endsToMerge.size() == 1)
        {
            AbstractEndNode end = endsToMerge.get(0);
            newExit = graph.add(new BeginNode());
            end.replaceAtPredecessor(newExit);
            end.safeDelete();
        }
        else
        {
            AbstractMergeNode newExitMerge = graph.add(new MergeNode());
            newExit = newExitMerge;
            FrameState state = loopBegin.stateAfter();
            FrameState duplicateState = null;
            if (state != null)
            {
                duplicateState = state.duplicateWithVirtualState();
                newExitMerge.setStateAfter(duplicateState);
            }
            for (EndNode end : endsToMerge)
            {
                newExitMerge.addForwardEnd(end);
            }

            for (final PhiNode phi : loopBegin.phis().snapshot())
            {
                if (phi.hasNoUsages())
                {
                    continue;
                }
                final PhiNode firstPhi = patchPhi(graph, phi, newExitMerge);
                for (AbstractEndNode end : newExitMerge.forwardEnds())
                {
                    LoopEndNode loopEnd = reverseEnds.get(end);
                    ValueNode prim = prim(phi.valueAt(loopEnd));
                    firstPhi.addInput(prim);
                }
                ValueNode initializer = firstPhi;
                if (duplicateState != null)
                {
                    // fix the merge's state after
                    duplicateState.applyToNonVirtual(new NodeClosure<ValueNode>()
                    {
                        @Override
                        public void apply(Node from, ValueNode node)
                        {
                            if (node == phi)
                            {
                                from.replaceFirstInput(phi, firstPhi);
                            }
                        }
                    });
                }
                mergedInitializers.put(phi, initializer);
            }
        }
        return newExit;
    }
}