package graalvm.compiler.replacements.amd64;

import graalvm.compiler.core.common.calc.FloatConvert;
import graalvm.compiler.core.common.type.ArithmeticOpTable.FloatConvertOp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatConvertNode;
import graalvm.compiler.nodes.calc.UnaryArithmeticNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This node has the semantics of the AMD64 floating point conversions. It is used in the lowering
 * of the {@link FloatConvertNode} which, on AMD64 needs a {@link AMD64FloatConvertNode} plus some
 * fixup code that handles the corner cases that differ between AMD64 and Java.
 */
public final class AMD64FloatConvertNode extends UnaryArithmeticNode<FloatConvertOp> implements ArithmeticLIRLowerable
{
    public static final NodeClass<AMD64FloatConvertNode> TYPE = NodeClass.create(AMD64FloatConvertNode.class);

    protected final FloatConvert op;

    public AMD64FloatConvertNode(FloatConvert op, ValueNode value)
    {
        super(TYPE, table -> table.getFloatConvert(op), value);
        this.op = op;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        // nothing to do
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitFloatConvert(op, nodeValueMap.operand(getValue())));
    }
}
