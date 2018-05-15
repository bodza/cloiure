package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.InputType.State;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.FixedAccessNode;
import graalvm.compiler.nodes.memory.LIRLowerableAccess;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.Value;

/**
 * Represents the lowered version of an atomic read-and-write operation like
 * {@link sun.misc.Unsafe#getAndSetInt(Object, long, int)}.
 */
@NodeInfo(allowedUsageTypes = {Memory}, cycles = CYCLES_8, size = SIZE_2)
public final class LoweredAtomicReadAndWriteNode extends FixedAccessNode implements StateSplit, LIRLowerableAccess, MemoryCheckpoint.Single {

    public static final NodeClass<LoweredAtomicReadAndWriteNode> TYPE = NodeClass.create(LoweredAtomicReadAndWriteNode.class);
    @Input ValueNode newValue;
    @OptionalInput(State) FrameState stateAfter;

    public LoweredAtomicReadAndWriteNode(AddressNode address, LocationIdentity location, ValueNode newValue, BarrierType barrierType) {
        super(TYPE, address, location, newValue.stamp(NodeView.DEFAULT).unrestricted(), barrierType);
        this.newValue = newValue;
    }

    @Override
    public FrameState stateAfter() {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x) {
        assert x == null || x.isAlive() : "frame state must be in a graph";
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect() {
        return true;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        Value result = gen.getLIRGeneratorTool().emitAtomicReadAndWrite(gen.operand(getAddress()), gen.operand(getNewValue()));
        gen.setResult(this, result);
    }

    @Override
    public boolean canNullCheck() {
        return false;
    }

    public ValueNode getNewValue() {
        return newValue;
    }

    @Override
    public Stamp getAccessStamp() {
        return stamp(NodeView.DEFAULT);
    }
}
