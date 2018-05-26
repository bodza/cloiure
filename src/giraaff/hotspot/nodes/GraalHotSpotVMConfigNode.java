package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Fold.InjectedParameter;
import giraaff.core.common.GraalOptions;
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

    private final GraalHotSpotVMConfig config;
    protected final int markId;

    /**
     * Constructor for {@link #areConfigValuesConstant()}.
     */
    public GraalHotSpotVMConfigNode(@InjectedNodeParameter GraalHotSpotVMConfig config)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Boolean));
        this.config = config;
        this.markId = 0;
    }

    /**
     * Constructor for node intrinsics below.
     *
     * @param markId id of the config value
     */
    public GraalHotSpotVMConfigNode(@InjectedNodeParameter Stamp stamp, @InjectedNodeParameter GraalHotSpotVMConfig config, int markId)
    {
        super(TYPE, stamp);
        this.config = config;
        this.markId = markId;
    }

    /**
     * Constructor with explicit type specification.
     *
     * @param markId id of the config value
     * @param kind explicit type of the node
     */
    public GraalHotSpotVMConfigNode(GraalHotSpotVMConfig config, int markId, JavaKind kind)
    {
        super(TYPE, StampFactory.forKind(kind));
        this.config = config;
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
        return loadLongConfigValue(cardTableAddressMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    public static boolean isCardTableAddressConstant()
    {
        return areConfigValuesConstant();
    }

    public static long heapTopAddress()
    {
        return loadLongConfigValue(heapTopAddressMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    public static long heapEndAddress()
    {
        return loadLongConfigValue(heapEndAddressMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    public static long crcTableAddress()
    {
        return loadLongConfigValue(crcTableAddressMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    public static int logOfHeapRegionGrainBytes()
    {
        return loadIntConfigValue(logOfHeapRegionGrainBytesMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    public static boolean inlineContiguousAllocationSupported()
    {
        return loadByteConfigValue(inlineContiguousAllocationSupportedMark(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) != 0;
    }

    @Fold
    public static int cardTableAddressMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_CARD_TABLE_ADDRESS;
    }

    @Fold
    public static int heapTopAddressMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_HEAP_TOP_ADDRESS;
    }

    @Fold
    public static int heapEndAddressMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_HEAP_END_ADDRESS;
    }

    @Fold
    public static int crcTableAddressMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_CRC_TABLE_ADDRESS;
    }

    @Fold
    public static int logOfHeapRegionGrainBytesMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_LOG_OF_HEAP_REGION_GRAIN_BYTES;
    }

    @Fold
    public static int inlineContiguousAllocationSupportedMark(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.MARKID_INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED;
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (markId == 0)
        {
            return ConstantNode.forBoolean(true);
        }
        if (markId == config.MARKID_CARD_TABLE_ADDRESS)
        {
            return ConstantNode.forLong(config.cardtableStartAddress);
        }
        else if (markId == config.MARKID_HEAP_TOP_ADDRESS)
        {
            return ConstantNode.forLong(config.heapTopAddress);
        }
        else if (markId == config.MARKID_HEAP_END_ADDRESS)
        {
            return ConstantNode.forLong(config.heapEndAddress);
        }
        else if (markId == config.MARKID_CRC_TABLE_ADDRESS)
        {
            return ConstantNode.forLong(config.crcTableAddress);
        }
        else if (markId == config.MARKID_LOG_OF_HEAP_REGION_GRAIN_BYTES)
        {
            return ConstantNode.forInt(config.logOfHRGrainBytes);
        }
        else if (markId == config.MARKID_INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED)
        {
            return ConstantNode.forBoolean(config.inlineContiguousAllocationSupported);
        }
        return this;
    }
}
