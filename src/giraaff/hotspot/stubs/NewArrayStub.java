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
    public NewArrayStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("newArray", __providers, __linkage);
    }

    @Override
    protected Object[] makeConstArgs()
    {
        HotSpotResolvedObjectType __intArrayType = (HotSpotResolvedObjectType) providers.getMetaAccess().lookupJavaType(int[].class);
        int __count = method.getSignature().getParameterCount(false);
        Object[] __args = new Object[__count];
        __args[3] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __intArrayType.klass(), null);
        __args[4] = providers.getRegisters().getThreadRegister();
        return __args;
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
    private static Object newArray(KlassPointer __hub, int __length, boolean __fillContents, @ConstantParameter KlassPointer __intArrayHub, @ConstantParameter Register __threadRegister)
    {
        int __layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(__hub);
        int __log2ElementSize = (__layoutHelper >> HotSpotRuntime.layoutHelperLog2ElementSizeShift) & HotSpotRuntime.layoutHelperLog2ElementSizeMask;
        int __headerSize = (__layoutHelper >> HotSpotRuntime.layoutHelperHeaderSizeShift) & HotSpotRuntime.layoutHelperHeaderSizeMask;
        int __elementKind = (__layoutHelper >> HotSpotRuntime.layoutHelperElementTypeShift) & HotSpotRuntime.layoutHelperElementTypeMask;
        int __sizeInBytes = HotSpotReplacementsUtil.arrayAllocationSize(__length, __headerSize, __log2ElementSize);

        // check that array length is small enough for fast path.
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        if (GraalHotSpotVMConfigNode.inlineContiguousAllocationSupported() && __length >= 0 && __length <= NewObjectSnippets.MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)
        {
            Word __memory = NewInstanceStub.refillAllocate(__thread, __intArrayHub, __sizeInBytes);
            if (__memory.notEqual(0))
            {
                return NewObjectSnippets.formatArray(__hub, __sizeInBytes, __length, __headerSize, __memory, WordFactory.unsigned(HotSpotRuntime.arrayPrototypeMarkWord), __fillContents, false);
            }
        }

        newArrayC(NEW_ARRAY_C, __thread, __hub, __length);
        StubUtil.handlePendingException(__thread, true);
        return HotSpotReplacementsUtil.getAndClearObjectResult(__thread);
    }

    // @def
    public static final ForeignCallDescriptor NEW_ARRAY_C = StubUtil.newDescriptor(NewArrayStub.class, "newArrayC", void.class, Word.class, KlassPointer.class, int.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    public static native void newArrayC(@ConstantNodeParameter ForeignCallDescriptor newArrayC, Word thread, KlassPointer hub, int length);
}
