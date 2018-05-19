package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

@NodeInfo(cycles = CYCLES_1, size = SIZE_1)
public final class ReverseBytesNode extends UnaryNode implements LIRLowerable
{
    public static final NodeClass<ReverseBytesNode> TYPE = NodeClass.create(ReverseBytesNode.class);

    public ReverseBytesNode(ValueNode value)
    {
        super(TYPE, StampFactory.forKind(value.getStackKind()), value);
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        if (getStackKind() == JavaKind.Int)
        {
            long mask = CodeUtil.mask(JavaKind.Int.getBitCount());
            return IntegerStamp.stampForMask(valueStamp.getBits(), Integer.reverse((int) valueStamp.downMask()) & mask, Integer.reverse((int) valueStamp.upMask()) & mask);
        }
        else if (getStackKind() == JavaKind.Long)
        {
            return IntegerStamp.stampForMask(valueStamp.getBits(), Long.reverse(valueStamp.downMask()), Long.reverse(valueStamp.upMask()));
        }
        else
        {
            return stamp(NodeView.DEFAULT);
        }
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            JavaConstant c = forValue.asJavaConstant();
            long reversed = getStackKind() == JavaKind.Int ? Integer.reverseBytes(c.asInt()) : Long.reverseBytes(c.asLong());
            return ConstantNode.forIntegerKind(getStackKind(), reversed);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result = gen.getLIRGeneratorTool().emitByteSwap(gen.operand(getValue()));
        gen.setResult(this, result);
    }
}
