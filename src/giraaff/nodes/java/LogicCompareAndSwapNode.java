package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Represents the low-level version of an atomic compare-and-swap operation.
 *
 * This version returns a boolean indicating is the CAS was successful or not.
 */
public final class LogicCompareAndSwapNode extends AbstractCompareAndSwapNode
{
    public static final NodeClass<LogicCompareAndSwapNode> TYPE = NodeClass.create(LogicCompareAndSwapNode.class);

    public LogicCompareAndSwapNode(ValueNode address, ValueNode expectedValue, ValueNode newValue, LocationIdentity location)
    {
        this((AddressNode) address, location, expectedValue, newValue, BarrierType.NONE);
    }

    public LogicCompareAndSwapNode(AddressNode address, LocationIdentity location, ValueNode expectedValue, ValueNode newValue, BarrierType barrierType)
    {
        super(TYPE, address, location, expectedValue, newValue, barrierType, StampFactory.forKind(JavaKind.Boolean.getStackKind()));
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        LIRGeneratorTool tool = gen.getLIRGeneratorTool();

        LIRKind resultKind = tool.getLIRKind(stamp(NodeView.DEFAULT));
        Value trueResult = tool.emitConstant(resultKind, JavaConstant.TRUE);
        Value falseResult = tool.emitConstant(resultKind, JavaConstant.FALSE);
        Value result = tool.emitLogicCompareAndSwap(gen.operand(getAddress()), gen.operand(getExpectedValue()), gen.operand(getNewValue()), trueResult, falseResult);

        gen.setResult(this, result);
    }
}
