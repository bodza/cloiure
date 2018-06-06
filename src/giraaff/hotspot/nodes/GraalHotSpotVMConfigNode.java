package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Represents {@link HotSpotRuntime} values that may change after compilation.
///
// @class GraalHotSpotVMConfigNode
public final class GraalHotSpotVMConfigNode extends FloatingNode implements LIRLowerable, Canonicalizable
{
    // @def
    public static final NodeClass<GraalHotSpotVMConfigNode> TYPE = NodeClass.create(GraalHotSpotVMConfigNode.class);

    // @field
    protected final int ___markId;

    ///
    // Constructor for {@link #areConfigValuesConstant()}.
    ///
    // @cons GraalHotSpotVMConfigNode
    public GraalHotSpotVMConfigNode()
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean));
        this.___markId = 0;
    }

    ///
    // Constructor for node intrinsics below.
    //
    // @param markId id of the config value
    ///
    // @cons GraalHotSpotVMConfigNode
    public GraalHotSpotVMConfigNode(@Node.InjectedNodeParameter Stamp __stamp, int __markId)
    {
        super(TYPE, __stamp);
        this.___markId = __markId;
    }

    ///
    // Constructor with explicit type specification.
    //
    // @param markId id of the config value
    // @param kind explicit type of the node
    ///
    // @cons GraalHotSpotVMConfigNode
    public GraalHotSpotVMConfigNode(int __markId, JavaKind __kind)
    {
        super(TYPE, StampFactory.forKind(__kind));
        this.___markId = __markId;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadConfigValue(this.___markId, __gen.getLIRGeneratorTool().getLIRKind(this.___stamp)));
    }

    @Node.NodeIntrinsic
    private static native boolean areConfigValuesConstant();

    @Node.NodeIntrinsic
    private static native long loadLongConfigValue(@Node.ConstantNodeParameter int __markId);

    @Node.NodeIntrinsic
    private static native int loadIntConfigValue(@Node.ConstantNodeParameter int __markId);

    @Node.NodeIntrinsic
    private static native byte loadByteConfigValue(@Node.ConstantNodeParameter int __markId);

    public static long cardTableAddress()
    {
        return loadLongConfigValue(HotSpotRuntime.cardTableAddressMark);
    }

    public static boolean isCardTableAddressConstant()
    {
        return areConfigValuesConstant();
    }

    public static long heapTopAddress()
    {
        return loadLongConfigValue(HotSpotRuntime.heapTopAddressMark);
    }

    public static long heapEndAddress()
    {
        return loadLongConfigValue(HotSpotRuntime.heapEndAddressMark);
    }

    public static int logOfHeapRegionGrainBytes()
    {
        return loadIntConfigValue(HotSpotRuntime.logOfHeapRegionGrainBytesMark);
    }

    public static boolean inlineContiguousAllocationSupported()
    {
        return loadByteConfigValue(HotSpotRuntime.inlineContiguousAllocationSupportedMark) != 0;
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___markId == 0)
        {
            return ConstantNode.forBoolean(true);
        }
        if (this.___markId == HotSpotRuntime.cardTableAddressMark)
        {
            return ConstantNode.forLong(HotSpotRuntime.cardTableAddress);
        }
        else if (this.___markId == HotSpotRuntime.heapTopAddressMark)
        {
            return ConstantNode.forLong(HotSpotRuntime.heapTopAddress);
        }
        else if (this.___markId == HotSpotRuntime.heapEndAddressMark)
        {
            return ConstantNode.forLong(HotSpotRuntime.heapEndAddress);
        }
        else if (this.___markId == HotSpotRuntime.logOfHeapRegionGrainBytesMark)
        {
            return ConstantNode.forInt(HotSpotRuntime.logOfHeapRegionGrainBytes);
        }
        else if (this.___markId == HotSpotRuntime.inlineContiguousAllocationSupportedMark)
        {
            return ConstantNode.forBoolean(HotSpotRuntime.inlineContiguousAllocationSupported);
        }
        return this;
    }
}
