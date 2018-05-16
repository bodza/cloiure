package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import java.util.List;

import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.memory.MemoryPhiNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.util.GraphUtil;

/**
 * Denotes the merging of multiple control-flow paths.
 */
@NodeInfo(allowedUsageTypes = Association, cycles = CYCLES_0, size = SIZE_0)
public abstract class AbstractMergeNode extends BeginStateSplitNode implements IterableNodeType, Simplifiable, LIRLowerable
{
    public static final NodeClass<AbstractMergeNode> TYPE = NodeClass.create(AbstractMergeNode.class);

    protected AbstractMergeNode(NodeClass<? extends AbstractMergeNode> c)
    {
        super(c);
    }

    @Input(Association) protected NodeInputList<EndNode> ends = new NodeInputList<>(this);

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.visitMerge(this);
    }

    public int forwardEndIndex(EndNode end)
    {
        return ends.indexOf(end);
    }

    public void addForwardEnd(EndNode end)
    {
        ends.add(end);
    }

    public final int forwardEndCount()
    {
        return ends.size();
    }

    public final EndNode forwardEndAt(int index)
    {
        return ends.get(index);
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
    public boolean isPhiAtMerge(Node value)
    {
        return value instanceof PhiNode && ((PhiNode) value).merge() == this;
    }

    /**
     * Removes the given end from the merge, along with the entries corresponding to this end in the
     * phis connected to the merge.
     *
     * @param pred the end to remove
     */
    public void removeEnd(AbstractEndNode pred)
    {
        int predIndex = phiPredecessorIndex(pred);
        assert predIndex != -1;
        deleteEnd(pred);
        for (PhiNode phi : phis().snapshot())
        {
            if (phi.isDeleted())
            {
                continue;
            }
            ValueNode removedValue = phi.valueAt(predIndex);
            phi.removeInput(predIndex);
            if (removedValue != null)
            {
                GraphUtil.tryKillUnused(removedValue);
            }
        }
    }

    protected void deleteEnd(AbstractEndNode end)
    {
        ends.remove(end);
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

    public int phiPredecessorIndex(AbstractEndNode pred)
    {
        return forwardEndIndex((EndNode) pred);
    }

    public AbstractEndNode phiPredecessorAt(int index)
    {
        return forwardEndAt(index);
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
        return super.anchored().filter(n -> !isPhiAtMerge(n));
    }

    /**
     * This simplify method can deal with a null value for tool, so that it can be used outside of
     * canonicalization.
     */
    @Override
    @SuppressWarnings("try")
    public void simplify(SimplifierTool tool)
    {
        FixedNode currentNext = next();
        if (currentNext instanceof AbstractEndNode)
        {
            AbstractEndNode origLoopEnd = (AbstractEndNode) currentNext;
            AbstractMergeNode merge = origLoopEnd.merge();
            if (merge instanceof LoopBeginNode && !(origLoopEnd instanceof LoopEndNode))
            {
                return;
            }
            // in order to move anchored values to the other merge we would need to check if the
            // anchors are used by phis of the other merge
            if (this.anchored().isNotEmpty())
            {
                return;
            }
            if (merge.stateAfter() == null && this.stateAfter() != null)
            {
                // We hold a state, but the succeeding merge does not => do not combine.
                return;
            }
            for (PhiNode phi : phis())
            {
                for (Node usage : phi.usages())
                {
                    if (!(usage instanceof VirtualState) && !merge.isPhiAtMerge(usage))
                    {
                        return;
                    }
                }
            }
            getDebug().log("Split %s into ends for %s.", this, merge);
            int numEnds = this.forwardEndCount();
            for (int i = 0; i < numEnds - 1; i++)
            {
                AbstractEndNode end = forwardEndAt(numEnds - 1 - i);
                if (tool != null)
                {
                    tool.addToWorkList(end);
                }
                AbstractEndNode newEnd;
                try (DebugCloseable position = end.withNodeSourcePosition())
                {
                    if (merge instanceof LoopBeginNode)
                    {
                        newEnd = graph().add(new LoopEndNode((LoopBeginNode) merge));
                    }
                    else
                    {
                        EndNode tmpEnd = graph().add(new EndNode());
                        merge.addForwardEnd(tmpEnd);
                        newEnd = tmpEnd;
                    }
                }
                for (PhiNode phi : merge.phis())
                {
                    ValueNode v = phi.valueAt(origLoopEnd);
                    ValueNode newInput;
                    if (isPhiAtMerge(v))
                    {
                        PhiNode endPhi = (PhiNode) v;
                        newInput = endPhi.valueAt(end);
                    }
                    else
                    {
                        newInput = v;
                    }
                    phi.addInput(newInput);
                }
                this.removeEnd(end);
                end.replaceAtPredecessor(newEnd);
                end.safeDelete();
                if (tool != null)
                {
                    tool.addToWorkList(newEnd.predecessor());
                }
            }
            graph().reduceTrivialMerge(this);
        }
        else if (currentNext instanceof ReturnNode)
        {
            ReturnNode returnNode = (ReturnNode) currentNext;
            if (anchored().isNotEmpty() || returnNode.getMemoryMap() != null)
            {
                return;
            }
            List<PhiNode> phis = phis().snapshot();
            for (PhiNode phi : phis)
            {
                for (Node usage : phi.usages())
                {
                    if (usage != returnNode && !(usage instanceof FrameState))
                    {
                        return;
                    }
                }
            }

            ValuePhiNode returnValuePhi = returnNode.result() == null || !isPhiAtMerge(returnNode.result()) ? null : (ValuePhiNode) returnNode.result();
            List<EndNode> endNodes = forwardEnds().snapshot();
            for (EndNode end : endNodes)
            {
                try (DebugCloseable position = returnNode.withNodeSourcePosition())
                {
                    ReturnNode newReturn = graph().add(new ReturnNode(returnValuePhi == null ? returnNode.result() : returnValuePhi.valueAt(end)));
                    if (tool != null)
                    {
                        tool.addToWorkList(end.predecessor());
                    }
                    end.replaceAtPredecessor(newReturn);
                }
            }
            GraphUtil.killCFG(this);
            for (EndNode end : endNodes)
            {
                end.safeDelete();
            }
            for (PhiNode phi : phis)
            {
                if (tool.allUsagesAvailable() && phi.isAlive() && phi.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(phi);
                }
            }
        }
    }
}
