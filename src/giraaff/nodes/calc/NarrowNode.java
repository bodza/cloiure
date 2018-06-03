package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp.Narrow;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp.SignExtend;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * The {@code NarrowNode} converts an integer to a narrower integer.
 */
// @class NarrowNode
public final class NarrowNode extends IntegerConvertNode<Narrow, SignExtend>
{
    // @def
    public static final NodeClass<NarrowNode> TYPE = NodeClass.create(NarrowNode.class);

    // @cons
    public NarrowNode(ValueNode __input, int __resultBits)
    {
        this(__input, PrimitiveStamp.getBits(__input.stamp(NodeView.DEFAULT)), __resultBits);
    }

    // @cons
    public NarrowNode(ValueNode __input, int __inputBits, int __resultBits)
    {
        super(TYPE, ArithmeticOpTable::getNarrow, ArithmeticOpTable::getSignExtend, __inputBits, __resultBits, __input);
    }

    public static ValueNode create(ValueNode __input, int __resultBits, NodeView __view)
    {
        return create(__input, PrimitiveStamp.getBits(__input.stamp(__view)), __resultBits, __view);
    }

    public static ValueNode create(ValueNode __input, int __inputBits, int __resultBits, NodeView __view)
    {
        IntegerConvertOp<Narrow> __signExtend = ArithmeticOpTable.forStamp(__input.stamp(__view)).getNarrow();
        ValueNode __synonym = findSynonym(__signExtend, __input, __inputBits, __resultBits, __signExtend.foldStamp(__inputBits, __resultBits, __input.stamp(__view)));
        if (__synonym != null)
        {
            return __synonym;
        }
        else
        {
            return new NarrowNode(__input, __inputBits, __resultBits);
        }
    }

    @Override
    public boolean isLossless()
    {
        return false;
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

        if (__forValue instanceof NarrowNode)
        {
            // zzzzzzzz yyyyxxxx -(narrow)-> yyyyxxxx -(narrow)-> xxxx
            // ==> zzzzzzzz yyyyxxxx -(narrow)-> xxxx
            NarrowNode __other = (NarrowNode) __forValue;
            return new NarrowNode(__other.getValue(), __other.getInputBits(), getResultBits());
        }
        else if (__forValue instanceof IntegerConvertNode)
        {
            // SignExtendNode or ZeroExtendNode
            IntegerConvertNode<?, ?> __other = (IntegerConvertNode<?, ?>) __forValue;
            if (__other.getValue().hasExactlyOneUsage() && __other.hasMoreThanOneUsage())
            {
                // Do not perform if this will introduce a new live value.
                // If the original value's usage count is > 1, there is already another user.
                // If the convert's usage count is <=1, it will be dead code eliminated.
                return this;
            }
            if (getResultBits() == __other.getInputBits())
            {
                // xxxx -(extend)-> yyyy xxxx -(narrow)-> xxxx
                // ==> no-op
                return __other.getValue();
            }
            else if (getResultBits() < __other.getInputBits())
            {
                // yyyyxxxx -(extend)-> zzzzzzzz yyyyxxxx -(narrow)-> xxxx
                // ==> yyyyxxxx -(narrow)-> xxxx
                return new NarrowNode(__other.getValue(), __other.getInputBits(), getResultBits());
            }
            else
            {
                if (__other instanceof SignExtendNode)
                {
                    // sxxx -(sign-extend)-> ssssssss sssssxxx -(narrow)-> sssssxxx
                    // ==> sxxx -(sign-extend)-> sssssxxx
                    return SignExtendNode.create(__other.getValue(), __other.getInputBits(), getResultBits(), __view);
                }
                else if (__other instanceof ZeroExtendNode)
                {
                    // xxxx -(zero-extend)-> 00000000 0000xxxx -(narrow)-> 0000xxxx
                    // ==> xxxx -(zero-extend)-> 0000xxxx
                    return new ZeroExtendNode(__other.getValue(), __other.getInputBits(), getResultBits(), ((ZeroExtendNode) __other).isInputAlwaysPositive());
                }
            }
        }
        else if (__forValue instanceof AndNode)
        {
            AndNode __andNode = (AndNode) __forValue;
            IntegerStamp __yStamp = (IntegerStamp) __andNode.getY().stamp(__view);
            IntegerStamp __xStamp = (IntegerStamp) __andNode.getX().stamp(__view);
            long __relevantMask = CodeUtil.mask(this.getResultBits());
            if ((__relevantMask & __yStamp.downMask()) == __relevantMask)
            {
                return create(__andNode.getX(), this.getResultBits(), __view);
            }
            else if ((__relevantMask & __xStamp.downMask()) == __relevantMask)
            {
                return create(__andNode.getY(), this.getResultBits(), __view);
            }
        }

        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitNarrow(__nodeValueMap.operand(getValue()), getResultBits()));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return false;
    }
}
