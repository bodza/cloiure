package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaKind;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.ArrayCompareToNode;

///
// Substitutions for {@code java.lang.StringUTF16} methods.
///
@ClassSubstitution(className = "java.lang.StringUTF16", optional = true)
// @class AMD64StringUTF16Substitutions
public final class AMD64StringUTF16Substitutions
{
    ///
    // @param value is char[]
    // @param other is char[]
    ///
    @MethodSubstitution
    public static int compareTo(byte[] __value, byte[] __other)
    {
        return ArrayCompareToNode.compareTo(__value, __other, __value.length, __other.length, JavaKind.Char, JavaKind.Char);
    }

    ///
    // @param value is char[]
    // @param other is byte[]
    ///
    @MethodSubstitution
    public static int compareToLatin1(byte[] __value, byte[] __other)
    {
        // Swapping array arguments because intrinsic expects order to be byte[]/char[] but kind arguments stay in original order.
        return ArrayCompareToNode.compareTo(__other, __value, __other.length, __value.length, JavaKind.Char, JavaKind.Byte);
    }
}
