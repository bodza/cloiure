package giraaff.nodes.calc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.SerializableConstant;

import giraaff.core.common.LIRKind;
import giraaff.core.common.type.ArithmeticStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// The {@code ReinterpretNode} class represents a reinterpreting conversion that changes the stamp
// of a primitive value to some other incompatible stamp. The new stamp must have the same width as
// the old stamp.
///
// @class ReinterpretNode
public final class ReinterpretNode extends UnaryNode implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<ReinterpretNode> TYPE = NodeClass.create(ReinterpretNode.class);

    // @cons ReinterpretNode
    protected ReinterpretNode(JavaKind __to, ValueNode __value)
    {
        this(StampFactory.forKind(__to), __value);
    }

    // @cons ReinterpretNode
    protected ReinterpretNode(Stamp __to, ValueNode __value)
    {
        super(TYPE, getReinterpretStamp(__to, __value.stamp(NodeView.DEFAULT)), __value);
    }

    public static ValueNode create(JavaKind __to, ValueNode __value, NodeView __view)
    {
        return create(StampFactory.forKind(__to), __value, __view);
    }

    public static ValueNode create(Stamp __to, ValueNode __value, NodeView __view)
    {
        return canonical(null, __to, __value, __view);
    }

    private static SerializableConstant evalConst(Stamp __stamp, SerializableConstant __c)
    {
        // We don't care about byte order here. Either would produce the correct result.
        ByteBuffer __buffer = ByteBuffer.wrap(new byte[__c.getSerializedSize()]).order(ByteOrder.nativeOrder());
        __c.serialize(__buffer);

        __buffer.rewind();
        return ((ArithmeticStamp) __stamp).deserialize(__buffer);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        NodeView __view = NodeView.from(__tool);
        return canonical(this, this.stamp(__view), __forValue, __view);
    }

    public static ValueNode canonical(ReinterpretNode __node, Stamp __forStamp, ValueNode __forValue, NodeView __view)
    {
        if (__forValue.isConstant())
        {
            return ConstantNode.forConstant(__forStamp, evalConst(__forStamp, (SerializableConstant) __forValue.asConstant()), null);
        }
        if (__forStamp.isCompatible(__forValue.stamp(__view)))
        {
            return __forValue;
        }
        if (__forValue instanceof ReinterpretNode)
        {
            ReinterpretNode __reinterpret = (ReinterpretNode) __forValue;
            return new ReinterpretNode(__forStamp, __reinterpret.getValue());
        }
        return __node != null ? __node : new ReinterpretNode(__forStamp, __forValue);
    }

    private static Stamp getReinterpretStamp(Stamp __toStamp, Stamp __fromStamp)
    {
        return __toStamp;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(getReinterpretStamp(stamp(NodeView.DEFAULT), getValue().stamp(NodeView.DEFAULT)));
    }

    @Override
    public void generate(NodeLIRBuilderTool __builder, ArithmeticLIRGeneratorTool __gen)
    {
        LIRKind __kind = __builder.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT));
        __builder.setResult(this, __gen.emitReinterpret(__kind, __builder.operand(getValue())));
    }

    public static ValueNode reinterpret(JavaKind __toKind, ValueNode __value)
    {
        return __value.graph().unique(new ReinterpretNode(__toKind, __value));
    }
}
