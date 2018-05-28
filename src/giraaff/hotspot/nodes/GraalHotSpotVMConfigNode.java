package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Represents {@link GraalHotSpotVMConfig} values that may change after compilation.
 */
public class GraalHotSpotVMConfigNode extends FloatingNode implements LIRLowerable, Canonicalizable
{
    public static final NodeClass<GraalHotSpotVMConfigNode> TYPE = NodeClass.create(GraalHotSpotVMConfigNode.class);

    protected final int markId;

    /**
     * Constructor for {@link #areConfigValuesConstant()}.
     */
    public GraalHotSpotVMConfigNode()
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean));
        this.markId = 0;
    }

    /**
     * Constructor for node intrinsics below.
     *
     * @param markId id of the config value
     */
    public GraalHotSpotVMConfigNode(@InjectedNodeParameter Stamp stamp, int markId)
    {
        super(TYPE, stamp);
        this.markId = markId;
    }

    /**
     * Constructor with explicit type specification.
     *
     * @param markId id of the config value
     * @param kind explicit type of the node
     */
    public GraalHotSpotVMConfigNode(int markId, JavaKind kind)
    {
        super(TYPE, StampFactory.forKind(kind));
        this.markId = markId;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitLoadConfigValue(markId, gen.getLIRGeneratorTool().getLIRKind(stamp)));
    }

    @NodeIntrinsic
    private static native boolean areConfigValuesConstant();

    @NodeIntrinsic
    private static native long loadLongConfigValue(@ConstantNodeParameter int markId);

    @NodeIntrinsic
    private static native int loadIntConfigValue(@ConstantNodeParameter int markId);

    @NodeIntrinsic
    private static native byte loadByteConfigValue(@ConstantNodeParameter int markId);

    public static long cardTableAddress()
    {
        return loadLongConfigValue(GraalHotSpotVMConfig.cardTableAddressMark);
    }

    public static boolean isCardTableAddressConstant()
    {
        return areConfigValuesConstant();
    }

    public static long heapTopAddress()
    {
        return loadLongConfigValue(GraalHotSpotVMConfig.heapTopAddressMark);
    }

    public static long heapEndAddress()
    {
        return loadLongConfigValue(GraalHotSpotVMConfig.heapEndAddressMark);
    }

    public static long crcTableAddress()
    {
        return loadLongConfigValue(GraalHotSpotVMConfig.crcTableAddressMark);
    }

    public static int logOfHeapRegionGrainBytes()
    {
        return loadIntConfigValue(GraalHotSpotVMConfig.logOfHeapRegionGrainBytesMark);
    }

    public static boolean inlineContiguousAllocationSupported()
    {
        return loadByteConfigValue(GraalHotSpotVMConfig.inlineContiguousAllocationSupportedMark) != 0;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (markId == 0)
        {
            return ConstantNode.forBoolean(true);
        }
        if (markId == GraalHotSpotVMConfig.cardTableAddressMark)
        {
            return ConstantNode.forLong(GraalHotSpotVMConfig.cardTableAddress);
        }
        else if (markId == GraalHotSpotVMConfig.heapTopAddressMark)
        {
            return ConstantNode.forLong(GraalHotSpotVMConfig.heapTopAddress);
        }
        else if (markId == GraalHotSpotVMConfig.heapEndAddressMark)
        {
            return ConstantNode.forLong(GraalHotSpotVMConfig.heapEndAddress);
        }
        else if (markId == GraalHotSpotVMConfig.crcTableAddressMark)
        {
            return ConstantNode.forLong(GraalHotSpotVMConfig.crcTableAddress);
        }
        else if (markId == GraalHotSpotVMConfig.logOfHeapRegionGrainBytesMark)
        {
            return ConstantNode.forInt(GraalHotSpotVMConfig.logOfHeapRegionGrainBytes);
        }
        else if (markId == GraalHotSpotVMConfig.inlineContiguousAllocationSupportedMark)
        {
            return ConstantNode.forBoolean(GraalHotSpotVMConfig.inlineContiguousAllocationSupported);
        }
        return this;
    }
}
