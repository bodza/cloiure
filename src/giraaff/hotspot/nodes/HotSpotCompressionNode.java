package giraaff.hotspot.nodes;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.type.Stamp;
import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.hotspot.nodes.type.HotSpotNarrowOopStamp;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

public final class HotSpotCompressionNode extends CompressionNode
{
    public static final NodeClass<HotSpotCompressionNode> TYPE = NodeClass.create(HotSpotCompressionNode.class);

    public HotSpotCompressionNode(CompressionOp op, ValueNode input, CompressEncoding encoding)
    {
        super(TYPE, op, input, HotSpotNarrowOopStamp.mkStamp(op, input.stamp(NodeView.DEFAULT), encoding), encoding);
    }

    public static HotSpotCompressionNode compress(ValueNode input, CompressEncoding encoding)
    {
        return input.graph().unique(new HotSpotCompressionNode(CompressionOp.Compress, input, encoding));
    }

    public static CompressionNode uncompress(ValueNode input, CompressEncoding encoding)
    {
        return input.graph().unique(new HotSpotCompressionNode(CompressionOp.Uncompress, input, encoding));
    }

    @Override
    protected Constant compress(Constant c)
    {
        if (JavaConstant.NULL_POINTER.equals(c))
        {
            return HotSpotCompressedNullConstant.COMPRESSED_NULL;
        }
        else if (c instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) c).compress();
        }
        else
        {
            throw GraalError.shouldNotReachHere("invalid constant input for compress op: " + c);
        }
    }

    @Override
    protected Constant uncompress(Constant c)
    {
        if (c instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) c).uncompress();
        }
        else
        {
            throw GraalError.shouldNotReachHere("invalid constant input for uncompress op: " + c);
        }
    }

    @Override
    protected Stamp mkStamp(Stamp input)
    {
        return HotSpotNarrowOopStamp.mkStamp(op, input, encoding);
    }
}
