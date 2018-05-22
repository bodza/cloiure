package giraaff.replacements.amd64;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.Pointer;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Fold.InjectedParameter;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.replacements.StringSubstitutions;
import giraaff.replacements.nodes.ArrayCompareToNode;
import giraaff.word.Word;

/**
 * Substitutions for {@link java.lang.String} methods.
 */
@ClassSubstitution(String.class)
public class AMD64StringSubstitutions
{
    @Fold
    static int charArrayBaseOffset(@InjectedParameter ArrayOffsetProvider arrayOffsetProvider)
    {
        return arrayOffsetProvider.arrayBaseOffset(JavaKind.Char);
    }

    @Fold
    static int charArrayIndexScale(@InjectedParameter ArrayOffsetProvider arrayOffsetProvider)
    {
        return arrayOffsetProvider.arrayScalingFactor(JavaKind.Char);
    }

    /**
     * Marker value for the {@link InjectedParameter} injected parameter.
     */
    static final ArrayOffsetProvider INJECTED = null;

    // Only exists in JDK <= 8
    @MethodSubstitution(isStatic = true, optional = true)
    public static int indexOf(char[] source, int sourceOffset, int sourceCount, @ConstantNodeParameter char[] target, int targetOffset, int targetCount, int origFromIndex)
    {
        int fromIndex = origFromIndex;
        if (fromIndex >= sourceCount)
        {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0)
        {
            fromIndex = 0;
        }
        if (targetCount == 0)
        {
            // The empty string is in every string.
            return fromIndex;
        }

        int totalOffset = sourceOffset + fromIndex;
        if (sourceCount - fromIndex < targetCount)
        {
            // The empty string contains nothing except the empty string.
            return -1;
        }

        Pointer sourcePointer = Word.objectToTrackedPointer(source).add(charArrayBaseOffset(INJECTED)).add(totalOffset * charArrayIndexScale(INJECTED));
        Pointer targetPointer = Word.objectToTrackedPointer(target).add(charArrayBaseOffset(INJECTED)).add(targetOffset * charArrayIndexScale(INJECTED));
        int result = AMD64StringIndexOfNode.optimizedStringIndexPointer(sourcePointer, sourceCount - fromIndex, targetPointer, targetCount);
        if (result >= 0)
        {
            return result + totalOffset;
        }
        return result;
    }

    @MethodSubstitution(isStatic = false)
    public static int compareTo(String receiver, String anotherString)
    {
        if (receiver == anotherString)
        {
            return 0;
        }
        char[] value = StringSubstitutions.getValue(receiver);
        char[] other = StringSubstitutions.getValue(anotherString);
        return ArrayCompareToNode.compareTo(value, other, value.length << 1, other.length << 1, JavaKind.Char, JavaKind.Char);
    }
}
