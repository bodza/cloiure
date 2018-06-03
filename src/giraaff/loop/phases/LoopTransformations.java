package giraaff.loop.phases;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jdk.vm.ci.code.BailoutException;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.graph.Graph.Mark;
import giraaff.graph.Node;
import giraaff.graph.Position;
import giraaff.loop.CountedLoopInfo;
import giraaff.loop.InductionVariable;
import giraaff.loop.InductionVariable.Direction;
import giraaff.loop.LoopEx;
import giraaff.loop.LoopFragmentInside;
import giraaff.loop.LoopFragmentWhole;
import giraaff.loop.MathUtil;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.extended.SwitchNode;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.util.GraalError;

// @class LoopTransformations
public abstract class LoopTransformations
{
    // @cons
    private LoopTransformations()
    {
        super();
    }

    public static void peel(LoopEx __loop)
    {
        __loop.inside().duplicate().insertBefore(__loop);
        __loop.loopBegin().setLoopFrequency(Math.max(0.0, __loop.loopBegin().loopFrequency() - 1));
    }

    public static void fullUnroll(LoopEx __loop, PhaseContext __context, CanonicalizerPhase __canonicalizer)
    {
        LoopBeginNode __loopBegin = __loop.loopBegin();
        StructuredGraph __graph = __loopBegin.graph();
        int __initialNodeCount = __graph.getNodeCount();
        while (!__loopBegin.isDeleted())
        {
            Mark __mark = __graph.getMark();
            peel(__loop);
            __canonicalizer.applyIncremental(__graph, __context, __mark);
            __loop.invalidateFragments();
            if (__graph.getNodeCount() > __initialNodeCount + GraalOptions.maximumDesiredSize * 2)
            {
                throw new BailoutException(false, "full unroll: graph seems to grow out of proportion");
            }
        }
    }

    public static void unswitch(LoopEx __loop, List<ControlSplitNode> __controlSplitNodeSet)
    {
        ControlSplitNode __firstNode = __controlSplitNodeSet.iterator().next();
        LoopFragmentWhole __originalLoop = __loop.whole();
        StructuredGraph __graph = __firstNode.graph();

        __loop.loopBegin().incrementUnswitches();

        // create new control split out of loop
        ControlSplitNode __newControlSplit = (ControlSplitNode) __firstNode.copyWithInputs();
        __originalLoop.entryPoint().replaceAtPredecessor(__newControlSplit);

        // The code below assumes that all of the control split nodes have the same successor
        // structure, which should have been enforced by findUnswitchable.
        Iterator<Position> __successors = __firstNode.successorPositions().iterator();
        // original loop is used as first successor
        Position __firstPosition = __successors.next();
        AbstractBeginNode __originalLoopBegin = BeginNode.begin(__originalLoop.entryPoint());
        __firstPosition.set(__newControlSplit, __originalLoopBegin);

        while (__successors.hasNext())
        {
            Position __position = __successors.next();
            // create a new loop duplicate and connect it
            LoopFragmentWhole __duplicateLoop = __originalLoop.duplicate();
            AbstractBeginNode __newBegin = BeginNode.begin(__duplicateLoop.entryPoint());
            __position.set(__newControlSplit, __newBegin);

            // for each cloned ControlSplitNode, simplify the proper path
            for (ControlSplitNode __controlSplitNode : __controlSplitNodeSet)
            {
                ControlSplitNode __duplicatedControlSplit = __duplicateLoop.getDuplicatedNode(__controlSplitNode);
                if (__duplicatedControlSplit.isAlive())
                {
                    AbstractBeginNode __survivingSuccessor = (AbstractBeginNode) __position.get(__duplicatedControlSplit);
                    __survivingSuccessor.replaceAtUsages(InputType.Guard, __newBegin);
                    __graph.removeSplitPropagate(__duplicatedControlSplit, __survivingSuccessor);
                }
            }
        }
        // original loop is simplified last to avoid deleting controlSplitNode too early
        for (ControlSplitNode __controlSplitNode : __controlSplitNodeSet)
        {
            if (__controlSplitNode.isAlive())
            {
                AbstractBeginNode __survivingSuccessor = (AbstractBeginNode) __firstPosition.get(__controlSplitNode);
                __survivingSuccessor.replaceAtUsages(InputType.Guard, __originalLoopBegin);
                __graph.removeSplitPropagate(__controlSplitNode, __survivingSuccessor);
            }
        }

        // TODO probabilities need some amount of fixup (probably also in other transforms)
    }

    public static void partialUnroll(LoopEx __loop)
    {
        LoopFragmentInside __newSegment = __loop.inside().duplicate();
        __newSegment.insertWithinAfter(__loop);
    }

    // This function splits candidate loops into pre, main and post loops,
    // dividing the iteration space to facilitate the majority of iterations
    // being executed in a main loop, which will have RCE implemented upon it.
    // The initial loop form is constrained to single entry/exit, but can have
    // flow. The translation looks like:
    //
    //       (Simple Loop entry)                   (Pre Loop Entry)
    //                |                                  |
    //         (LoopBeginNode)                    (LoopBeginNode)
    //                |                                  |
    //       (Loop Control Test)<------   ==>  (Loop control Test)<------
    //         /               \       \         /               \       \
    //    (Loop Exit)      (Loop Body) |    (Loop Exit)      (Loop Body) |
    //        |                |       |        |                |       |
    // (continue code)     (Loop End)  |  if (M < length)*   (Loop End)  |
    //                         \       /       /      \           \      /
    //                          ----->        /       |            ----->
    //                                       /  if ( ... )*
    //                                      /     /       \
    //                                     /     /         \
    //                                    /     /           \
    //                                   |     /     (Main Loop Entry)
    //                                   |    |             |
    //                                   |    |      (LoopBeginNode)
    //                                   |    |             |
    //                                   |    |     (Loop Control Test)<------
    //                                   |    |      /               \        \
    //                                   |    |  (Loop Exit)      (Loop Body) |
    //                                    \   \      |                |       |
    //                                     \   \     |            (Loop End)  |
    //                                      \   \    |                \       /
    //                                       \   \   |                 ------>
    //                                        \   \  |
    //                                      (Main Loop Merge)*
    //                                               |
    //                                      (Post Loop Entry)
    //                                               |
    //                                        (LoopBeginNode)
    //                                               |
    //                                       (Loop Control Test)<-----
    //                                        /               \       \
    //                                    (Loop Exit)     (Loop Body) |
    //                                        |               |       |
    //                                 (continue code)    (Loop End)  |
    //                                                         \      /
    //                                                          ----->
    //
    // Key: "*" = optional.
    //
    // The value "M" is the maximal value of the loop trip for the original
    // loop. The value of "length" is applicable to the number of arrays found
    // in the loop but is reduced if some or all of the arrays are known to be
    // the same length as "M". The maximum number of tests can be equal to the
    // number of arrays in the loop, where multiple instances of an array are
    // subsumed into a single test for that arrays length.
    //
    // If the optional main loop entry tests are absent, the Pre Loop exit
    // connects to the Main loops entry and there is no merge hanging off the
    // main loops exit to converge flow from said tests. All split use data
    // flow is mitigated through phi(s) in the main merge if present and
    // passed through the main and post loop phi(s) from the originating pre
    // loop with final phi(s) and data flow patched to the "continue code".
    // The pre loop is constrained to one iteration for now and will likely
    // be updated to produce vector alignment if applicable.

    public static LoopBeginNode insertPrePostLoops(LoopEx __loop)
    {
        StructuredGraph __graph = __loop.loopBegin().graph();
        LoopFragmentWhole __preLoop = __loop.whole();
        CountedLoopInfo __preCounted = __loop.counted();
        IfNode __preLimit = __preCounted.getLimitTest();
        LoopBeginNode __preLoopBegin = __loop.loopBegin();
        InductionVariable __preIv = __preCounted.getCounter();
        LoopExitNode __preLoopExitNode = __preLoopBegin.getSingleLoopExit();
        FixedNode __continuationNode = __preLoopExitNode.next();

        // each duplication is inserted after the original, ergo create the post loop first
        LoopFragmentWhole __mainLoop = __preLoop.duplicate();
        LoopFragmentWhole __postLoop = __preLoop.duplicate();
        __preLoopBegin.incrementSplits();
        __preLoopBegin.incrementSplits();
        __preLoopBegin.setPreLoop();
        LoopBeginNode __mainLoopBegin = __mainLoop.getDuplicatedNode(__preLoopBegin);
        __mainLoopBegin.setMainLoop();
        LoopBeginNode __postLoopBegin = __postLoop.getDuplicatedNode(__preLoopBegin);
        __postLoopBegin.setPostLoop();

        EndNode __postEndNode = getBlockEndAfterLoopExit(__postLoopBegin);
        AbstractMergeNode __postMergeNode = __postEndNode.merge();
        LoopExitNode __postLoopExitNode = __postLoopBegin.getSingleLoopExit();

        // update the main loop phi initialization to carry from the pre loop
        for (PhiNode __prePhiNode : __preLoopBegin.phis())
        {
            PhiNode __mainPhiNode = __mainLoop.getDuplicatedNode(__prePhiNode);
            __mainPhiNode.setValueAt(0, __prePhiNode);
        }

        EndNode __mainEndNode = getBlockEndAfterLoopExit(__mainLoopBegin);
        AbstractMergeNode __mainMergeNode = __mainEndNode.merge();
        AbstractEndNode __postEntryNode = __postLoopBegin.forwardEnd();

        // in the case of no Bounds tests, we just flow right into the main loop
        AbstractBeginNode __mainLandingNode = BeginNode.begin(__postEntryNode);
        LoopExitNode __mainLoopExitNode = __mainLoopBegin.getSingleLoopExit();
        __mainLoopExitNode.setNext(__mainLandingNode);
        __preLoopExitNode.setNext(__mainLoopBegin.forwardEnd());

        // add and update any phi edges as per merge usage as needed and update usages
        processPreLoopPhis(__loop, __mainLoop, __postLoop);
        __continuationNode.predecessor().clearSuccessors();
        __postLoopExitNode.setNext(__continuationNode);
        cleanupMerge(__postMergeNode, __postLoopExitNode);
        cleanupMerge(__mainMergeNode, __mainLandingNode);

        // change the preLoop to execute one iteration for now
        updateMainLoopLimit(__preLimit, __preIv, __mainLoop);
        updatePreLoopLimit(__preLimit, __preIv, __preCounted);
        __preLoopBegin.setLoopFrequency(1);
        __mainLoopBegin.setLoopFrequency(Math.max(0.0, __mainLoopBegin.loopFrequency() - 2));
        __postLoopBegin.setLoopFrequency(Math.max(0.0, __postLoopBegin.loopFrequency() - 1));

        // the pre and post loops don't require safepoints at all
        for (SafepointNode __safepoint : __preLoop.nodes().filter(SafepointNode.class))
        {
            __graph.removeFixed(__safepoint);
        }
        for (SafepointNode __safepoint : __postLoop.nodes().filter(SafepointNode.class))
        {
            __graph.removeFixed(__safepoint);
        }
        return __mainLoopBegin;
    }

    ///
    // Cleanup the merge and remove the predecessors too.
    ///
    private static void cleanupMerge(AbstractMergeNode __mergeNode, AbstractBeginNode __landingNode)
    {
        for (EndNode __end : __mergeNode.cfgPredecessors().snapshot())
        {
            __mergeNode.removeEnd(__end);
            __end.safeDelete();
        }
        __mergeNode.prepareDelete(__landingNode);
        __mergeNode.safeDelete();
    }

    private static void processPreLoopPhis(LoopEx __preLoop, LoopFragmentWhole __mainLoop, LoopFragmentWhole __postLoop)
    {
        // process phis for the post loop
        LoopBeginNode __preLoopBegin = __preLoop.loopBegin();
        for (PhiNode __prePhiNode : __preLoopBegin.phis())
        {
            PhiNode __postPhiNode = __postLoop.getDuplicatedNode(__prePhiNode);
            PhiNode __mainPhiNode = __mainLoop.getDuplicatedNode(__prePhiNode);
            __postPhiNode.setValueAt(0, __mainPhiNode);

            // build a work list to update the pre loop phis to the post loops phis
            for (Node __usage : __prePhiNode.usages().snapshot())
            {
                if (__usage == __mainPhiNode)
                {
                    continue;
                }
                if (__preLoop.isOutsideLoop(__usage))
                {
                    __usage.replaceFirstInput(__prePhiNode, __postPhiNode);
                }
            }
        }
        for (Node __node : __preLoop.inside().nodes())
        {
            for (Node __externalUsage : __node.usages().snapshot())
            {
                if (__preLoop.isOutsideLoop(__externalUsage))
                {
                    Node __postUsage = __postLoop.getDuplicatedNode(__node);
                    __externalUsage.replaceFirstInput(__node, __postUsage);
                }
            }
        }
    }

    ///
    // Find the end of the block following the LoopExit.
    ///
    private static EndNode getBlockEndAfterLoopExit(LoopBeginNode __curLoopBegin)
    {
        FixedNode __node = __curLoopBegin.getSingleLoopExit().next();
        // find the last node after the exit blocks starts
        return getBlockEnd(__node);
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

    private static void updateMainLoopLimit(IfNode __preLimit, InductionVariable __preIv, LoopFragmentWhole __mainLoop)
    {
        // update the main loops limit test to be different than the post loop
        StructuredGraph __graph = __preLimit.graph();
        IfNode __mainLimit = __mainLoop.getDuplicatedNode(__preLimit);
        LogicNode __ifTest = __mainLimit.condition();
        CompareNode __compareNode = (CompareNode) __ifTest;
        ValueNode __prePhi = __preIv.valueNode();
        ValueNode __mainPhi = __mainLoop.getDuplicatedNode(__prePhi);
        ValueNode __preStride = __preIv.strideNode();
        ValueNode __mainStride;
        if (__preStride instanceof ConstantNode)
        {
            __mainStride = __preStride;
        }
        else
        {
            __mainStride = __mainLoop.getDuplicatedNode(__preStride);
        }
        // fetch the bounds to pose lowering the range by one
        ValueNode __ub = null;
        if (__compareNode.getX() == __mainPhi)
        {
            __ub = __compareNode.getY();
        }
        else if (__compareNode.getY() == __mainPhi)
        {
            __ub = __compareNode.getX();
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }

        // Preloop always performs at least one iteration, so remove that from the main loop.
        ValueNode __newLimit = MathUtil.sub(__graph, __ub, __mainStride);

        // re-wire the condition with the new limit
        __compareNode.replaceFirstInput(__ub, __newLimit);
    }

    private static void updatePreLoopLimit(IfNode __preLimit, InductionVariable __preIv, CountedLoopInfo __preCounted)
    {
        // update the pre loops limit test
        StructuredGraph __graph = __preLimit.graph();
        LogicNode __ifTest = __preLimit.condition();
        CompareNode __compareNode = (CompareNode) __ifTest;
        ValueNode __prePhi = __preIv.valueNode();
        // make new limit one iteration
        ValueNode __initIv = __preCounted.getStart();
        ValueNode __newLimit = MathUtil.add(__graph, __initIv, __preIv.strideNode());

        // fetch the variable we are not replacing and configure the one we are
        ValueNode __ub;
        if (__compareNode.getX() == __prePhi)
        {
            __ub = __compareNode.getY();
        }
        else if (__compareNode.getY() == __prePhi)
        {
            __ub = __compareNode.getX();
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
        // re-wire the condition with the new limit
        if (__preIv.direction() == Direction.Up)
        {
            __compareNode.replaceFirstInput(__ub, __graph.unique(new ConditionalNode(__graph.unique(new IntegerLessThanNode(__newLimit, __ub)), __newLimit, __ub)));
        }
        else
        {
            __compareNode.replaceFirstInput(__ub, __graph.unique(new ConditionalNode(__graph.unique(new IntegerLessThanNode(__ub, __newLimit)), __newLimit, __ub)));
        }
    }

    public static List<ControlSplitNode> findUnswitchable(LoopEx __loop)
    {
        List<ControlSplitNode> __controls = null;
        ValueNode __invariantValue = null;
        for (IfNode __ifNode : __loop.whole().nodes().filter(IfNode.class))
        {
            if (__loop.isOutsideLoop(__ifNode.condition()))
            {
                if (__controls == null)
                {
                    __invariantValue = __ifNode.condition();
                    __controls = new ArrayList<>();
                    __controls.add(__ifNode);
                }
                else if (__ifNode.condition() == __invariantValue)
                {
                    __controls.add(__ifNode);
                }
            }
        }
        if (__controls == null)
        {
            SwitchNode __firstSwitch = null;
            for (SwitchNode __switchNode : __loop.whole().nodes().filter(SwitchNode.class))
            {
                if (__switchNode.successors().count() > 1 && __loop.isOutsideLoop(__switchNode.value()))
                {
                    if (__controls == null)
                    {
                        __firstSwitch = __switchNode;
                        __invariantValue = __switchNode.value();
                        __controls = new ArrayList<>();
                        __controls.add(__switchNode);
                    }
                    else if (__switchNode.value() == __invariantValue && __firstSwitch.structureEquals(__switchNode))
                    {
                        // only collect switches which test the same values in the same order
                        __controls.add(__switchNode);
                    }
                }
            }
        }
        return __controls;
    }

    public static boolean isUnrollableLoop(LoopEx __loop)
    {
        if (!__loop.isCounted() || !__loop.counted().getCounter().isConstantStride() || !__loop.loop().getChildren().isEmpty())
        {
            return false;
        }
        LoopBeginNode __loopBegin = __loop.loopBegin();
        LogicNode __condition = __loop.counted().getLimitTest().condition();
        if (!(__condition instanceof CompareNode))
        {
            return false;
        }
        if (((CompareNode) __condition).condition() == CanonicalCondition.EQ)
        {
            return false;
        }
        if (__loopBegin.isMainLoop() || __loopBegin.isSimpleLoop())
        {
            // Flow-less loops to partial unroll for now. 3 blocks corresponds to an if that either exits
            // or continues the loop. There might be fixed and floating work within the loop as well.
            if (__loop.loop().getBlocks().size() < 3)
            {
                return true;
            }
        }
        return false;
    }
}
