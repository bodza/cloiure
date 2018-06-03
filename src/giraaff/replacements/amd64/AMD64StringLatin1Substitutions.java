package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaKind;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.ArrayCompareToNode;

///
// Substitutions for {@code java.lang.StringLatin1} methods.
///
@ClassSubstitution(className = "java.lang.StringLatin1", optional = true)
// @class AMD64StringLatin1Substitutions
public final class AMD64StringLatin1Substitutions
{
    ///
    // @param value is byte[]
    // @param other is byte[]
    ///
    @MethodSubstitution
    public static int compareTo(byte[] __value, byte[] __other)
    {
        return ArrayCompareToNode.compareTo(__value, __other, __value.length, __other.length, JavaKind.Byte, JavaKind.Byte);
    }

    ///
    // @param value is byte[]
    // @param other is char[]
    ///
    @MethodSubstitution
    public static int compareToUTF16(byte[] __value, byte[] __other)
    {
        return ArrayCompareToNode.compareTo(__value, __other, __value.length, __other.length, JavaKind.Byte, JavaKind.Char);
    }
}
