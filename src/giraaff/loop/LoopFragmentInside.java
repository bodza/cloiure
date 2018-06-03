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

// @class LoopFragmentInside
public class LoopFragmentInside extends LoopFragment
{
    ///
    // mergedInitializers. When an inside fragment's (loop)ends are merged to create a unique exit
    // point, some phis must be created : they phis together all the back-values of the loop-phis
    // These can then be used to update the loop-phis' forward edge value ('initializer') in the
    // peeling case. In the unrolling case they will be used as the value that replace the loop-phis
    // of the duplicated inside fragment
    ///
    // @field
    private EconomicMap<PhiNode, ValueNode> ___mergedInitializers;
    // @closure
    private final DuplicationReplacement dataFixBefore = new DuplicationReplacement()
    {
        @Override
        public Node replacement(Node __oriInput)
        {
            if (!(__oriInput instanceof ValueNode))
            {
                return __oriInput;
            }
            return prim((ValueNode) __oriInput);
        }
    };

    // @closure
    private final DuplicationReplacement dataFixWithinAfter = new DuplicationReplacement()
    {
        @Override
        public Node replacement(Node __oriInput)
        {
            if (!(__oriInput instanceof ValueNode))
            {
                return __oriInput;
            }
            return primAfter((ValueNode) __oriInput);
        }
    };

    // @cons
    public LoopFragmentInside(LoopEx __loop)
    {
        super(__loop);
    }

    // @cons
    public LoopFragmentInside(LoopFragmentInside __original)
    {
        super(null, __original);
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
    public void appendInside(LoopEx __loop)
    {
        // TODO
    }

    @Override
    public LoopEx loop()
    {
        return super.loop();
    }

    @Override
    public void insertBefore(LoopEx __loop)
    {
        patchNodes(dataFixBefore);

        AbstractBeginNode __end = mergeEnds();

        mergeEarlyExits();

        original().patchPeeling(this);

        AbstractBeginNode __entry = getDuplicatedNode(__loop.loopBegin());
        __loop.entryPoint().replaceAtPredecessor(__entry);
        __end.setNext(__loop.entryPoint());
    }

    ///
    // Duplicate the body within the loop after the current copy copy of the body, updating the
    // iteration limit to account for the duplication.
    ///
    public void insertWithinAfter(LoopEx __loop)
    {
        insertWithinAfter(__loop, true);
    }

    ///
    // Duplicate the body within the loop after the current copy copy of the body.
    //
    // @param updateLimit true if the iteration limit should be adjusted.
    ///
    public void insertWithinAfter(LoopEx __loop, boolean __updateLimit)
    {
        patchNodes(dataFixWithinAfter);

        // Collect any new back edges values before updating them since they might reference each other.
        LoopBeginNode __mainLoopBegin = __loop.loopBegin();
        ArrayList<ValueNode> __backedgeValues = new ArrayList<>();
        for (PhiNode __mainPhiNode : __mainLoopBegin.phis())
        {
            ValueNode __duplicatedNode = getDuplicatedNode(__mainPhiNode.valueAt(1));
            if (__duplicatedNode == null)
            {
                if (__mainLoopBegin.isPhiAtMerge(__mainPhiNode.valueAt(1)))
                {
                    __duplicatedNode = ((PhiNode) (__mainPhiNode.valueAt(1))).valueAt(1);
                }
            }
            __backedgeValues.add(__duplicatedNode);
        }
        int __index = 0;
        for (PhiNode __mainPhiNode : __mainLoopBegin.phis())
        {
            ValueNode __duplicatedNode = __backedgeValues.get(__index++);
            if (__duplicatedNode != null)
            {
                __mainPhiNode.setValueAt(1, __duplicatedNode);
            }
        }

        placeNewSegmentAndCleanup(__loop);

        // remove any safepoints from the original copy leaving only the duplicated one
        for (SafepointNode __safepoint : __loop.whole().nodes().filter(SafepointNode.class))
        {
            graph().removeFixed(__safepoint);
        }

        int __unrollFactor = __mainLoopBegin.getUnrollFactor();
        StructuredGraph __graph = __mainLoopBegin.graph();
        if (__updateLimit)
        {
            // use the previous unrollFactor to update the exit condition to power of two
            InductionVariable __iv = __loop.counted().getCounter();
            CompareNode __compareNode = (CompareNode) __loop.counted().getLimitTest().condition();
            ValueNode __compareBound;
            if (__compareNode.getX() == __iv.valueNode())
            {
                __compareBound = __compareNode.getY();
            }
            else if (__compareNode.getY() == __iv.valueNode())
            {
                __compareBound = __compareNode.getX();
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            long __originalStride = __unrollFactor == 1 ? __iv.constantStride() : __iv.constantStride() / __unrollFactor;
            if (__iv.direction() == InductionVariable.Direction.Up)
            {
                ConstantNode __aboveVal = __graph.unique(ConstantNode.forIntegerStamp(__iv.initNode().stamp(NodeView.DEFAULT), __unrollFactor * __originalStride));
                ValueNode __newLimit = __graph.addWithoutUnique(new SubNode(__compareBound, __aboveVal));
                __compareNode.replaceFirstInput(__compareBound, __newLimit);
            }
            else if (__iv.direction() == InductionVariable.Direction.Down)
            {
                ConstantNode __aboveVal = __graph.unique(ConstantNode.forIntegerStamp(__iv.initNode().stamp(NodeView.DEFAULT), __unrollFactor * -__originalStride));
                ValueNode __newLimit = __graph.addWithoutUnique(new AddNode(__compareBound, __aboveVal));
                __compareNode.replaceFirstInput(__compareBound, __newLimit);
            }
        }
        __mainLoopBegin.setUnrollFactor(__unrollFactor * 2);
        __mainLoopBegin.setLoopFrequency(__mainLoopBegin.loopFrequency() / 2);
    }

    private void placeNewSegmentAndCleanup(LoopEx __loop)
    {
        CountedLoopInfo __mainCounted = __loop.counted();
        LoopBeginNode __mainLoopBegin = __loop.loopBegin();
        // discard the segment entry and its flow after if merging it into the loop
        StructuredGraph __graph = __mainLoopBegin.graph();
        IfNode __loopTest = __mainCounted.getLimitTest();
        IfNode __newSegmentTest = getDuplicatedNode(__loopTest);
        AbstractBeginNode __trueSuccessor = __loopTest.trueSuccessor();
        AbstractBeginNode __falseSuccessor = __loopTest.falseSuccessor();
        FixedNode __firstNode;
        boolean __codeInTrueSide = false;
        if (__trueSuccessor == __mainCounted.getBody())
        {
            __firstNode = __trueSuccessor.next();
            __codeInTrueSide = true;
        }
        else
        {
            __firstNode = __falseSuccessor.next();
        }
        __trueSuccessor = __newSegmentTest.trueSuccessor();
        __falseSuccessor = __newSegmentTest.falseSuccessor();
        for (Node __usage : __falseSuccessor.anchored().snapshot())
        {
            __usage.replaceFirstInput(__falseSuccessor, __loopTest.falseSuccessor());
        }
        for (Node __usage : __trueSuccessor.anchored().snapshot())
        {
            __usage.replaceFirstInput(__trueSuccessor, __loopTest.trueSuccessor());
        }
        AbstractBeginNode __startBlockNode;
        if (__codeInTrueSide)
        {
            __startBlockNode = __trueSuccessor;
        }
        else
        {
            __startBlockNode = __falseSuccessor;
        }
        FixedNode __lastNode = getBlockEnd(__startBlockNode);
        LoopEndNode __loopEndNode = __mainLoopBegin.getSingleLoopEnd();
        FixedWithNextNode __lastCodeNode = (FixedWithNextNode) __loopEndNode.predecessor();
        FixedNode __newSegmentFirstNode = getDuplicatedNode(__firstNode);
        FixedWithNextNode __newSegmentLastNode = getDuplicatedNode(__lastCodeNode);
        if (__firstNode instanceof LoopEndNode)
        {
            GraphUtil.killCFG(getDuplicatedNode(__mainLoopBegin));
        }
        else
        {
            __newSegmentLastNode.clearSuccessors();
            __startBlockNode.setNext(__lastNode);
            __lastCodeNode.replaceFirstSuccessor(__loopEndNode, __newSegmentFirstNode);
            __newSegmentLastNode.replaceFirstSuccessor(__lastNode, __loopEndNode);
            __lastCodeNode.setNext(__newSegmentFirstNode);
            __newSegmentLastNode.setNext(__loopEndNode);
            __startBlockNode.clearSuccessors();
            __lastNode.safeDelete();
            Node __newSegmentTestStart = __newSegmentTest.predecessor();
            LogicNode __newSegmentIfTest = __newSegmentTest.condition();
            __newSegmentTestStart.clearSuccessors();
            __newSegmentTest.safeDelete();
            __newSegmentIfTest.safeDelete();
            __trueSuccessor.safeDelete();
            __falseSuccessor.safeDelete();
            __newSegmentTestStart.safeDelete();
        }
    }

    private static EndNode getBlockEnd(FixedNode __node)
    {
        FixedNode __curNode = __node;
        while (__curNode instanceof FixedWithNextNode)
        {
            __curNode = ((FixedWithNextNode) __curNode).next();
        }
        return (EndNode) __curNode;
    }

    @Override
    public NodeBitMap nodes()
    {
        if (this.___nodes == null)
        {
            LoopFragmentWhole __whole = loop().whole();
            __whole.nodes(); // init nodes bitmap in whole
            this.___nodes = __whole.___nodes.copy();
            // remove the phis
            LoopBeginNode __loopBegin = loop().loopBegin();
            for (PhiNode __phi : __loopBegin.phis())
            {
                this.___nodes.clear(__phi);
            }
            clearStateNodes(__loopBegin);
            for (LoopExitNode __exit : exits())
            {
                clearStateNodes(__exit);
                for (ProxyNode __proxy : __exit.proxies())
                {
                    this.___nodes.clear(__proxy);
                }
            }
        }
        return this.___nodes;
    }

    private void clearStateNodes(StateSplit __stateSplit)
    {
        FrameState __loopState = __stateSplit.stateAfter();
        if (__loopState != null)
        {
            __loopState.applyToVirtual(__v ->
            {
                if (__v.usages().filter(__n -> this.___nodes.isMarked(__n) && __n != __stateSplit).isEmpty())
                {
                    this.___nodes.clear(__v);
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
        final LoopBeginNode __loopBegin = loop().loopBegin();
        final StructuredGraph __graph = graph();
        // @closure
        return new DuplicationReplacement()
        {
            // @field
            private EconomicMap<Node, Node> ___seenNode = EconomicMap.create(Equivalence.IDENTITY);

            @Override
            public Node replacement(Node __original)
            {
                if (__original == __loopBegin)
                {
                    Node __value = this.___seenNode.get(__original);
                    if (__value != null)
                    {
                        return __value;
                    }
                    AbstractBeginNode __newValue = __graph.add(new BeginNode());
                    this.___seenNode.put(__original, __newValue);
                    return __newValue;
                }
                if (__original instanceof LoopExitNode && ((LoopExitNode) __original).loopBegin() == __loopBegin)
                {
                    Node __value = this.___seenNode.get(__original);
                    if (__value != null)
                    {
                        return __value;
                    }
                    AbstractBeginNode __newValue = __graph.add(new BeginNode());
                    this.___seenNode.put(__original, __newValue);
                    return __newValue;
                }
                if (__original instanceof LoopEndNode && ((LoopEndNode) __original).loopBegin() == __loopBegin)
                {
                    Node __value = this.___seenNode.get(__original);
                    if (__value != null)
                    {
                        return __value;
                    }
                    EndNode __newValue = __graph.add(new EndNode());
                    this.___seenNode.put(__original, __newValue);
                    return __newValue;
                }
                return __original;
            }
        };
    }

    @Override
    protected void finishDuplication()
    {
        // TODO
    }

    @Override
    protected void beforeDuplication()
    {
        // Nothing to do
    }

    private static PhiNode patchPhi(StructuredGraph __graph, PhiNode __phi, AbstractMergeNode __merge)
    {
        PhiNode __ret;
        if (__phi instanceof ValuePhiNode)
        {
            __ret = new ValuePhiNode(__phi.stamp(NodeView.DEFAULT), __merge);
        }
        else if (__phi instanceof GuardPhiNode)
        {
            __ret = new GuardPhiNode(__merge);
        }
        else if (__phi instanceof MemoryPhiNode)
        {
            __ret = new MemoryPhiNode(__merge, ((MemoryPhiNode) __phi).getLocationIdentity());
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
        return __graph.addWithoutUnique(__ret);
    }

    private void patchPeeling(LoopFragmentInside __peel)
    {
        LoopBeginNode __loopBegin = loop().loopBegin();
        StructuredGraph __graph = __loopBegin.graph();
        List<PhiNode> __newPhis = new LinkedList<>();

        NodeBitMap __usagesToPatch = this.___nodes.copy();
        for (LoopExitNode __exit : exits())
        {
            markStateNodes(__exit, __usagesToPatch);
            for (ProxyNode __proxy : __exit.proxies())
            {
                __usagesToPatch.markAndGrow(__proxy);
            }
        }
        markStateNodes(__loopBegin, __usagesToPatch);

        List<PhiNode> __oldPhis = __loopBegin.phis().snapshot();
        for (PhiNode __phi : __oldPhis)
        {
            if (__phi.hasNoUsages())
            {
                continue;
            }
            ValueNode __first;
            if (__loopBegin.loopEnds().count() == 1)
            {
                ValueNode __b = __phi.valueAt(__loopBegin.loopEnds().first()); // back edge value
                __first = __peel.prim(__b); // corresponding value in the peel
            }
            else
            {
                __first = __peel.___mergedInitializers.get(__phi);
            }
            // create a new phi (we don't patch the old one since some usages of the old one may
            // still be valid)
            PhiNode __newPhi = patchPhi(__graph, __phi, __loopBegin);
            __newPhi.addInput(__first);
            for (LoopEndNode __end : __loopBegin.orderedLoopEnds())
            {
                __newPhi.addInput(__phi.valueAt(__end));
            }
            __peel.putDuplicatedNode(__phi, __newPhi);
            __newPhis.add(__newPhi);
            for (Node __usage : __phi.usages().snapshot())
            {
                // patch only usages that should use the new phi ie usages that were peeled
                if (__usagesToPatch.isMarkedAndGrow(__usage))
                {
                    __usage.replaceFirstInput(__phi, __newPhi);
                }
            }
        }
        // check new phis to see if they have as input some old phis, replace those inputs with the
        // new corresponding phis
        for (PhiNode __phi : __newPhis)
        {
            for (int __i = 0; __i < __phi.valueCount(); __i++)
            {
                ValueNode __v = __phi.valueAt(__i);
                if (__loopBegin.isPhiAtMerge(__v))
                {
                    PhiNode __newV = __peel.getDuplicatedNode((ValuePhiNode) __v);
                    if (__newV != null)
                    {
                        __phi.setValueAt(__i, __newV);
                    }
                }
            }
        }

        boolean __progress = true;
        while (__progress)
        {
            __progress = false;
            int __i = 0;
            outer: while (__i < __oldPhis.size())
            {
                PhiNode __oldPhi = __oldPhis.get(__i);
                for (Node __usage : __oldPhi.usages())
                {
                    if (__usage instanceof PhiNode && __oldPhis.contains(__usage))
                    {
                        // Do not mark.
                    }
                    else
                    {
                        // Mark alive by removing from delete set.
                        __oldPhis.remove(__i);
                        __progress = true;
                        continue outer;
                    }
                }
                __i++;
            }
        }

        for (PhiNode __deadPhi : __oldPhis)
        {
            __deadPhi.clearInputs();
        }

        for (PhiNode __deadPhi : __oldPhis)
        {
            if (__deadPhi.isAlive())
            {
                GraphUtil.killWithUnusedFloatingInputs(__deadPhi);
            }
        }
    }

    private static void markStateNodes(StateSplit __stateSplit, NodeBitMap __marks)
    {
        FrameState __exitState = __stateSplit.stateAfter();
        if (__exitState != null)
        {
            __exitState.applyToVirtual(__v -> __marks.markAndGrow(__v));
        }
    }

    ///
    // Gets the corresponding value in this fragment.
    //
    // @param b original value
    // @return corresponding value in the peel
    ///
    @Override
    protected ValueNode prim(ValueNode __b)
    {
        LoopBeginNode __loopBegin = original().loop().loopBegin();
        if (__loopBegin.isPhiAtMerge(__b))
        {
            PhiNode __phi = (PhiNode) __b;
            return __phi.valueAt(__loopBegin.forwardEnd());
        }
        else if (this.___nodesReady)
        {
            ValueNode __v = getDuplicatedNode(__b);
            if (__v == null)
            {
                return __b;
            }
            return __v;
        }
        else
        {
            return __b;
        }
    }

    protected ValueNode primAfter(ValueNode __b)
    {
        LoopBeginNode __loopBegin = original().loop().loopBegin();
        if (__loopBegin.isPhiAtMerge(__b))
        {
            PhiNode __phi = (PhiNode) __b;
            return __phi.valueAt(1);
        }
        else if (this.___nodesReady)
        {
            ValueNode __v = getDuplicatedNode(__b);
            if (__v == null)
            {
                return __b;
            }
            return __v;
        }
        else
        {
            return __b;
        }
    }

    private AbstractBeginNode mergeEnds()
    {
        List<EndNode> __endsToMerge = new LinkedList<>();
        // map peel exits to the corresponding loop exits
        EconomicMap<AbstractEndNode, LoopEndNode> __reverseEnds = EconomicMap.create(Equivalence.IDENTITY);
        LoopBeginNode __loopBegin = original().loop().loopBegin();
        for (LoopEndNode __le : __loopBegin.loopEnds())
        {
            AbstractEndNode __duplicate = getDuplicatedNode(__le);
            if (__duplicate != null)
            {
                __endsToMerge.add((EndNode) __duplicate);
                __reverseEnds.put(__duplicate, __le);
            }
        }
        this.___mergedInitializers = EconomicMap.create(Equivalence.IDENTITY);
        AbstractBeginNode __newExit;
        StructuredGraph __graph = graph();
        if (__endsToMerge.size() == 1)
        {
            AbstractEndNode __end = __endsToMerge.get(0);
            __newExit = __graph.add(new BeginNode());
            __end.replaceAtPredecessor(__newExit);
            __end.safeDelete();
        }
        else
        {
            AbstractMergeNode __newExitMerge = __graph.add(new MergeNode());
            __newExit = __newExitMerge;
            FrameState __state = __loopBegin.stateAfter();
            FrameState __duplicateState = null;
            if (__state != null)
            {
                __duplicateState = __state.duplicateWithVirtualState();
                __newExitMerge.setStateAfter(__duplicateState);
            }
            for (EndNode __end : __endsToMerge)
            {
                __newExitMerge.addForwardEnd(__end);
            }

            for (final PhiNode __phi : __loopBegin.phis().snapshot())
            {
                if (__phi.hasNoUsages())
                {
                    continue;
                }
                final PhiNode __firstPhi = patchPhi(__graph, __phi, __newExitMerge);
                for (AbstractEndNode __end : __newExitMerge.forwardEnds())
                {
                    LoopEndNode __loopEnd = __reverseEnds.get(__end);
                    ValueNode __prim = prim(__phi.valueAt(__loopEnd));
                    __firstPhi.addInput(__prim);
                }
                ValueNode __initializer = __firstPhi;
                if (__duplicateState != null)
                {
                    // fix the merge's state after
                    __duplicateState.applyToNonVirtual(new NodeClosure<ValueNode>()
                    {
                        @Override
                        public void apply(Node __from, ValueNode __node)
                        {
                            if (__node == __phi)
                            {
                                __from.replaceFirstInput(__phi, __firstPhi);
                            }
                        }
                    });
                }
                this.___mergedInitializers.put(__phi, __initializer);
            }
        }
        return __newExit;
    }
}
