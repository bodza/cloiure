package giraaff.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.BitScanForwardNode;
import giraaff.replacements.nodes.BitScanReverseNode;

@ClassSubstitution(Long.class)
// @class LongSubstitutions
public final class LongSubstitutions
{
    @MethodSubstitution
    public static int numberOfLeadingZeros(long __i)
    {
        if (__i == 0)
        {
            return 64;
        }
        return 63 - BitScanReverseNode.unsafeScan(__i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(long __i)
    {
        if (__i == 0)
        {
            return 64;
        }
        return BitScanForwardNode.unsafeScan(__i);
    }
}
