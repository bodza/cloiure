package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// The {@code ZeroExtendNode} converts an integer to a wider integer using zero extension.
///
// @class ZeroExtendNode
public final class ZeroExtendNode extends IntegerConvertNode<ArithmeticOpTable.IntegerConvertOp.ZeroExtend, ArithmeticOpTable.IntegerConvertOp.Narrow>
{
    // @def
    public static final NodeClass<ZeroExtendNode> TYPE = NodeClass.create(ZeroExtendNode.class);

    // @field
    private final boolean ___inputAlwaysPositive;

    // @cons ZeroExtendNode
    public ZeroExtendNode(ValueNode __input, int __resultBits)
    {
        this(__input, PrimitiveStamp.getBits(__input.stamp(NodeView.DEFAULT)), __resultBits, false);
    }

    // @cons ZeroExtendNode
    public ZeroExtendNode(ValueNode __input, int __inputBits, int __resultBits, boolean __inputAlwaysPositive)
    {
        super(TYPE, ArithmeticOpTable::getZeroExtend, ArithmeticOpTable::getNarrow, __inputBits, __resultBits, __input);
        this.___inputAlwaysPositive = __inputAlwaysPositive;
    }

    public static ValueNode create(ValueNode __input, int __resultBits, NodeView __view)
    {
        return create(__input, PrimitiveStamp.getBits(__input.stamp(__view)), __resultBits, __view, false);
    }

    public static ValueNode create(ValueNode __input, int __inputBits, int __resultBits, NodeView __view)
    {
        return create(__input, __inputBits, __resultBits, __view, false);
    }

    public static ValueNode create(ValueNode __input, int __inputBits, int __resultBits, NodeView __view, boolean __alwaysPositive)
    {
        ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend> __signExtend = ArithmeticOpTable.forStamp(__input.stamp(__view)).getZeroExtend();
        ValueNode __synonym = findSynonym(__signExtend, __input, __inputBits, __resultBits, __signExtend.foldStamp(__inputBits, __resultBits, __input.stamp(__view)));
        if (__synonym != null)
        {
            return __synonym;
        }
        return canonical(null, __input, __inputBits, __resultBits, __view, __alwaysPositive);
    }

    @Override
    public boolean isLossless()
    {
        return true;
    }

    public boolean isInputAlwaysPositive()
    {
        return this.___inputAlwaysPositive;
    }

    @Override
    public boolean preservesOrder(CanonicalCondition __cond)
    {
        switch (__cond)
        {
            case LT:
                return false;
            default:
                return true;
        }
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

        return canonical(this, __forValue, getInputBits(), getResultBits(), __view, this.___inputAlwaysPositive);
    }

    private static ValueNode canonical(ZeroExtendNode __zeroExtendNode, ValueNode __forValue, int __inputBits, int __resultBits, NodeView __view, boolean __alwaysPositive)
    {
        ZeroExtendNode __self = __zeroExtendNode;
        if (__forValue instanceof ZeroExtendNode)
        {
            // xxxx -(zero-extend)-> 0000 xxxx -(zero-extend)-> 00000000 0000xxxx
            // ==> xxxx -(zero-extend)-> 00000000 0000xxxx
            ZeroExtendNode __other = (ZeroExtendNode) __forValue;
            return new ZeroExtendNode(__other.getValue(), __other.getInputBits(), __resultBits, __other.isInputAlwaysPositive());
        }
        if (__forValue instanceof NarrowNode)
        {
            NarrowNode __narrow = (NarrowNode) __forValue;
            Stamp __inputStamp = __narrow.getValue().stamp(__view);
            if (__inputStamp instanceof IntegerStamp)
            {
                IntegerStamp __istamp = (IntegerStamp) __inputStamp;
                long __mask = CodeUtil.mask(PrimitiveStamp.getBits(__narrow.stamp(__view)));

                if ((__istamp.upMask() & ~__mask) == 0)
                {
                    // The original value cannot change because of the narrow and zero extend.

                    if (__istamp.getBits() < __resultBits)
                    {
                        // Need to keep the zero extend, skip the narrow.
                        return create(__narrow.getValue(), __resultBits, __view);
                    }
                    else if (__istamp.getBits() > __resultBits)
                    {
                        // Need to keep the narrow, skip the zero extend.
                        return NarrowNode.create(__narrow.getValue(), __resultBits, __view);
                    }
                    else
                    {
                        // Just return the original value.
                        return __narrow.getValue();
                    }
                }
            }
        }

        if (__self == null)
        {
            __self = new ZeroExtendNode(__forValue, __inputBits, __resultBits, __alwaysPositive);
        }
        return __self;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitZeroExtend(__nodeValueMap.operand(getValue()), getInputBits(), getResultBits()));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
