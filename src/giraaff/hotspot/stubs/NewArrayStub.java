package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.NewObjectSnippets;
import giraaff.hotspot.stubs.NewInstanceStub;
import giraaff.hotspot.stubs.StubUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.options.OptionValues;
import giraaff.word.Word;

/**
 * Stub implementing the fast path for TLAB refill during instance class allocation. This stub is
 * called from the {@linkplain NewObjectSnippets inline} allocation code when TLAB allocation fails.
 * If this stub fails to refill the TLAB or allocate the object, it calls out to the HotSpot C++
 * runtime to complete the allocation.
 */
// @class NewArrayStub
public final class NewArrayStub extends SnippetStub
{
    // @cons
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
     * Re-attempts allocation after an initial TLAB allocation failed or was skipped (e.g. due to -XX:-UseTLAB).
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
        int log2ElementSize = (layoutHelper >> HotSpotRuntime.layoutHelperLog2ElementSizeShift) & HotSpotRuntime.layoutHelperLog2ElementSizeMask;
        int headerSize = (layoutHelper >> HotSpotRuntime.layoutHelperHeaderSizeShift) & HotSpotRuntime.layoutHelperHeaderSizeMask;
        int elementKind = (layoutHelper >> HotSpotRuntime.layoutHelperElementTypeShift) & HotSpotRuntime.layoutHelperElementTypeMask;
        int sizeInBytes = HotSpotReplacementsUtil.arrayAllocationSize(length, headerSize, log2ElementSize);

        // check that array length is small enough for fast path.
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        if (GraalHotSpotVMConfigNode.inlineContiguousAllocationSupported() && length >= 0 && length <= NewObjectSnippets.MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)
        {
            Word memory = NewInstanceStub.refillAllocate(thread, intArrayHub, sizeInBytes);
            if (memory.notEqual(0))
            {
                return NewObjectSnippets.formatArray(hub, sizeInBytes, length, headerSize, memory, WordFactory.unsigned(HotSpotRuntime.arrayPrototypeMarkWord), fillContents, false);
            }
        }

        newArrayC(NEW_ARRAY_C, thread, hub, length);
        StubUtil.handlePendingException(thread, true);
        return HotSpotReplacementsUtil.getAndClearObjectResult(thread);
    }

    public static final ForeignCallDescriptor NEW_ARRAY_C = StubUtil.newDescriptor(NewArrayStub.class, "newArrayC", void.class, Word.class, KlassPointer.class, int.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    public static native void newArrayC(@ConstantNodeParameter ForeignCallDescriptor newArrayC, Word thread, KlassPointer hub, int length);
}
