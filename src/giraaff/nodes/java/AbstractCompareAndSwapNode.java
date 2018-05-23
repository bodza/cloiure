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
// NodeInfo.allowedUsageTypes = Value, Memory
public abstract class AbstractCompareAndSwapNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single
{
    public static final NodeClass<AbstractCompareAndSwapNode> TYPE = NodeClass.create(AbstractCompareAndSwapNode.class);
    @Input ValueNode expectedValue;
    @Input ValueNode newValue;
    @OptionalInput(InputType.State) FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
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

    public AbstractCompareAndSwapNode(NodeClass<? extends AbstractCompareAndSwapNode> c, AddressNode address, LocationIdentity location, ValueNode expectedValue, ValueNode newValue, BarrierType barrierType, Stamp stamp)
    {
        super(c, address, location, stamp, barrierType);
        this.expectedValue = expectedValue;
        this.newValue = newValue;
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
