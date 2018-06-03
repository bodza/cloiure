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
    // @def
    public static final NodeClass<ValueCompareAndSwapNode> TYPE = NodeClass.create(ValueCompareAndSwapNode.class);

    // @cons
    public ValueCompareAndSwapNode(ValueNode __address, ValueNode __expectedValue, ValueNode __newValue, LocationIdentity __location)
    {
        this((AddressNode) __address, __expectedValue, __newValue, __location, BarrierType.NONE);
    }

    // @cons
    public ValueCompareAndSwapNode(AddressNode __address, ValueNode __expectedValue, ValueNode __newValue, LocationIdentity __location, BarrierType __barrierType)
    {
        super(TYPE, __address, __location, __expectedValue, __newValue, __barrierType, __expectedValue.stamp(NodeView.DEFAULT).meet(__newValue.stamp(NodeView.DEFAULT)).unrestricted());
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRGeneratorTool __tool = __gen.getLIRGeneratorTool();
        __gen.setResult(this, __tool.emitValueCompareAndSwap(__gen.operand(getAddress()), __gen.operand(getExpectedValue()), __gen.operand(getNewValue())));
    }
}
