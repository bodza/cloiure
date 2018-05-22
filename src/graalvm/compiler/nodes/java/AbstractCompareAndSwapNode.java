package graalvm.compiler.nodes.java;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.FixedAccessNode;
import graalvm.compiler.nodes.memory.LIRLowerableAccess;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.address.AddressNode;

/**
 * Low-level atomic compare-and-swap operation.
 */
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
