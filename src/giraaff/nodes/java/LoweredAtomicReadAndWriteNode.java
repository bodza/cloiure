package giraaff.nodes.java;

import jdk.vm.ci.meta.Value;

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
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Represents the lowered version of an atomic read-and-write operation like
 * {@link sun.misc.Unsafe#getAndSetInt(Object, long, int)}.
 */
public final class LoweredAtomicReadAndWriteNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single
{
    public static final NodeClass<LoweredAtomicReadAndWriteNode> TYPE = NodeClass.create(LoweredAtomicReadAndWriteNode.class);
    @Input ValueNode newValue;
    @OptionalInput(InputType.State) FrameState stateAfter;

    public LoweredAtomicReadAndWriteNode(AddressNode address, LocationIdentity location, ValueNode newValue, BarrierType barrierType)
    {
        super(TYPE, address, location, newValue.stamp(NodeView.DEFAULT).unrestricted(), barrierType);
        this.newValue = newValue;
    }

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

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result = gen.getLIRGeneratorTool().emitAtomicReadAndWrite(gen.operand(getAddress()), gen.operand(getNewValue()));
        gen.setResult(this, result);
    }

    @Override
    public boolean canNullCheck()
    {
        return false;
    }

    public ValueNode getNewValue()
    {
        return newValue;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return stamp(NodeView.DEFAULT);
    }
}
