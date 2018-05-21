package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.IntegerConvertOp;
import graalvm.compiler.core.common.type.ArithmeticOpTable.IntegerConvertOp.Narrow;
import graalvm.compiler.core.common.type.ArithmeticOpTable.IntegerConvertOp.SignExtend;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.PrimitiveStamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * The {@code SignExtendNode} converts an integer to a wider integer using sign extension.
 */
public final class SignExtendNode extends IntegerConvertNode<SignExtend, Narrow>
{
    public static final NodeClass<SignExtendNode> TYPE = NodeClass.create(SignExtendNode.class);

    public SignExtendNode(ValueNode input, int resultBits)
    {
        this(input, PrimitiveStamp.getBits(input.stamp(NodeView.DEFAULT)), resultBits);
    }

    public SignExtendNode(ValueNode input, int inputBits, int resultBits)
    {
        super(TYPE, ArithmeticOpTable::getSignExtend, ArithmeticOpTable::getNarrow, inputBits, resultBits, input);
    }

    public static ValueNode create(ValueNode input, int resultBits, NodeView view)
    {
        return create(input, PrimitiveStamp.getBits(input.stamp(view)), resultBits, view);
    }

    public static ValueNode create(ValueNode input, int inputBits, int resultBits, NodeView view)
    {
        IntegerConvertOp<SignExtend> signExtend = ArithmeticOpTable.forStamp(input.stamp(view)).getSignExtend();
        ValueNode synonym = findSynonym(signExtend, input, inputBits, resultBits, signExtend.foldStamp(inputBits, resultBits, input.stamp(view)));
        if (synonym != null)
        {
            return synonym;
        }
        return canonical(null, input, inputBits, resultBits, view);
    }

    @Override
    public boolean isLossless()
    {
        return true;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        NodeView view = NodeView.from(tool);
        ValueNode ret = super.canonical(tool, forValue);
        if (ret != this)
        {
            return ret;
        }

        return canonical(this, forValue, getInputBits(), getResultBits(), view);
    }

    private static ValueNode canonical(SignExtendNode self, ValueNode forValue, int inputBits, int resultBits, NodeView view)
    {
        if (forValue instanceof SignExtendNode)
        {
            // sxxx -(sign-extend)-> ssss sxxx -(sign-extend)-> ssssssss sssssxxx
            // ==> sxxx -(sign-extend)-> ssssssss sssssxxx
            SignExtendNode other = (SignExtendNode) forValue;
            return SignExtendNode.create(other.getValue(), other.getInputBits(), resultBits, view);
        }
        else if (forValue instanceof ZeroExtendNode)
        {
            ZeroExtendNode other = (ZeroExtendNode) forValue;
            if (other.getResultBits() > other.getInputBits())
            {
                // sxxx -(zero-extend)-> 0000 sxxx -(sign-extend)-> 00000000 0000sxxx
                // ==> sxxx -(zero-extend)-> 00000000 0000sxxx
                return ZeroExtendNode.create(other.getValue(), other.getInputBits(), resultBits, view, other.isInputAlwaysPositive());
            }
        }

        if (forValue.stamp(view) instanceof IntegerStamp)
        {
            IntegerStamp inputStamp = (IntegerStamp) forValue.stamp(view);
            if ((inputStamp.upMask() & (1L << (inputBits - 1))) == 0L)
            {
                // 0xxx -(sign-extend)-> 0000 0xxx
                // ==> 0xxx -(zero-extend)-> 0000 0xxx
                return ZeroExtendNode.create(forValue, inputBits, resultBits, view, true);
            }
        }

        return self != null ? self : new SignExtendNode(forValue, inputBits, resultBits);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitSignExtend(nodeValueMap.operand(getValue()), getInputBits(), getResultBits()));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
