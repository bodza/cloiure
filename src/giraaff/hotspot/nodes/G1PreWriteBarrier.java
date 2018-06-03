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
    FrameState ___stateBefore;
    // @field
    protected final boolean ___nullCheck;
    // @field
    protected final boolean ___doLoad;

    // @cons
    public G1PreWriteBarrier(AddressNode __address, ValueNode __expectedObject, boolean __doLoad, boolean __nullCheck)
    {
        super(TYPE, __address, __expectedObject, true);
        this.___doLoad = __doLoad;
        this.___nullCheck = __nullCheck;
    }

    public ValueNode getExpectedObject()
    {
        return getValue();
    }

    public boolean doLoad()
    {
        return this.___doLoad;
    }

    public boolean getNullCheck()
    {
        return this.___nullCheck;
    }

    @Override
    public boolean canDeoptimize()
    {
        return this.___nullCheck;
    }

    @Override
    public FrameState stateBefore()
    {
        return this.___stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __state)
    {
        updateUsages(this.___stateBefore, __state);
        this.___stateBefore = __state;
    }
}
