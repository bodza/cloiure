package giraaff.nodes.calc;

import java.util.EnumMap;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;

import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.FloatConvertOp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * A {@code FloatConvert} converts between integers and floating point numbers according to Java semantics.
 */
// @class FloatConvertNode
public final class FloatConvertNode extends UnaryArithmeticNode<FloatConvertOp> implements ConvertNode, Lowerable, ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<FloatConvertNode> TYPE = NodeClass.create(FloatConvertNode.class);

    // @field
    protected final FloatConvert op;

    // @def
    private static final EnumMap<FloatConvert, SerializableUnaryFunction<FloatConvertOp>> getOps;
    static
    {
        getOps = new EnumMap<>(FloatConvert.class);
        for (FloatConvert __op : FloatConvert.values())
        {
            getOps.put(__op, __table -> __table.getFloatConvert(__op));
        }
    }

    // @cons
    public FloatConvertNode(FloatConvert __op, ValueNode __input)
    {
        super(TYPE, getOps.get(__op), __input);
        this.op = __op;
    }

    public static ValueNode create(FloatConvert __op, ValueNode __input, NodeView __view)
    {
        ValueNode __synonym = findSynonym(__input, ArithmeticOpTable.forStamp(__input.stamp(__view)).getFloatConvert(__op));
        if (__synonym != null)
        {
            return __synonym;
        }
        return new FloatConvertNode(__op, __input);
    }

    public FloatConvert getFloatConvert()
    {
        return op;
    }

    @Override
    public Constant convert(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        return getArithmeticOp().foldConstant(__c);
    }

    @Override
    public Constant reverse(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        FloatConvertOp __reverse = ArithmeticOpTable.forStamp(stamp(NodeView.DEFAULT)).getFloatConvert(op.reverse());
        return __reverse.foldConstant(__c);
    }

    @Override
    public boolean isLossless()
    {
        switch (getFloatConvert())
        {
            case F2D:
            case I2D:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __ret = super.canonical(__tool, __forValue);
        if (__ret != this)
        {
            return __ret;
        }

        if (__forValue instanceof FloatConvertNode)
        {
            FloatConvertNode __other = (FloatConvertNode) __forValue;
            if (__other.isLossless() && __other.op == this.op.reverse())
            {
                return __other.getValue();
            }
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitFloatConvert(getFloatConvert(), __nodeValueMap.operand(getValue())));
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return false;
    }
}
