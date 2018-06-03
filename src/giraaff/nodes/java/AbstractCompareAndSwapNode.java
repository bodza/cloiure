package giraaff.nodes.java;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.LIRLowerableAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.AddressNode;

///
// Low-level atomic compare-and-swap operation.
///
// @NodeInfo.allowedUsageTypes "Value, Memory"
// @class AbstractCompareAndSwapNode
public abstract class AbstractCompareAndSwapNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<AbstractCompareAndSwapNode> TYPE = NodeClass.create(AbstractCompareAndSwapNode.class);

    @Input
    // @field
    ValueNode ___expectedValue;
    @Input
    // @field
    ValueNode ___newValue;
    @OptionalInput(InputType.State)
    // @field
    FrameState ___stateAfter;

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

    public ValueNode getExpectedValue()
    {
        return this.___expectedValue;
    }

    public ValueNode getNewValue()
    {
        return this.___newValue;
    }

    // @cons
    public AbstractCompareAndSwapNode(NodeClass<? extends AbstractCompareAndSwapNode> __c, AddressNode __address, LocationIdentity __location, ValueNode __expectedValue, ValueNode __newValue, BarrierType __barrierType, Stamp __stamp)
    {
        super(__c, __address, __location, __stamp, __barrierType);
        this.___expectedValue = __expectedValue;
        this.___newValue = __newValue;
    }

    @Override
    public boolean canNullCheck()
    {
        return false;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return this.___expectedValue.stamp(NodeView.DEFAULT).meet(this.___newValue.stamp(NodeView.DEFAULT)).unrestricted();
    }
}
