package graalvm.compiler.nodes.calc;

import java.util.function.Function;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.IntegerConvertOp;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.ArithmeticOperation;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.StampInverter;

/**
 * An {@code IntegerConvert} converts an integer to an integer of different width.
 */
public abstract class IntegerConvertNode<OP, REV> extends UnaryNode implements ArithmeticOperation, ConvertNode, ArithmeticLIRLowerable, StampInverter
{
    @SuppressWarnings("rawtypes") public static final NodeClass<IntegerConvertNode> TYPE = NodeClass.create(IntegerConvertNode.class);

    protected final SerializableIntegerConvertFunction<OP> getOp;
    protected final SerializableIntegerConvertFunction<REV> getReverseOp;

    protected final int inputBits;
    protected final int resultBits;

    protected interface SerializableIntegerConvertFunction<T> extends Function<ArithmeticOpTable, IntegerConvertOp<T>>
    {
    }

    protected IntegerConvertNode(NodeClass<? extends IntegerConvertNode<OP, REV>> c, SerializableIntegerConvertFunction<OP> getOp, SerializableIntegerConvertFunction<REV> getReverseOp, int inputBits, int resultBits, ValueNode input)
    {
        super(c, getOp.apply(ArithmeticOpTable.forStamp(input.stamp(NodeView.DEFAULT))).foldStamp(inputBits, resultBits, input.stamp(NodeView.DEFAULT)), input);
        this.getOp = getOp;
        this.getReverseOp = getReverseOp;
        this.inputBits = inputBits;
        this.resultBits = resultBits;
    }

    public int getInputBits()
    {
        return inputBits;
    }

    public int getResultBits()
    {
        return resultBits;
    }

    protected final IntegerConvertOp<OP> getOp(ValueNode forValue)
    {
        return getOp.apply(ArithmeticOpTable.forStamp(forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final IntegerConvertOp<OP> getArithmeticOp()
    {
        return getOp(getValue());
    }

    @Override
    public Constant convert(Constant c, ConstantReflectionProvider constantReflection)
    {
        return getArithmeticOp().foldConstant(getInputBits(), getResultBits(), c);
    }

    @Override
    public Constant reverse(Constant c, ConstantReflectionProvider constantReflection)
    {
        IntegerConvertOp<REV> reverse = getReverseOp.apply(ArithmeticOpTable.forStamp(stamp(NodeView.DEFAULT)));
        return reverse.foldConstant(getResultBits(), getInputBits(), c);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return getArithmeticOp().foldStamp(inputBits, resultBits, newStamp);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode synonym = findSynonym(getOp(forValue), forValue, inputBits, resultBits, stamp(NodeView.DEFAULT));
        if (synonym != null)
        {
            return synonym;
        }
        return this;
    }

    protected static <T> ValueNode findSynonym(IntegerConvertOp<T> operation, ValueNode value, int inputBits, int resultBits, Stamp stamp)
    {
        if (inputBits == resultBits)
        {
            return value;
        }
        else if (value.isConstant())
        {
            return ConstantNode.forPrimitive(stamp, operation.foldConstant(inputBits, resultBits, value.asConstant()));
        }
        return null;
    }

    public static ValueNode convert(ValueNode input, Stamp stamp, NodeView view)
    {
        return convert(input, stamp, false, view);
    }

    public static ValueNode convert(ValueNode input, Stamp stamp, StructuredGraph graph, NodeView view)
    {
        ValueNode convert = convert(input, stamp, false, view);
        if (!convert.isAlive())
        {
            convert = graph.addOrUniqueWithInputs(convert);
        }
        return convert;
    }

    public static ValueNode convertUnsigned(ValueNode input, Stamp stamp, NodeView view)
    {
        return convert(input, stamp, true, view);
    }

    public static ValueNode convertUnsigned(ValueNode input, Stamp stamp, StructuredGraph graph, NodeView view)
    {
        ValueNode convert = convert(input, stamp, true, view);
        if (!convert.isAlive())
        {
            convert = graph.addOrUniqueWithInputs(convert);
        }
        return convert;
    }

    public static ValueNode convert(ValueNode input, Stamp stamp, boolean zeroExtend, NodeView view)
    {
        IntegerStamp fromStamp = (IntegerStamp) input.stamp(view);
        IntegerStamp toStamp = (IntegerStamp) stamp;

        ValueNode result;
        if (toStamp.getBits() == fromStamp.getBits())
        {
            result = input;
        }
        else if (toStamp.getBits() < fromStamp.getBits())
        {
            result = new NarrowNode(input, fromStamp.getBits(), toStamp.getBits());
        }
        else if (zeroExtend)
        {
            // toStamp.getBits() > fromStamp.getBits()
            result = ZeroExtendNode.create(input, toStamp.getBits(), view);
        }
        else
        {
            // toStamp.getBits() > fromStamp.getBits()
            result = SignExtendNode.create(input, toStamp.getBits(), view);
        }

        IntegerStamp resultStamp = (IntegerStamp) result.stamp(view);
        return result;
    }

    @Override
    public Stamp invertStamp(Stamp outStamp)
    {
        return getArithmeticOp().invertStamp(inputBits, resultBits, outStamp);
    }
}
