package giraaff.hotspot.nodes;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.hotspot.nodes.type.HotSpotNarrowOopStamp;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.util.GraalError;

// @class HotSpotCompressionNode
public final class HotSpotCompressionNode extends CompressionNode
{
    // @def
    public static final NodeClass<HotSpotCompressionNode> TYPE = NodeClass.create(HotSpotCompressionNode.class);

    // @cons HotSpotCompressionNode
    public HotSpotCompressionNode(CompressionNode.CompressionOp __op, ValueNode __input, CompressEncoding __encoding)
    {
        super(TYPE, __op, __input, HotSpotNarrowOopStamp.mkStamp(__op, __input.stamp(NodeView.DEFAULT), __encoding), __encoding);
    }

    public static HotSpotCompressionNode compress(ValueNode __input, CompressEncoding __encoding)
    {
        return __input.graph().unique(new HotSpotCompressionNode(CompressionNode.CompressionOp.Compress, __input, __encoding));
    }

    public static CompressionNode uncompress(ValueNode __input, CompressEncoding __encoding)
    {
        return __input.graph().unique(new HotSpotCompressionNode(CompressionNode.CompressionOp.Uncompress, __input, __encoding));
    }

    @Override
    protected Constant compress(Constant __c)
    {
        if (JavaConstant.NULL_POINTER.equals(__c))
        {
            return HotSpotCompressedNullConstant.COMPRESSED_NULL;
        }
        else if (__c instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) __c).compress();
        }
        else
        {
            throw GraalError.shouldNotReachHere("invalid constant input for compress op: " + __c);
        }
    }

    @Override
    protected Constant uncompress(Constant __c)
    {
        if (__c instanceof HotSpotConstant)
        {
            return ((HotSpotConstant) __c).uncompress();
        }
        else
        {
            throw GraalError.shouldNotReachHere("invalid constant input for uncompress op: " + __c);
        }
    }

    @Override
    protected Stamp mkStamp(Stamp __input)
    {
        return HotSpotNarrowOopStamp.mkStamp(this.___op, __input, this.___encoding);
    }
}
