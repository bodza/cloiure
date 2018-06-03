package giraaff.nodes.extended;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;

/**
 * This node provides a state split along with the functionality of {@link FixedValueAnchorNode}.
 * This is used to capture a state for deoptimization when a node has side effects which aren't
 * easily represented. The anchored value is usually part of the FrameState since this forces uses
 * of the value below this node so they will consume this frame state instead of an earlier one.
 */
// @class StateSplitProxyNode
public final class StateSplitProxyNode extends FixedValueAnchorNode implements Canonicalizable, StateSplit
{
    // @def
    public static final NodeClass<StateSplitProxyNode> TYPE = NodeClass.create(StateSplitProxyNode.class);

    @OptionalInput(InputType.State)
    // @field
    FrameState stateAfter;
    /**
     * Disallows elimination of this node until after the FrameState has been consumed.
     */
    // @field
    private final boolean delayElimination;

    // @cons
    public StateSplitProxyNode(ValueNode __object)
    {
        this(__object, false);
    }

    // @cons
    public StateSplitProxyNode(ValueNode __object, boolean __delayElimination)
    {
        super(TYPE, __object);
        this.delayElimination = __delayElimination;
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(stateAfter, __x);
        stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (object.isConstant() && !delayElimination || stateAfter == null)
        {
            return object;
        }
        return this;
    }
}
