package giraaff.nodes.extended;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;

///
// This node provides a state split along with the functionality of {@link FixedValueAnchorNode}.
// This is used to capture a state for deoptimization when a node has side effects which aren't
// easily represented. The anchored value is usually part of the FrameState since this forces uses
// of the value below this node so they will consume this frame state instead of an earlier one.
///
// @class StateSplitProxyNode
public final class StateSplitProxyNode extends FixedValueAnchorNode implements Canonicalizable, StateSplit
{
    // @def
    public static final NodeClass<StateSplitProxyNode> TYPE = NodeClass.create(StateSplitProxyNode.class);

    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateAfter;
    ///
    // Disallows elimination of this node until after the FrameState has been consumed.
    ///
    // @field
    private final boolean ___delayElimination;

    // @cons StateSplitProxyNode
    public StateSplitProxyNode(ValueNode __object)
    {
        this(__object, false);
    }

    // @cons StateSplitProxyNode
    public StateSplitProxyNode(ValueNode __object, boolean __delayElimination)
    {
        super(TYPE, __object);
        this.___delayElimination = __delayElimination;
    }

    @Override
    public FrameState stateAfter()
    {
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(this.___stateAfter, __x);
        this.___stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___object.isConstant() && !this.___delayElimination || this.___stateAfter == null)
        {
            return this.___object;
        }
        return this;
    }
}
