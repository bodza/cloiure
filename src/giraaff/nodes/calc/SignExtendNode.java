package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// The {@code SignExtendNode} converts an integer to a wider integer using sign extension.
///
// @class SignExtendNode
public final class SignExtendNode extends IntegerConvertNode<ArithmeticOpTable.IntegerConvertOp.SignExtend, ArithmeticOpTable.IntegerConvertOp.Narrow>
{
    // @def
    public static final NodeClass<SignExtendNode> TYPE = NodeClass.create(SignExtendNode.class);

    // @cons SignExtendNode
    public SignExtendNode(ValueNode __input, int __resultBits)
    {
        this(__input, PrimitiveStamp.getBits(__input.stamp(NodeView.DEFAULT)), __resultBits);
    }

    // @cons SignExtendNode
    public SignExtendNode(ValueNode __input, int __inputBits, int __resultBits)
    {
        super(TYPE, ArithmeticOpTable::getSignExtend, ArithmeticOpTable::getNarrow, __inputBits, __resultBits, __input);
    }

    public static ValueNode create(ValueNode __input, int __resultBits, NodeView __view)
    {
        return create(__input, PrimitiveStamp.getBits(__input.stamp(__view)), __resultBits, __view);
    }

    public static ValueNode create(ValueNode __input, int __inputBits, int __resultBits, NodeView __view)
    {
        ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend> __signExtend = ArithmeticOpTable.forStamp(__input.stamp(__view)).getSignExtend();
        ValueNode __synonym = findSynonym(__signExtend, __input, __inputBits, __resultBits, __signExtend.foldStamp(__inputBits, __resultBits, __input.stamp(__view)));
        if (__synonym != null)
        {
            return __synonym;
        }
        return canonical(null, __input, __inputBits, __resultBits, __view);
    }

    @Override
    public boolean isLossless()
    {
        return true;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __ret = super.canonical(__tool, __forValue);
        if (__ret != this)
        {
            return __ret;
        }

        return canonical(this, __forValue, getInputBits(), getResultBits(), __view);
    }

    private static ValueNode canonical(SignExtendNode __self, ValueNode __forValue, int __inputBits, int __resultBits, NodeView __view)
    {
        if (__forValue instanceof SignExtendNode)
        {
            // sxxx -(sign-extend)-> ssss sxxx -(sign-extend)-> ssssssss sssssxxx
            // ==> sxxx -(sign-extend)-> ssssssss sssssxxx
            SignExtendNode __other = (SignExtendNode) __forValue;
            return SignExtendNode.create(__other.getValue(), __other.getInputBits(), __resultBits, __view);
        }
        else if (__forValue instanceof ZeroExtendNode)
        {
            ZeroExtendNode __other = (ZeroExtendNode) __forValue;
            if (__other.getResultBits() > __other.getInputBits())
            {
                // sxxx -(zero-extend)-> 0000 sxxx -(sign-extend)-> 00000000 0000sxxx
                // ==> sxxx -(zero-extend)-> 00000000 0000sxxx
                return ZeroExtendNode.create(__other.getValue(), __other.getInputBits(), __resultBits, __view, __other.isInputAlwaysPositive());
            }
        }

        if (__forValue.stamp(__view) instanceof IntegerStamp)
        {
            IntegerStamp __inputStamp = (IntegerStamp) __forValue.stamp(__view);
            if ((__inputStamp.upMask() & (1L << (__inputBits - 1))) == 0L)
            {
                // 0xxx -(sign-extend)-> 0000 0xxx
                // ==> 0xxx -(zero-extend)-> 0000 0xxx
                return ZeroExtendNode.create(__forValue, __inputBits, __resultBits, __view, true);
            }
        }

        return __self != null ? __self : new SignExtendNode(__forValue, __inputBits, __resultBits);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitSignExtend(__nodeValueMap.operand(getValue()), getInputBits(), getResultBits()));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
