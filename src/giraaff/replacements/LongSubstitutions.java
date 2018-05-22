package giraaff.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.BitScanForwardNode;
import giraaff.replacements.nodes.BitScanReverseNode;

@ClassSubstitution(Long.class)
public class LongSubstitutions
{
    @MethodSubstitution
    public static int numberOfLeadingZeros(long i)
    {
        if (i == 0)
        {
            return 64;
        }
        return 63 - BitScanReverseNode.unsafeScan(i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(long i)
    {
        if (i == 0)
        {
            return 64;
        }
        return BitScanForwardNode.unsafeScan(i);
    }
}
