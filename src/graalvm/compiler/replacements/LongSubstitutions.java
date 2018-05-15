package graalvm.compiler.replacements;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.replacements.nodes.BitScanForwardNode;
import graalvm.compiler.replacements.nodes.BitScanReverseNode;

@ClassSubstitution(Long.class)
public class LongSubstitutions {

    @MethodSubstitution
    public static int numberOfLeadingZeros(long i) {
        if (i == 0) {
            return 64;
        }
        return 63 - BitScanReverseNode.unsafeScan(i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(long i) {
        if (i == 0) {
            return 64;
        }
        return BitScanForwardNode.unsafeScan(i);
    }
}
