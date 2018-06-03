package giraaff.nodes;

import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.iterators.NodePredicates;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

// @class LoopBeginNode
public final class LoopBeginNode extends AbstractMergeNode implements IterableNodeType, LIRLowerable
{
    // @def
    public static final NodeClass<LoopBeginNode> TYPE = NodeClass.create(LoopBeginNode.class);

    // @field
    protected double loopFrequency;
    // @field
    protected double loopOrigFrequency;
    // @field
    protected int nextEndIndex;
    // @field
    protected int unswitches;
    // @field
    protected int splits;
    // @field
    protected int inversionCount;
    // @field
    protected LoopType loopType;
    // @field
    protected int unrollFactor;

    // @enum LoopBeginNode.LoopType
    public enum LoopType
    {
        SIMPLE_LOOP,
        PRE_LOOP,
        MAIN_LOOP,
        POST_LOOP
    }

    /**
     * See {@link LoopEndNode#canSafepoint} for more information.
     */
    // @field
    boolean canEndsSafepoint;

    @OptionalInput(InputType.Guard)
    // @field
    GuardingNode overflowGuard;

    // @cons
    public LoopBeginNode()
    {
        super(TYPE);
        loopFrequency = 1;
        loopOrigFrequency = 1;
        unswitches = 0;
        splits = 0;
        this.canEndsSafepoint = true;
        loopType = LoopType.SIMPLE_LOOP;
        unrollFactor = 1;
    }

    public boolean isSimpleLoop()
    {
        return (loopType == LoopType.SIMPLE_LOOP);
    }

    public void setPreLoop()
    {
        loopType = LoopType.PRE_LOOP;
    }

    public boolean isPreLoop()
    {
        return (loopType == LoopType.PRE_LOOP);
    }

    public void setMainLoop()
    {
        loopType = LoopType.MAIN_LOOP;
    }

    public boolean isMainLoop()
    {
        return (loopType == LoopType.MAIN_LOOP);
    }

    public void setPostLoop()
    {
        loopType = LoopType.POST_LOOP;
    }

    public boolean isPostLoop()
    {
        return (loopType == LoopType.POST_LOOP);
    }

    public int getUnrollFactor()
    {
        return unrollFactor;
    }

    public void setUnrollFactor(int __currentUnrollFactor)
    {
        unrollFactor = __currentUnrollFactor;
    }

    /**
     * Disables safepoint for the whole loop, i.e., for all {@link LoopEndNode loop ends}.
     */
    public void disableSafepoint()
    {
        // Store flag locally in case new loop ends are created later on.
        this.canEndsSafepoint = false;
        // Propagate flag to all existing loop ends.
        for (LoopEndNode __loopEnd : loopEnds())
        {
            __loopEnd.disableSafepoint();
        }
    }

    public double loopOrigFrequency()
    {
        return loopOrigFrequency;
    }

    public void setLoopOrigFrequency(double __loopOrigFrequency)
    {
        this.loopOrigFrequency = __loopOrigFrequency;
    }

    public double loopFrequency()
    {
        return loopFrequency;
    }

    public void setLoopFrequency(double __loopFrequency)
    {
        this.loopFrequency = __loopFrequency;
    }

    /**
     * Returns the <b>unordered</b> set of {@link LoopEndNode} that correspond to back-edges for
     * this loop. The order of the back-edges is unspecified, if you need to get an ordering
     * compatible for {@link PhiNode} creation, use {@link #orderedLoopEnds()}.
     *
     * @return the set of {@code LoopEndNode} that correspond to back-edges for this loop
     */
    public NodeIterable<LoopEndNode> loopEnds()
    {
        return usages().filter(LoopEndNode.class);
    }

    public NodeIterable<LoopExitNode> loopExits()
    {
        return usages().filter(LoopExitNode.class);
    }

    @Override
    public NodeIterable<Node> anchored()
    {
        return super.anchored().filter(NodePredicates.isNotA(LoopEndNode.class).nor(LoopExitNode.class));
    }

    /**
     * Returns the set of {@link LoopEndNode} that correspond to back-edges for this loop, in
     * increasing {@link #phiPredecessorIndex} order. This method is suited to create new loop
     * {@link PhiNode}.
     *
     * For example a new PhiNode may be added as follow:
     *
     * <pre>
     * PhiNode phi = new ValuePhiNode(stamp, loop);
     * phi.addInput(forwardEdgeValue);
     * for (LoopEndNode loopEnd : loop.orderedLoopEnds()) {
     *     phi.addInput(backEdgeValue(loopEnd));
     * }
     * </pre>
     *
     * @return the set of {@code LoopEndNode} that correspond to back-edges for this loop
     */
    public LoopEndNode[] orderedLoopEnds()
    {
        LoopEndNode[] __result = new LoopEndNode[this.getLoopEndCount()];
        for (LoopEndNode __end : loopEnds())
        {
            __result[__end.endIndex()] = __end;
        }
        return __result;
    }

    public boolean isSingleEntryLoop()
    {
        return (forwardEndCount() == 1);
    }

    public AbstractEndNode forwardEnd()
    {
        return forwardEndAt(0);
    }

    public int splits()
    {
        return splits;
    }

    public void incrementSplits()
    {
        splits++;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // Nothing to emit, since this is node is used for structural purposes only.
    }

    @Override
    protected void deleteEnd(AbstractEndNode __end)
    {
        if (__end instanceof LoopEndNode)
        {
            LoopEndNode __loopEnd = (LoopEndNode) __end;
            __loopEnd.setLoopBegin(null);
            int __idx = __loopEnd.endIndex();
            for (LoopEndNode __le : loopEnds())
            {
                int __leIdx = __le.endIndex();
                if (__leIdx > __idx)
                {
                    __le.setEndIndex(__leIdx - 1);
                }
            }
            nextEndIndex--;
        }
        else
        {
            super.deleteEnd(__end);
        }
    }

    @Override
    public int phiPredecessorCount()
    {
        return forwardEndCount() + loopEnds().count();
    }

    @Override
    public int phiPredecessorIndex(AbstractEndNode __pred)
    {
        if (__pred instanceof LoopEndNode)
        {
            LoopEndNode __loopEnd = (LoopEndNode) __pred;
            if (__loopEnd.loopBegin() == this)
            {
                return __loopEnd.endIndex() + forwardEndCount();
            }
        }
        else
        {
            return super.forwardEndIndex((EndNode) __pred);
        }
        throw new InternalError("should not reach here: " + "unknown __pred : " + __pred);
    }

    @Override
    public AbstractEndNode phiPredecessorAt(int __index)
    {
        if (__index < forwardEndCount())
        {
            return forwardEndAt(__index);
        }
        for (LoopEndNode __end : loopEnds())
        {
            int __idx = __index - forwardEndCount();
            if (__end.endIndex() == __idx)
            {
                return __end;
            }
        }
        throw new InternalError("should not reach here");
    }

    int nextEndIndex()
    {
        return nextEndIndex++;
    }

    public int getLoopEndCount()
    {
        return nextEndIndex;
    }

    public int unswitches()
    {
        return unswitches;
    }

    public void incrementUnswitches()
    {
        unswitches++;
    }

    public int getInversionCount()
    {
        return inversionCount;
    }

    public void setInversionCount(int __count)
    {
        inversionCount = __count;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        canonicalizePhis(__tool);
    }

    public boolean isLoopExit(AbstractBeginNode __begin)
    {
        return __begin instanceof LoopExitNode && ((LoopExitNode) __begin).loopBegin() == this;
    }

    public LoopExitNode getSingleLoopExit()
    {
        return loopExits().first();
    }

    public LoopEndNode getSingleLoopEnd()
    {
        return loopEnds().first();
    }

    public void removeExits()
    {
        for (LoopExitNode __loopexit : loopExits().snapshot())
        {
            __loopexit.removeProxies();
            FrameState __loopStateAfter = __loopexit.stateAfter();
            graph().replaceFixedWithFixed(__loopexit, graph().add(new BeginNode()));
            if (__loopStateAfter != null)
            {
                GraphUtil.tryKillUnused(__loopStateAfter);
            }
        }
    }

    public GuardingNode getOverflowGuard()
    {
        return overflowGuard;
    }

    public void setOverflowGuard(GuardingNode __overflowGuard)
    {
        updateUsagesInterface(this.overflowGuard, __overflowGuard);
        this.overflowGuard = __overflowGuard;
    }

    // @def
    private static final int NO_INCREMENT = Integer.MIN_VALUE;

    /**
     * Returns an array with one entry for each input of the phi, which is either {@link #NO_INCREMENT}
     * or the increment, i.e., the value by which the phi is incremented in the corresponding branch.
     */
    private static int[] getSelfIncrements(PhiNode __phi)
    {
        int[] __selfIncrement = new int[__phi.valueCount()];
        for (int __i = 0; __i < __phi.valueCount(); __i++)
        {
            ValueNode __input = __phi.valueAt(__i);
            long __increment = NO_INCREMENT;
            if (__input != null && __input instanceof AddNode && __input.stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                AddNode __add = (AddNode) __input;
                if (__add.getX() == __phi && __add.getY().isConstant())
                {
                    __increment = __add.getY().asJavaConstant().asLong();
                }
                else if (__add.getY() == __phi && __add.getX().isConstant())
                {
                    __increment = __add.getX().asJavaConstant().asLong();
                }
            }
            else if (__input == __phi)
            {
                __increment = 0;
            }
            if (__increment < Integer.MIN_VALUE || __increment > Integer.MAX_VALUE || __increment == NO_INCREMENT)
            {
                __increment = NO_INCREMENT;
            }
            __selfIncrement[__i] = (int) __increment;
        }
        return __selfIncrement;
    }

    /**
     * Coalesces loop phis that represent the same value (which is not handled by normal Global
     * Value Numbering).
     */
    public void canonicalizePhis(SimplifierTool __tool)
    {
        int __phiCount = phis().count();
        if (__phiCount > 1)
        {
            int __phiInputCount = phiPredecessorCount();
            int __phiIndex = 0;
            int[][] __selfIncrement = new int[__phiCount][];
            PhiNode[] __phis = this.phis().snapshot().toArray(new PhiNode[__phiCount]);

            for (__phiIndex = 0; __phiIndex < __phiCount; __phiIndex++)
            {
                PhiNode __phi = __phis[__phiIndex];
                if (__phi != null)
                {
                    nextPhi: for (int otherPhiIndex = __phiIndex + 1; otherPhiIndex < __phiCount; otherPhiIndex++)
                    {
                        PhiNode __otherPhi = __phis[otherPhiIndex];
                        if (__otherPhi == null || __phi.getNodeClass() != __otherPhi.getNodeClass() || !__phi.valueEquals(__otherPhi))
                        {
                            continue nextPhi;
                        }
                        if (__selfIncrement[__phiIndex] == null)
                        {
                            __selfIncrement[__phiIndex] = getSelfIncrements(__phi);
                        }
                        if (__selfIncrement[otherPhiIndex] == null)
                        {
                            __selfIncrement[otherPhiIndex] = getSelfIncrements(__otherPhi);
                        }
                        int[] __phiIncrement = __selfIncrement[__phiIndex];
                        int[] __otherPhiIncrement = __selfIncrement[otherPhiIndex];
                        for (int __inputIndex = 0; __inputIndex < __phiInputCount; __inputIndex++)
                        {
                            if (__phiIncrement[__inputIndex] == NO_INCREMENT)
                            {
                                if (__phi.valueAt(__inputIndex) != __otherPhi.valueAt(__inputIndex))
                                {
                                    continue nextPhi;
                                }
                            }
                            if (__phiIncrement[__inputIndex] != __otherPhiIncrement[__inputIndex])
                            {
                                continue nextPhi;
                            }
                        }
                        if (__tool != null)
                        {
                            __tool.addToWorkList(__otherPhi.usages());
                        }
                        __otherPhi.replaceAtUsages(__phi);
                        GraphUtil.killWithUnusedFloatingInputs(__otherPhi);
                        __phis[otherPhiIndex] = null;
                    }
                }
            }
        }
    }
}
