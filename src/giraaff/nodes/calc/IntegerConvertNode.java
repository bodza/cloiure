package giraaff.nodes.calc;

import java.util.function.Function;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ArithmeticOperation;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.StampInverter;

///
// An {@code IntegerConvert} converts an integer to an integer of different width.
///
// @class IntegerConvertNode
public abstract class IntegerConvertNode<OP, REV> extends UnaryNode implements ArithmeticOperation, ConvertNode, ArithmeticLIRLowerable, StampInverter
{
    @SuppressWarnings("rawtypes")
    // @def
    public static final NodeClass<IntegerConvertNode> TYPE = NodeClass.create(IntegerConvertNode.class);

    // @field
    protected final SerializableIntegerConvertFunction<OP> ___getOp;
    // @field
    protected final SerializableIntegerConvertFunction<REV> ___getReverseOp;

    // @field
    protected final int ___inputBits;
    // @field
    protected final int ___resultBits;

    // @iface IntegerConvertNode.SerializableIntegerConvertFunction
    protected interface SerializableIntegerConvertFunction<T> extends Function<ArithmeticOpTable, IntegerConvertOp<T>>
    {
    }

    // @cons
    protected IntegerConvertNode(NodeClass<? extends IntegerConvertNode<OP, REV>> __c, SerializableIntegerConvertFunction<OP> __getOp, SerializableIntegerConvertFunction<REV> __getReverseOp, int __inputBits, int __resultBits, ValueNode __input)
    {
        super(__c, __getOp.apply(ArithmeticOpTable.forStamp(__input.stamp(NodeView.DEFAULT))).foldStamp(__inputBits, __resultBits, __input.stamp(NodeView.DEFAULT)), __input);
        this.___getOp = __getOp;
        this.___getReverseOp = __getReverseOp;
        this.___inputBits = __inputBits;
        this.___resultBits = __resultBits;
    }

    public int getInputBits()
    {
        return this.___inputBits;
    }

    public int getResultBits()
    {
        return this.___resultBits;
    }

    protected final IntegerConvertOp<OP> getOp(ValueNode __forValue)
    {
        return this.___getOp.apply(ArithmeticOpTable.forStamp(__forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final IntegerConvertOp<OP> getArithmeticOp()
    {
        return getOp(getValue());
    }

    @Override
    public Constant convert(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        return getArithmeticOp().foldConstant(getInputBits(), getResultBits(), __c);
    }

    @Override
    public Constant reverse(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        IntegerConvertOp<REV> __reverse = this.___getReverseOp.apply(ArithmeticOpTable.forStamp(stamp(NodeView.DEFAULT)));
        return __reverse.foldConstant(getResultBits(), getInputBits(), __c);
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return getArithmeticOp().foldStamp(this.___inputBits, this.___resultBits, __newStamp);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __synonym = findSynonym(getOp(__forValue), __forValue, this.___inputBits, this.___resultBits, stamp(NodeView.DEFAULT));
        if (__synonym != null)
        {
            return __synonym;
        }
        return this;
    }

    protected static <T> ValueNode findSynonym(IntegerConvertOp<T> __operation, ValueNode __value, int __inputBits, int __resultBits, Stamp __stamp)
    {
        if (__inputBits == __resultBits)
        {
            return __value;
        }
        else if (__value.isConstant())
        {
            return ConstantNode.forPrimitive(__stamp, __operation.foldConstant(__inputBits, __resultBits, __value.asConstant()));
        }
        return null;
    }

    public static ValueNode convert(ValueNode __input, Stamp __stamp, NodeView __view)
    {
        return convert(__input, __stamp, false, __view);
    }

    public static ValueNode convert(ValueNode __input, Stamp __stamp, StructuredGraph __graph, NodeView __view)
    {
        ValueNode __convert = convert(__input, __stamp, false, __view);
        if (!__convert.isAlive())
        {
            __convert = __graph.addOrUniqueWithInputs(__convert);
        }
        return __convert;
    }

    public static ValueNode convertUnsigned(ValueNode __input, Stamp __stamp, NodeView __view)
    {
        return convert(__input, __stamp, true, __view);
    }

    public static ValueNode convertUnsigned(ValueNode __input, Stamp __stamp, StructuredGraph __graph, NodeView __view)
    {
        ValueNode __convert = convert(__input, __stamp, true, __view);
        if (!__convert.isAlive())
        {
            __convert = __graph.addOrUniqueWithInputs(__convert);
        }
        return __convert;
    }

    public static ValueNode convert(ValueNode __input, Stamp __stamp, boolean __zeroExtend, NodeView __view)
    {
        IntegerStamp __fromStamp = (IntegerStamp) __input.stamp(__view);
        IntegerStamp __toStamp = (IntegerStamp) __stamp;

        ValueNode __result;
        if (__toStamp.getBits() == __fromStamp.getBits())
        {
            __result = __input;
        }
        else if (__toStamp.getBits() < __fromStamp.getBits())
        {
            __result = new NarrowNode(__input, __fromStamp.getBits(), __toStamp.getBits());
        }
        else if (__zeroExtend)
        {
            // toStamp.getBits() > fromStamp.getBits()
            __result = ZeroExtendNode.create(__input, __toStamp.getBits(), __view);
        }
        else
        {
            // toStamp.getBits() > fromStamp.getBits()
            __result = SignExtendNode.create(__input, __toStamp.getBits(), __view);
        }

        IntegerStamp __resultStamp = (IntegerStamp) __result.stamp(__view);
        return __result;
    }

    @Override
    public Stamp invertStamp(Stamp __outStamp)
    {
        return getArithmeticOp().invertStamp(this.___inputBits, this.___resultBits, __outStamp);
    }
}
