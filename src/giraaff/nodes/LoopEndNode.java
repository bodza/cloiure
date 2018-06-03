package giraaff.nodes;

import java.util.Collections;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * LoopEnd nodes represent a loop back-edge. When a LoopEnd is reached, execution continues at the
 * {@linkplain #loopBegin() loop header}.
 */
// @class LoopEndNode
public final class LoopEndNode extends AbstractEndNode
{
    // @def
    public static final NodeClass<LoopEndNode> TYPE = NodeClass.create(LoopEndNode.class);

    /*
     * The declared type of the field cannot be LoopBeginNode, because loop explosion during partial
     * evaluation can temporarily assign a non-loop begin. This node will then be deleted shortly
     * after - but we still must not have type system violations for that short amount of time.
     */
    @Input(InputType.Association)
    // @field
    AbstractBeginNode loopBegin;
    // @field
    protected int endIndex;

    /**
     * Most loop ends need a safepoint (flag set to true) so that garbage collection can interrupt a
     * long-running (possibly endless) loop. Safepoints may be disabled for two reasons: 1) Some
     * code must be safepoint free, i.e., uninterruptible by garbage collection. 2) An optimization
     * phase determined that the loop already has another safepoint or cannot be endless, so there
     * is no need for a loop-end safepoint.
     *
     * Note that 1) is a hard correctness issue: emitting a safepoint in uninterruptible code is a
     * bug, i.e., it is not allowed to set the flag back to true once it is false. To ensure that
     * loop ends that are created late, e.g. during control flow simplifications, have no
     * safepoints in such cases, the safepoints are actually disabled for the
     * {@link LoopBeginNode#canEndsSafepoint loop begin}. New loop ends inherit the flag value from
     * the loop begin.
     */
    // @field
    boolean canSafepoint;

    // @cons
    public LoopEndNode(LoopBeginNode __begin)
    {
        super(TYPE);
        int __idx = __begin.nextEndIndex();
        this.endIndex = __idx;
        this.loopBegin = __begin;
        this.canSafepoint = __begin.canEndsSafepoint;
    }

    @Override
    public AbstractMergeNode merge()
    {
        return loopBegin();
    }

    public LoopBeginNode loopBegin()
    {
        return (LoopBeginNode) loopBegin;
    }

    public void setLoopBegin(LoopBeginNode __x)
    {
        updateUsages(this.loopBegin, __x);
        this.loopBegin = __x;
    }

    /**
     * Disables safepoints for only this loop end (in contrast to disabling it for
     * {@link LoopBeginNode#disableSafepoint() the whole loop}.
     */
    public void disableSafepoint()
    {
        this.canSafepoint = false;
    }

    public boolean canSafepoint()
    {
        return canSafepoint;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.visitLoopEnd(this);
        super.generate(__gen);
    }

    /**
     * Returns the index of this loop end amongst its {@link LoopBeginNode}'s loop ends.
     *
     * Since a LoopBeginNode also has {@linkplain LoopBeginNode#forwardEnds() forward ends},
     * this is *not* the index into {@link PhiNode} values at the loop begin. Use
     * {@link LoopBeginNode#phiPredecessorIndex(AbstractEndNode)} for this purpose.
     */
    int endIndex()
    {
        return endIndex;
    }

    void setEndIndex(int __idx)
    {
        this.endIndex = __idx;
    }

    @Override
    public Iterable<? extends Node> cfgSuccessors()
    {
        return Collections.emptyList();
    }
}
