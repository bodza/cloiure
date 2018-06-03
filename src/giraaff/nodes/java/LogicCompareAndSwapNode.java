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
// @class LogicCompareAndSwapNode
public final class LogicCompareAndSwapNode extends AbstractCompareAndSwapNode
{
    // @def
    public static final NodeClass<LogicCompareAndSwapNode> TYPE = NodeClass.create(LogicCompareAndSwapNode.class);

    // @cons
    public LogicCompareAndSwapNode(ValueNode __address, ValueNode __expectedValue, ValueNode __newValue, LocationIdentity __location)
    {
        this((AddressNode) __address, __location, __expectedValue, __newValue, BarrierType.NONE);
    }

    // @cons
    public LogicCompareAndSwapNode(AddressNode __address, LocationIdentity __location, ValueNode __expectedValue, ValueNode __newValue, BarrierType __barrierType)
    {
        super(TYPE, __address, __location, __expectedValue, __newValue, __barrierType, StampFactory.forKind(JavaKind.Boolean.getStackKind()));
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        LIRGeneratorTool __tool = __gen.getLIRGeneratorTool();

        LIRKind __resultKind = __tool.getLIRKind(stamp(NodeView.DEFAULT));
        Value __trueResult = __tool.emitConstant(__resultKind, JavaConstant.TRUE);
        Value __falseResult = __tool.emitConstant(__resultKind, JavaConstant.FALSE);
        Value __result = __tool.emitLogicCompareAndSwap(__gen.operand(getAddress()), __gen.operand(getExpectedValue()), __gen.operand(getNewValue()), __trueResult, __falseResult);

        __gen.setResult(this, __result);
    }
}
