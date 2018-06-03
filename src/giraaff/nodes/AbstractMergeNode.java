package giraaff.nodes;

import java.util.List;

import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.graph.iterators.NodeIterable;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.memory.MemoryPhiNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

/**
 * Denotes the merging of multiple control-flow paths.
 */
// @NodeInfo.allowedUsageTypes "Association"
// @class AbstractMergeNode
public abstract class AbstractMergeNode extends BeginStateSplitNode implements IterableNodeType, Simplifiable, LIRLowerable
{
    // @def
    public static final NodeClass<AbstractMergeNode> TYPE = NodeClass.create(AbstractMergeNode.class);

    // @cons
    protected AbstractMergeNode(NodeClass<? extends AbstractMergeNode> __c)
    {
        super(__c);
    }

    @Input(InputType.Association)
    // @field
    protected NodeInputList<EndNode> ends = new NodeInputList<>(this);

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.visitMerge(this);
    }

    public int forwardEndIndex(EndNode __end)
    {
        return ends.indexOf(__end);
    }

    public void addForwardEnd(EndNode __end)
    {
        ends.add(__end);
    }

    public final int forwardEndCount()
    {
        return ends.size();
    }

    public final EndNode forwardEndAt(int __index)
    {
        return ends.get(__index);
    }

    @Override
    public NodeIterable<EndNode> cfgPredecessors()
    {
        return ends;
    }

    /**
     * Determines if a given node is a phi whose {@linkplain PhiNode#merge() merge} is this node.
     *
     * @param value the instruction to test
     * @return {@code true} if {@code value} is a phi and its merge is {@code this}
     */
    public boolean isPhiAtMerge(Node __value)
    {
        return __value instanceof PhiNode && ((PhiNode) __value).merge() == this;
    }

    /**
     * Removes the given end from the merge, along with the entries corresponding to this end in the
     * phis connected to the merge.
     *
     * @param pred the end to remove
     */
    public void removeEnd(AbstractEndNode __pred)
    {
        int __predIndex = phiPredecessorIndex(__pred);
        deleteEnd(__pred);
        for (PhiNode __phi : phis().snapshot())
        {
            if (__phi.isDeleted())
            {
                continue;
            }
            ValueNode __removedValue = __phi.valueAt(__predIndex);
            __phi.removeInput(__predIndex);
            if (__removedValue != null)
            {
                GraphUtil.tryKillUnused(__removedValue);
            }
        }
    }

    protected void deleteEnd(AbstractEndNode __end)
    {
        ends.remove(__end);
    }

    public void clearEnds()
    {
        ends.clear();
    }

    public NodeInputList<EndNode> forwardEnds()
    {
        return ends;
    }

    public int phiPredecessorCount()
    {
        return forwardEndCount();
    }

    public int phiPredecessorIndex(AbstractEndNode __pred)
    {
        return forwardEndIndex((EndNode) __pred);
    }

    public AbstractEndNode phiPredecessorAt(int __index)
    {
        return forwardEndAt(__index);
    }

    public NodeIterable<PhiNode> phis()
    {
        return this.usages().filter(PhiNode.class).filter(this::isPhiAtMerge);
    }

    public NodeIterable<ValuePhiNode> valuePhis()
    {
        return this.usages().filter(ValuePhiNode.class);
    }

    public NodeIterable<MemoryPhiNode> memoryPhis()
    {
        return this.usages().filter(MemoryPhiNode.class);
    }

    @Override
    public NodeIterable<Node> anchored()
    {
        return super.anchored().filter(__n -> !isPhiAtMerge(__n));
    }

    /**
     * This simplify method can deal with a null value for tool, so that it can be used outside of canonicalization.
     */
    @Override
    public void simplify(SimplifierTool __tool)
    {
        FixedNode __currentNext = next();
        if (__currentNext instanceof AbstractEndNode)
        {
            AbstractEndNode __origLoopEnd = (AbstractEndNode) __currentNext;
            AbstractMergeNode __merge = __origLoopEnd.merge();
            if (__merge instanceof LoopBeginNode && !(__origLoopEnd instanceof LoopEndNode))
            {
                return;
            }
            // in order to move anchored values to the other merge we would need to check if the
            // anchors are used by phis of the other merge
            if (this.anchored().isNotEmpty())
            {
                return;
            }
            if (__merge.stateAfter() == null && this.stateAfter() != null)
            {
                // We hold a state, but the succeeding merge does not => do not combine.
                return;
            }
            for (PhiNode __phi : phis())
            {
                for (Node __usage : __phi.usages())
                {
                    if (!(__usage instanceof VirtualState) && !__merge.isPhiAtMerge(__usage))
                    {
                        return;
                    }
                }
            }
            int __numEnds = this.forwardEndCount();
            for (int __i = 0; __i < __numEnds - 1; __i++)
            {
                AbstractEndNode __end = forwardEndAt(__numEnds - 1 - __i);
                if (__tool != null)
                {
                    __tool.addToWorkList(__end);
                }
                AbstractEndNode __newEnd;
                if (__merge instanceof LoopBeginNode)
                {
                    __newEnd = graph().add(new LoopEndNode((LoopBeginNode) __merge));
                }
                else
                {
                    EndNode __tmpEnd = graph().add(new EndNode());
                    __merge.addForwardEnd(__tmpEnd);
                    __newEnd = __tmpEnd;
                }
                for (PhiNode __phi : __merge.phis())
                {
                    ValueNode __v = __phi.valueAt(__origLoopEnd);
                    ValueNode __newInput;
                    if (isPhiAtMerge(__v))
                    {
                        PhiNode __endPhi = (PhiNode) __v;
                        __newInput = __endPhi.valueAt(__end);
                    }
                    else
                    {
                        __newInput = __v;
                    }
                    __phi.addInput(__newInput);
                }
                this.removeEnd(__end);
                __end.replaceAtPredecessor(__newEnd);
                __end.safeDelete();
                if (__tool != null)
                {
                    __tool.addToWorkList(__newEnd.predecessor());
                }
            }
            graph().reduceTrivialMerge(this);
        }
        else if (__currentNext instanceof ReturnNode)
        {
            ReturnNode __returnNode = (ReturnNode) __currentNext;
            if (anchored().isNotEmpty() || __returnNode.getMemoryMap() != null)
            {
                return;
            }
            List<PhiNode> __phis = phis().snapshot();
            for (PhiNode __phi : __phis)
            {
                for (Node __usage : __phi.usages())
                {
                    if (__usage != __returnNode && !(__usage instanceof FrameState))
                    {
                        return;
                    }
                }
            }

            ValuePhiNode __returnValuePhi = __returnNode.result() == null || !isPhiAtMerge(__returnNode.result()) ? null : (ValuePhiNode) __returnNode.result();
            List<EndNode> __endNodes = forwardEnds().snapshot();
            for (EndNode __end : __endNodes)
            {
                ReturnNode __newReturn = graph().add(new ReturnNode(__returnValuePhi == null ? __returnNode.result() : __returnValuePhi.valueAt(__end)));
                if (__tool != null)
                {
                    __tool.addToWorkList(__end.predecessor());
                }
                __end.replaceAtPredecessor(__newReturn);
            }
            GraphUtil.killCFG(this);
            for (EndNode __end : __endNodes)
            {
                __end.safeDelete();
            }
            for (PhiNode __phi : __phis)
            {
                if (__tool.allUsagesAvailable() && __phi.isAlive() && __phi.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(__phi);
                }
            }
        }
    }
}
