package graalvm.compiler.replacements.amd64;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.type.StampTool;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Count the number of leading zeros using the {@code lzcntq} or {@code lzcntl} instructions.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class AMD64CountLeadingZerosNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<AMD64CountLeadingZerosNode> TYPE = NodeClass.create(AMD64CountLeadingZerosNode.class);

    public AMD64CountLeadingZerosNode(ValueNode value)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), value), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return computeStamp(newStamp, getValue());
    }

    private static Stamp computeStamp(Stamp newStamp, ValueNode theValue)
    {
        return StampTool.stampForLeadingZeros((IntegerStamp) newStamp);
    }

    public static ValueNode tryFold(ValueNode value)
    {
        if (value.isConstant())
        {
            JavaConstant c = value.asJavaConstant();
            if (value.getStackKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Integer.numberOfLeadingZeros(c.asInt()));
            }
            else
            {
                return ConstantNode.forInt(Long.numberOfLeadingZeros(c.asLong()));
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode folded = tryFold(forValue);
        return folded != null ? folded : this;
    }

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen)
    {
        builder.setResult(this, ((AMD64ArithmeticLIRGeneratorTool) gen).emitCountLeadingZeros(builder.operand(getValue())));
    }
}
