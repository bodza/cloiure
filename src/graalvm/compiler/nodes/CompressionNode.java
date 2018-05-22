package graalvm.compiler.nodes;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.CompressEncoding;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.type.AbstractObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.nodes.calc.ConvertNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.type.StampTool;

/**
 * Compress or uncompress an oop or metaspace pointer.
 */
public abstract class CompressionNode extends UnaryNode implements ConvertNode, LIRLowerable
{
    public static final NodeClass<CompressionNode> TYPE = NodeClass.create(CompressionNode.class);

    public enum CompressionOp
    {
        Compress,
        Uncompress
    }

    protected final CompressionOp op;
    protected final CompressEncoding encoding;

    public CompressionNode(NodeClass<? extends UnaryNode> c, CompressionOp op, ValueNode input, Stamp stamp, CompressEncoding encoding)
    {
        super(c, stamp, input);
        this.op = op;
        this.encoding = encoding;
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return mkStamp(newStamp);
    }

    protected abstract Constant compress(Constant c);

    protected abstract Constant uncompress(Constant c);

    @Override
    public Constant convert(Constant c, ConstantReflectionProvider constantReflection)
    {
        switch (op)
        {
            case Compress:
                return compress(c);
            case Uncompress:
                return uncompress(c);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Constant reverse(Constant c, ConstantReflectionProvider constantReflection)
    {
        switch (op)
        {
            case Compress:
                return uncompress(c);
            case Uncompress:
                return compress(c);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean isLossless()
    {
        return true;
    }

    protected abstract Stamp mkStamp(Stamp input);

    public CompressionOp getOp()
    {
        return op;
    }

    public CompressEncoding getEncoding()
    {
        return encoding;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (forValue.isConstant())
        {
            if (GraalOptions.GeneratePIC.getValue(tool.getOptions()))
            {
                // We always want uncompressed constants
                return this;
            }

            ConstantNode constant = (ConstantNode) forValue;
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), convert(constant.getValue(), tool.getConstantReflection()), constant.getStableDimension(), constant.isDefaultStable(), tool.getMetaAccess());
        }
        else if (forValue instanceof CompressionNode)
        {
            CompressionNode other = (CompressionNode) forValue;
            if (op != other.op && encoding.equals(other.encoding))
            {
                return other.getValue();
            }
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        boolean nonNull;
        if (value.stamp(NodeView.DEFAULT) instanceof AbstractObjectStamp)
        {
            nonNull = StampTool.isPointerNonNull(value.stamp(NodeView.DEFAULT));
        }
        else
        {
            // metaspace pointers are never null
            nonNull = true;
        }

        LIRGeneratorTool tool = gen.getLIRGeneratorTool();
        Value result;
        switch (op)
        {
            case Compress:
                result = tool.emitCompress(gen.operand(value), encoding, nonNull);
                break;
            case Uncompress:
                result = tool.emitUncompress(gen.operand(value), encoding, nonNull);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }

        gen.setResult(this, result);
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
