package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;

import java.util.EnumMap;

import graalvm.compiler.core.common.calc.FloatConvert;
import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.FloatConvertOp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;

/**
 * A {@code FloatConvert} converts between integers and floating point numbers according to Java
 * semantics.
 */
@NodeInfo(cycles = CYCLES_8)
public final class FloatConvertNode extends UnaryArithmeticNode<FloatConvertOp> implements ConvertNode, Lowerable, ArithmeticLIRLowerable {
    public static final NodeClass<FloatConvertNode> TYPE = NodeClass.create(FloatConvertNode.class);

    protected final FloatConvert op;

    private static final EnumMap<FloatConvert, SerializableUnaryFunction<FloatConvertOp>> getOps;
    static {
        getOps = new EnumMap<>(FloatConvert.class);
        for (FloatConvert op : FloatConvert.values()) {
            getOps.put(op, table -> table.getFloatConvert(op));
        }
    }

    public FloatConvertNode(FloatConvert op, ValueNode input) {
        super(TYPE, getOps.get(op), input);
        this.op = op;
    }

    public static ValueNode create(FloatConvert op, ValueNode input, NodeView view) {
        ValueNode synonym = findSynonym(input, ArithmeticOpTable.forStamp(input.stamp(view)).getFloatConvert(op));
        if (synonym != null) {
            return synonym;
        }
        return new FloatConvertNode(op, input);
    }

    public FloatConvert getFloatConvert() {
        return op;
    }

    @Override
    public Constant convert(Constant c, ConstantReflectionProvider constantReflection) {
        return getArithmeticOp().foldConstant(c);
    }

    @Override
    public Constant reverse(Constant c, ConstantReflectionProvider constantReflection) {
        FloatConvertOp reverse = ArithmeticOpTable.forStamp(stamp(NodeView.DEFAULT)).getFloatConvert(op.reverse());
        return reverse.foldConstant(c);
    }

    @Override
    public boolean isLossless() {
        switch (getFloatConvert()) {
            case F2D:
            case I2D:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        ValueNode ret = super.canonical(tool, forValue);
        if (ret != this) {
            return ret;
        }

        if (forValue instanceof FloatConvertNode) {
            FloatConvertNode other = (FloatConvertNode) forValue;
            if (other.isLossless() && other.op == this.op.reverse()) {
                return other.getValue();
            }
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen) {
        nodeValueMap.setResult(this, gen.emitFloatConvert(getFloatConvert(), nodeValueMap.operand(getValue())));
    }

    @Override
    public boolean mayNullCheckSkipConversion() {
        return false;
    }
}