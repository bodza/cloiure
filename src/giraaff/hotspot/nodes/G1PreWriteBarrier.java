package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class G1PreWriteBarrier
public final class G1PreWriteBarrier extends ObjectWriteBarrier implements DeoptimizingNode.DeoptBefore
{
    // @def
    public static final NodeClass<G1PreWriteBarrier> TYPE = NodeClass.create(G1PreWriteBarrier.class);

    @OptionalInput(InputType.State)
    // @field
    FrameState stateBefore;
    // @field
    protected final boolean nullCheck;
    // @field
    protected final boolean doLoad;

    // @cons
    public G1PreWriteBarrier(AddressNode __address, ValueNode __expectedObject, boolean __doLoad, boolean __nullCheck)
    {
        super(TYPE, __address, __expectedObject, true);
        this.doLoad = __doLoad;
        this.nullCheck = __nullCheck;
    }

    public ValueNode getExpectedObject()
    {
        return getValue();
    }

    public boolean doLoad()
    {
        return doLoad;
    }

    public boolean getNullCheck()
    {
        return nullCheck;
    }

    @Override
    public boolean canDeoptimize()
    {
        return nullCheck;
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __state)
    {
        updateUsages(stateBefore, __state);
        stateBefore = __state;
    }
}
