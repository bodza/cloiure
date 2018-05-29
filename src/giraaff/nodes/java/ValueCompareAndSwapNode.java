package giraaff.nodes.java;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * A special purpose store node that differs from {@link LogicCompareAndSwapNode} in that it returns
 * either the expected value or the compared against value instead of a boolean.
 */
// @class ValueCompareAndSwapNode
public final class ValueCompareAndSwapNode extends AbstractCompareAndSwapNode
{
    public static final NodeClass<ValueCompareAndSwapNode> TYPE = NodeClass.create(ValueCompareAndSwapNode.class);

    // @cons
    public ValueCompareAndSwapNode(ValueNode address, ValueNode expectedValue, ValueNode newValue, LocationIdentity location)
    {
        this((AddressNode) address, expectedValue, newValue, location, BarrierType.NONE);
    }

    // @cons
    public ValueCompareAndSwapNode(AddressNode address, ValueNode expectedValue, ValueNode newValue, LocationIdentity location, BarrierType barrierType)
    {
        super(TYPE, address, location, expectedValue, newValue, barrierType, expectedValue.stamp(NodeView.DEFAULT).meet(newValue.stamp(NodeView.DEFAULT)).unrestricted());
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRGeneratorTool tool = gen.getLIRGeneratorTool();
        gen.setResult(this, tool.emitValueCompareAndSwap(gen.operand(getAddress()), gen.operand(getExpectedValue()), gen.operand(getNewValue())));
    }
}
