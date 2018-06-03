package giraaff.replacements.amd64;

import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.type.ArithmeticOpTable.FloatConvertOp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.calc.UnaryArithmeticNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// This node has the semantics of the AMD64 floating point conversions. It is used in the lowering
// of the {@link FloatConvertNode} which, on AMD64 needs a {@link AMD64FloatConvertNode} plus some
// fixup code that handles the corner cases that differ between AMD64 and Java.
///
// @class AMD64FloatConvertNode
public final class AMD64FloatConvertNode extends UnaryArithmeticNode<FloatConvertOp> implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<AMD64FloatConvertNode> TYPE = NodeClass.create(AMD64FloatConvertNode.class);

    // @field
    protected final FloatConvert ___op;

    // @cons
    public AMD64FloatConvertNode(FloatConvert __op, ValueNode __value)
    {
        super(TYPE, __table -> __table.getFloatConvert(__op), __value);
        this.___op = __op;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        // nothing to do
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitFloatConvert(this.___op, __nodeValueMap.operand(getValue())));
    }
}
