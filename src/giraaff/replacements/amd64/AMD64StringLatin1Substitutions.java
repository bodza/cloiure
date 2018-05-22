package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaKind;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.ArrayCompareToNode;

/**
 * Substitutions for {@code java.lang.StringLatin1} methods.
 *
 * Since JDK 9.
 */
@ClassSubstitution(className = "java.lang.StringLatin1", optional = true)
public class AMD64StringLatin1Substitutions
{
    /**
     * @param value is byte[]
     * @param other is byte[]
     */
    @MethodSubstitution
    public static int compareTo(byte[] value, byte[] other)
    {
        return ArrayCompareToNode.compareTo(value, other, value.length, other.length, JavaKind.Byte, JavaKind.Byte);
    }

    /**
     * @param value is byte[]
     * @param other is char[]
     */
    @MethodSubstitution
    public static int compareToUTF16(byte[] value, byte[] other)
    {
        return ArrayCompareToNode.compareTo(value, other, value.length, other.length, JavaKind.Byte, JavaKind.Char);
    }
}
