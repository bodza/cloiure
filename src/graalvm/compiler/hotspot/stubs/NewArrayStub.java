package graalvm.compiler.hotspot.stubs;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import org.graalvm.word.WordFactory;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.GraalHotSpotVMConfigNode;
import graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.replacements.NewObjectSnippets;
import graalvm.compiler.hotspot.stubs.NewInstanceStub;
import graalvm.compiler.hotspot.stubs.StubUtil;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.word.Word;

/**
 * Stub implementing the fast path for TLAB refill during instance class allocation. This stub is
 * called from the {@linkplain NewObjectSnippets inline} allocation code when TLAB allocation fails.
 * If this stub fails to refill the TLAB or allocate the object, it calls out to the HotSpot C++
 * runtime to complete the allocation.
 */
public class NewArrayStub extends SnippetStub
{
    public NewArrayStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("newArray", options, providers, linkage);
    }

    @Override
    protected Object[] makeConstArgs()
    {
        HotSpotResolvedObjectType intArrayType = (HotSpotResolvedObjectType) providers.getMetaAccess().lookupJavaType(int[].class);
        int count = method.getSignature().getParameterCount(false);
        Object[] args = new Object[count];
        args[3] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), intArrayType.klass(), null);
        args[4] = providers.getRegisters().getThreadRegister();
        args[5] = options;
        return args;
    }

    /**
     * Re-attempts allocation after an initial TLAB allocation failed or was skipped (e.g., due to
     * -XX:-UseTLAB).
     *
     * @param hub the hub of the object to be allocated
     * @param length the length of the array
     * @param fillContents Should the array be filled with zeroes?
     * @param intArrayHub the hub for {@code int[].class}
     */
    @Snippet
    private static Object newArray(KlassPointer hub, int length, boolean fillContents, @ConstantParameter KlassPointer intArrayHub, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options)
    {
        int layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(hub);
        int log2ElementSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperLog2ElementSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperLog2ElementSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        int headerSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperHeaderSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperHeaderSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        int elementKind = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperElementTypeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperElementTypeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        int sizeInBytes = HotSpotReplacementsUtil.arrayAllocationSize(length, headerSize, log2ElementSize);

        // check that array length is small enough for fast path.
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        boolean inlineContiguousAllocationSupported = GraalHotSpotVMConfigNode.inlineContiguousAllocationSupported();
        if (inlineContiguousAllocationSupported && !HotSpotReplacementsUtil.useCMSIncrementalMode(GraalHotSpotVMConfig.INJECTED_VMCONFIG) && length >= 0 && length <= NewObjectSnippets.MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)
        {
            Word memory = NewInstanceStub.refillAllocate(thread, intArrayHub, sizeInBytes);
            if (memory.notEqual(0))
            {
                return StubUtil.verifyObject(NewObjectSnippets.formatArray(hub, sizeInBytes, length, headerSize, memory, WordFactory.unsigned(HotSpotReplacementsUtil.arrayPrototypeMarkWord(GraalHotSpotVMConfig.INJECTED_VMCONFIG)), fillContents, false, null));
            }
        }

        newArrayC(NEW_ARRAY_C, thread, hub, length);
        StubUtil.handlePendingException(thread, true);
        return StubUtil.verifyObject(HotSpotReplacementsUtil.getAndClearObjectResult(thread));
    }

    public static final ForeignCallDescriptor NEW_ARRAY_C = StubUtil.newDescriptor(NewArrayStub.class, "newArrayC", void.class, Word.class, KlassPointer.class, int.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    public static native void newArrayC(@ConstantNodeParameter ForeignCallDescriptor newArrayC, Word thread, KlassPointer hub, int length);
}
