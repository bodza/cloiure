package giraaff.nodes.java;

import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.LIRLowerableAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Represents the lowered version of an atomic read-and-write operation like
// {@link sun.misc.Unsafe#getAndSetInt(Object, long, int)}.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class LoweredAtomicReadAndWriteNode
public final class LoweredAtomicReadAndWriteNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<LoweredAtomicReadAndWriteNode> TYPE = NodeClass.create(LoweredAtomicReadAndWriteNode.class);

    @Node.Input
    // @field
    ValueNode ___newValue;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateAfter;

    // @cons LoweredAtomicReadAndWriteNode
    public LoweredAtomicReadAndWriteNode(AddressNode __address, LocationIdentity __location, ValueNode __newValue, HeapAccess.BarrierType __barrierType)
    {
        super(TYPE, __address, __location, __newValue.stamp(NodeView.DEFAULT).unrestricted(), __barrierType);
        this.___newValue = __newValue;
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
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __result = __gen.getLIRGeneratorTool().emitAtomicReadAndWrite(__gen.operand(getAddress()), __gen.operand(getNewValue()));
        __gen.setResult(this, __result);
    }

    @Override
    public boolean canNullCheck()
    {
        return false;
    }

    public ValueNode getNewValue()
    {
        return this.___newValue;
    }

    @Override
    public Stamp getAccessStamp()
    {
        return stamp(NodeView.DEFAULT);
    }
}
