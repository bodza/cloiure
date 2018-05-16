package graalvm.compiler.replacements;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.replacements.nodes.BitScanForwardNode;
import graalvm.compiler.replacements.nodes.BitScanReverseNode;

@ClassSubstitution(Integer.class)
public class IntegerSubstitutions
{
    @MethodSubstitution
    public static int numberOfLeadingZeros(int i)
    {
        if (i == 0)
        {
            return 32;
        }
        return 31 - BitScanReverseNode.unsafeScan(i);
    }

    @MethodSubstitution
    public static int numberOfTrailingZeros(int i)
    {
        if (i == 0)
        {
            return 32;
        }
        return BitScanForwardNode.unsafeScan(i);
    }
}
