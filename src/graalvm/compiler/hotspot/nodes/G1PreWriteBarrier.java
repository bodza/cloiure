package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

@NodeInfo(cycles = CYCLES_64, size = SIZE_64)
public final class G1PreWriteBarrier extends ObjectWriteBarrier implements DeoptimizingNode.DeoptBefore {

    public static final NodeClass<G1PreWriteBarrier> TYPE = NodeClass.create(G1PreWriteBarrier.class);

    @OptionalInput(InputType.State) FrameState stateBefore;
    protected final boolean nullCheck;
    protected final boolean doLoad;

    public G1PreWriteBarrier(AddressNode address, ValueNode expectedObject, boolean doLoad, boolean nullCheck) {
        super(TYPE, address, expectedObject, true);
        this.doLoad = doLoad;
        this.nullCheck = nullCheck;
    }

    public ValueNode getExpectedObject() {
        return getValue();
    }

    public boolean doLoad() {
        return doLoad;
    }

    public boolean getNullCheck() {
        return nullCheck;
    }

    @Override
    public boolean canDeoptimize() {
        return nullCheck;
    }

    @Override
    public FrameState stateBefore() {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState state) {
        updateUsages(stateBefore, state);
        stateBefore = state;
    }
}
