package graalvm.compiler.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

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
 * The {@code NarrowNode} converts an integer to a narrower integer.
 */
public final class NarrowNode extends IntegerConvertNode<Narrow, SignExtend>
{
    public static final NodeClass<NarrowNode> TYPE = NodeClass.create(NarrowNode.class);

    public NarrowNode(ValueNode input, int resultBits)
    {
        this(input, PrimitiveStamp.getBits(input.stamp(NodeView.DEFAULT)), resultBits);
    }

    public NarrowNode(ValueNode input, int inputBits, int resultBits)
    {
        super(TYPE, ArithmeticOpTable::getNarrow, ArithmeticOpTable::getSignExtend, inputBits, resultBits, input);
    }

    public static ValueNode create(ValueNode input, int resultBits, NodeView view)
    {
        return create(input, PrimitiveStamp.getBits(input.stamp(view)), resultBits, view);
    }

    public static ValueNode create(ValueNode input, int inputBits, int resultBits, NodeView view)
    {
        IntegerConvertOp<Narrow> signExtend = ArithmeticOpTable.forStamp(input.stamp(view)).getNarrow();
        ValueNode synonym = findSynonym(signExtend, input, inputBits, resultBits, signExtend.foldStamp(inputBits, resultBits, input.stamp(view)));
        if (synonym != null)
        {
            return synonym;
        }
        else
        {
            return new NarrowNode(input, inputBits, resultBits);
        }
    }

    @Override
    public boolean isLossless()
    {
        return false;
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

        if (forValue instanceof NarrowNode)
        {
            // zzzzzzzz yyyyxxxx -(narrow)-> yyyyxxxx -(narrow)-> xxxx
            // ==> zzzzzzzz yyyyxxxx -(narrow)-> xxxx
            NarrowNode other = (NarrowNode) forValue;
            return new NarrowNode(other.getValue(), other.getInputBits(), getResultBits());
        }
        else if (forValue instanceof IntegerConvertNode)
        {
            // SignExtendNode or ZeroExtendNode
            IntegerConvertNode<?, ?> other = (IntegerConvertNode<?, ?>) forValue;
            if (other.getValue().hasExactlyOneUsage() && other.hasMoreThanOneUsage())
            {
                // Do not perform if this will introduce a new live value.
                // If the original value's usage count is > 1, there is already another user.
                // If the convert's usage count is <=1, it will be dead code eliminated.
                return this;
            }
            if (getResultBits() == other.getInputBits())
            {
                // xxxx -(extend)-> yyyy xxxx -(narrow)-> xxxx
                // ==> no-op
                return other.getValue();
            }
            else if (getResultBits() < other.getInputBits())
            {
                // yyyyxxxx -(extend)-> zzzzzzzz yyyyxxxx -(narrow)-> xxxx
                // ==> yyyyxxxx -(narrow)-> xxxx
                return new NarrowNode(other.getValue(), other.getInputBits(), getResultBits());
            }
            else
            {
                if (other instanceof SignExtendNode)
                {
                    // sxxx -(sign-extend)-> ssssssss sssssxxx -(narrow)-> sssssxxx
                    // ==> sxxx -(sign-extend)-> sssssxxx
                    return SignExtendNode.create(other.getValue(), other.getInputBits(), getResultBits(), view);
                }
                else if (other instanceof ZeroExtendNode)
                {
                    // xxxx -(zero-extend)-> 00000000 0000xxxx -(narrow)-> 0000xxxx
                    // ==> xxxx -(zero-extend)-> 0000xxxx
                    return new ZeroExtendNode(other.getValue(), other.getInputBits(), getResultBits(), ((ZeroExtendNode) other).isInputAlwaysPositive());
                }
            }
        }
        else if (forValue instanceof AndNode)
        {
            AndNode andNode = (AndNode) forValue;
            IntegerStamp yStamp = (IntegerStamp) andNode.getY().stamp(view);
            IntegerStamp xStamp = (IntegerStamp) andNode.getX().stamp(view);
            long relevantMask = CodeUtil.mask(this.getResultBits());
            if ((relevantMask & yStamp.downMask()) == relevantMask)
            {
                return create(andNode.getX(), this.getResultBits(), view);
            }
            else if ((relevantMask & xStamp.downMask()) == relevantMask)
            {
                return create(andNode.getY(), this.getResultBits(), view);
            }
        }

        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitNarrow(nodeValueMap.operand(getValue()), getResultBits()));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return false;
    }
}
