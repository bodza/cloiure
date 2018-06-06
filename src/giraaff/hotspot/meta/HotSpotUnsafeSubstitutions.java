package giraaff.hotspot.meta;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.word.Word;

@ClassSubstitution(className = { "jdk.internal.misc.Unsafe", "sun.misc.Unsafe" })
// @class HotSpotUnsafeSubstitutions
public final class HotSpotUnsafeSubstitutions
{
    // @cons HotSpotUnsafeSubstitutions
    private HotSpotUnsafeSubstitutions()
    {
        super();
    }

    // @def
    public static final String copyMemoryName = "copyMemory0";

    @SuppressWarnings("unused")
    @MethodSubstitution(isStatic = false)
    static void copyMemory(Object __receiver, Object __srcBase, long __srcOffset, Object __destBase, long __destOffset, long __bytes)
    {
        Word __srcAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__srcBase, __srcOffset));
        Word __dstAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(__destBase, __destOffset));
        Word __size = WordFactory.signed(__bytes);
        HotSpotBackend.unsafeArraycopy(__srcAddr, __dstAddr, __size);
    }
}
