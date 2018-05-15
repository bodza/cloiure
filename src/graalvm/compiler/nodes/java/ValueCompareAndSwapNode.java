package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.word.LocationIdentity;

/**
 * A special purpose store node that differs from {@link LogicCompareAndSwapNode} in that it returns
 * either the expected value or the compared against value instead of a boolean.
 */
@NodeInfo(cycles = CYCLES_8, size = SIZE_8)
public final class ValueCompareAndSwapNode extends AbstractCompareAndSwapNode {
    public static final NodeClass<ValueCompareAndSwapNode> TYPE = NodeClass.create(ValueCompareAndSwapNode.class);

    public ValueCompareAndSwapNode(ValueNode address, ValueNode expectedValue, ValueNode newValue, LocationIdentity location) {
        this((AddressNode) address, expectedValue, newValue, location, BarrierType.NONE);
    }

    public ValueCompareAndSwapNode(AddressNode address, ValueNode expectedValue, ValueNode newValue, LocationIdentity location, BarrierType barrierType) {
        super(TYPE, address, location, expectedValue, newValue, barrierType, expectedValue.stamp(NodeView.DEFAULT).meet(newValue.stamp(NodeView.DEFAULT)).unrestricted());
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        assert getNewValue().stamp(NodeView.DEFAULT).isCompatible(getExpectedValue().stamp(NodeView.DEFAULT));
        LIRGeneratorTool tool = gen.getLIRGeneratorTool();
        assert !this.canDeoptimize();
        gen.setResult(this, tool.emitValueCompareAndSwap(gen.operand(getAddress()), gen.operand(getExpectedValue()), gen.operand(getNewValue())));
    }
}
