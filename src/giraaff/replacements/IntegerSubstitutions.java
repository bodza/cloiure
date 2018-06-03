package giraaff.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.BitScanForwardNode;
import giraaff.replacements.nodes.BitScanReverseNode;

@ClassSubstitution(Integer.class)
// @class IntegerSubstitutions
public final class IntegerSubstitutions
{
    @MethodSubstitution
    public static int numberOfLeadingZeros(int __i)
    {
        if (__i == 0)
        {
            return 32;
        }
        return 31 - BitScanReverseNode.unsafeScan(__i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(int __i)
    {
        if (__i == 0)
        {
            return 32;
        }
        return BitScanForwardNode.unsafeScan(__i);
    }
}
