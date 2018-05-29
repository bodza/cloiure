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
    public static final NodeClass<G1PreWriteBarrier> TYPE = NodeClass.create(G1PreWriteBarrier.class);

    @OptionalInput(InputType.State) FrameState stateBefore;
    protected final boolean nullCheck;
    protected final boolean doLoad;

    // @cons
    public G1PreWriteBarrier(AddressNode address, ValueNode expectedObject, boolean doLoad, boolean nullCheck)
    {
        super(TYPE, address, expectedObject, true);
        this.doLoad = doLoad;
        this.nullCheck = nullCheck;
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
    public void setStateBefore(FrameState state)
    {
        updateUsages(stateBefore, state);
        stateBefore = state;
    }
}
