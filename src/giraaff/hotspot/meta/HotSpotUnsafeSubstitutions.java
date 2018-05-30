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
    // @cons
    private HotSpotUnsafeSubstitutions()
    {
        super();
    }

    public static final String copyMemoryName = "copyMemory0";

    @SuppressWarnings("unused")
    @MethodSubstitution(isStatic = false)
    static void copyMemory(Object receiver, Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes)
    {
        Word srcAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(srcBase, srcOffset));
        Word dstAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(destBase, destOffset));
        Word size = WordFactory.signed(bytes);
        HotSpotBackend.unsafeArraycopy(srcAddr, dstAddr, size);
    }
}
