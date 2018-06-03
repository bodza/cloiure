package giraaff.nodes;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.type.AbstractObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.nodes.calc.ConvertNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;

///
// Compress or uncompress an oop or metaspace pointer.
///
// @class CompressionNode
public abstract class CompressionNode extends UnaryNode implements ConvertNode, LIRLowerable
{
    // @def
    public static final NodeClass<CompressionNode> TYPE = NodeClass.create(CompressionNode.class);

    // @enum CompressionNode.CompressionOp
    public enum CompressionOp
    {
        Compress,
        Uncompress
    }

    // @field
    protected final CompressionOp ___op;
    // @field
    protected final CompressEncoding ___encoding;

    // @cons
    public CompressionNode(NodeClass<? extends UnaryNode> __c, CompressionOp __op, ValueNode __input, Stamp __stamp, CompressEncoding __encoding)
    {
        super(__c, __stamp, __input);
        this.___op = __op;
        this.___encoding = __encoding;
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return mkStamp(__newStamp);
    }

    protected abstract Constant compress(Constant __c);

    protected abstract Constant uncompress(Constant __c);

    @Override
    public Constant convert(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        switch (this.___op)
        {
            case Compress:
                return compress(__c);
            case Uncompress:
                return uncompress(__c);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Constant reverse(Constant __c, ConstantReflectionProvider __constantReflection)
    {
        switch (this.___op)
        {
            case Compress:
                return uncompress(__c);
            case Uncompress:
                return compress(__c);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean isLossless()
    {
        return true;
    }

    protected abstract Stamp mkStamp(Stamp __input);

    public CompressionOp getOp()
    {
        return this.___op;
    }

    public CompressEncoding getEncoding()
    {
        return this.___encoding;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__forValue.isConstant())
        {
            ConstantNode __constant = (ConstantNode) __forValue;
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), convert(__constant.getValue(), __tool.getConstantReflection()), __constant.getStableDimension(), __constant.isDefaultStable(), __tool.getMetaAccess());
        }
        else if (__forValue instanceof CompressionNode)
        {
            CompressionNode __other = (CompressionNode) __forValue;
            if (this.___op != __other.___op && this.___encoding.equals(__other.___encoding))
            {
                return __other.getValue();
            }
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        boolean __nonNull;
        if (this.___value.stamp(NodeView.DEFAULT) instanceof AbstractObjectStamp)
        {
            __nonNull = StampTool.isPointerNonNull(this.___value.stamp(NodeView.DEFAULT));
        }
        else
        {
            // metaspace pointers are never null
            __nonNull = true;
        }

        LIRGeneratorTool __tool = __gen.getLIRGeneratorTool();
        Value __result;
        switch (this.___op)
        {
            case Compress:
            {
                __result = __tool.emitCompress(__gen.operand(this.___value), this.___encoding, __nonNull);
                break;
            }
            case Uncompress:
            {
                __result = __tool.emitUncompress(__gen.operand(this.___value), this.___encoding, __nonNull);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }

        __gen.setResult(this, __result);
    }

    @Override
    public boolean mayNullCheckSkipConversion()
    {
        return true;
    }
}
