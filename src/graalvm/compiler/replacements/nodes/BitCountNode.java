package graalvm.compiler.replacements.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class BitCountNode extends UnaryNode implements ArithmeticLIRLowerable
{
    public static final NodeClass<BitCountNode> TYPE = NodeClass.create(BitCountNode.class);

    public BitCountNode(ValueNode value)
    {
        super(TYPE, computeStamp(value.stamp(NodeView.DEFAULT), value), value);
        assert value.getStackKind() == JavaKind.Int || value.getStackKind() == JavaKind.Long;
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        ValueNode theValue = getValue();
        return computeStamp(newStamp, theValue);
    }

    static Stamp computeStamp(Stamp newStamp, ValueNode theValue)
    {
        assert newStamp.isCompatible(theValue.stamp(NodeView.DEFAULT));
        IntegerStamp valueStamp = (IntegerStamp) newStamp;
        assert (valueStamp.downMask() & CodeUtil.mask(valueStamp.getBits())) == valueStamp.downMask();
        assert (valueStamp.upMask() & CodeUtil.mask(valueStamp.getBits())) == valueStamp.upMask();
        return StampFactory.forInteger(JavaKind.Int, Long.bitCount(valueStamp.downMask()), Long.bitCount(valueStamp.upMask()));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            JavaConstant c = forValue.asJavaConstant();
            return ConstantNode.forInt(forValue.getStackKind() == JavaKind.Int ? Integer.bitCount(c.asInt()) : Long.bitCount(c.asLong()));
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool builder, ArithmeticLIRGeneratorTool gen)
    {
        builder.setResult(this, gen.emitBitCount(builder.operand(getValue())));
    }
}
