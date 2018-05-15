package graalvm.compiler.hotspot.meta;

import static graalvm.compiler.serviceprovider.GraalServices.Java8OrEarlier;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.nodes.ComputeObjectAddressNode;
import graalvm.compiler.word.Word;
import org.graalvm.word.WordFactory;

@ClassSubstitution(className = {"jdk.internal.misc.Unsafe", "sun.misc.Unsafe"})
public class HotSpotUnsafeSubstitutions {

    public static final String copyMemoryName = Java8OrEarlier ? "copyMemory" : "copyMemory0";

    @SuppressWarnings("unused")
    @MethodSubstitution(isStatic = false)
    static void copyMemory(Object receiver, Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        Word srcAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(srcBase, srcOffset));
        Word dstAddr = WordFactory.unsigned(ComputeObjectAddressNode.get(destBase, destOffset));
        Word size = WordFactory.signed(bytes);
        HotSpotBackend.unsafeArraycopy(srcAddr, dstAddr, size);
    }
}
