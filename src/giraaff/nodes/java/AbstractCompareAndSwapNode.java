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

/**
 * Low-level atomic compare-and-swap operation.
 */
// @NodeInfo.allowedUsageTypes "Value, Memory"
// @class AbstractCompareAndSwapNode
public abstract class AbstractCompareAndSwapNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<AbstractCompareAndSwapNode> TYPE = NodeClass.create(AbstractCompareAndSwapNode.class);

    @Input
    // @field
    ValueNode expectedValue;
    @Input
    // @field
    ValueNode newValue;
    @OptionalInput(InputType.State)
    // @field
    FrameState stateAfter;

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

    public ValueNode getExpectedValue()
    {
        return expectedValue;
    }

    public ValueNode getNewValue()
    {
        return newValue;
    }

    // @cons
    public AbstractCompareAndSwapNode(NodeClass<? extends AbstractCompareAndSwapNode> __c, AddressNode __address, LocationIdentity __location, ValueNode __expectedValue, ValueNode __newValue, BarrierType __barrierType, Stamp __stamp)
    {
        super(__c, __address, __location, __stamp, __barrierType);
        this.expectedValue = __expectedValue;
        this.newValue = __newValue;
    }

    @Override
    public boolean canNullCheck()
    {
        return false;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return expectedValue.stamp(NodeView.DEFAULT).meet(newValue.stamp(NodeView.DEFAULT)).unrestricted();
    }
}
