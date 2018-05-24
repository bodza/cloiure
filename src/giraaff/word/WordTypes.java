package giraaff.word;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.WordBase;
import org.graalvm.word.WordFactory;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.nodes.ValueNode;
import giraaff.nodes.type.StampTool;
import giraaff.word.Word.Operation;

/**
 * Encapsulates information for Java types representing raw words (as opposed to Objects).
 */
public class WordTypes
{
    /**
     * Resolved type for {@link WordBase}.
     */
    private final ResolvedJavaType wordBaseType;

    /**
     * Resolved type for {@link Word}.
     */
    private final ResolvedJavaType wordImplType;

    /**
     * Resolved type for {@link WordFactory}.
     */
    private final ResolvedJavaType wordFactoryType;

    /**
     * Resolved type for {@link ObjectAccess}.
     */
    private final ResolvedJavaType objectAccessType;

    /**
     * Resolved type for {@link BarrieredAccess}.
     */
    private final ResolvedJavaType barrieredAccessType;

    private final JavaKind wordKind;

    public WordTypes(MetaAccessProvider metaAccess, JavaKind wordKind)
    {
        this.wordKind = wordKind;
        this.wordBaseType = metaAccess.lookupJavaType(WordBase.class);
        this.wordImplType = metaAccess.lookupJavaType(Word.class);
        this.wordFactoryType = metaAccess.lookupJavaType(WordFactory.class);
        this.objectAccessType = metaAccess.lookupJavaType(ObjectAccess.class);
        this.barrieredAccessType = metaAccess.lookupJavaType(BarrieredAccess.class);

        Word.ensureInitialized();
        this.wordImplType.initialize();
    }

    /**
     * Determines if a given method denotes a word operation.
     */
    public boolean isWordOperation(ResolvedJavaMethod targetMethod)
    {
        final boolean isWordFactory = wordFactoryType.equals(targetMethod.getDeclaringClass());
        if (isWordFactory)
        {
            return true;
        }
        final boolean isObjectAccess = objectAccessType.equals(targetMethod.getDeclaringClass());
        final boolean isBarrieredAccess = barrieredAccessType.equals(targetMethod.getDeclaringClass());
        if (isObjectAccess || isBarrieredAccess)
        {
            return true;
        }
        return isWord(targetMethod.getDeclaringClass());
    }

    /**
     * Gets the method annotated with {@link Operation} based on a given method that represents a
     * word operation (but may not necessarily have the annotation).
     *
     * @param callingContextType the {@linkplain ResolvedJavaType type} from which
     *            {@code targetMethod} is invoked
     * @return the {@link Operation} method resolved for {@code targetMethod} if any
     */
    public ResolvedJavaMethod getWordOperation(ResolvedJavaMethod targetMethod, ResolvedJavaType callingContextType)
    {
        final boolean isWordBase = wordBaseType.isAssignableFrom(targetMethod.getDeclaringClass());
        ResolvedJavaMethod wordMethod = targetMethod;
        if (isWordBase && !targetMethod.isStatic())
        {
            wordMethod = wordImplType.resolveConcreteMethod(targetMethod, callingContextType);
        }
        return wordMethod;
    }

    /**
     * Determines if a given node has a word type.
     */
    public boolean isWord(ValueNode node)
    {
        return isWord(StampTool.typeOrNull(node));
    }

    /**
     * Determines if a given type is a word type.
     */
    public boolean isWord(JavaType type)
    {
        return type instanceof ResolvedJavaType && wordBaseType.isAssignableFrom((ResolvedJavaType) type);
    }

    /**
     * Gets the kind for a given type, returning the {@linkplain #getWordKind() word kind} if
     * {@code type} is a {@linkplain #isWord(JavaType) word type}.
     */
    public JavaKind asKind(JavaType type)
    {
        if (isWord(type))
        {
            return wordKind;
        }
        else
        {
            return type.getJavaKind();
        }
    }

    public JavaKind getWordKind()
    {
        return wordKind;
    }

    /**
     * Gets the stamp for a given {@linkplain #isWord(JavaType) word type}.
     */
    public Stamp getWordStamp(ResolvedJavaType type)
    {
        return StampFactory.forKind(wordKind);
    }

    public ResolvedJavaType getWordImplType()
    {
        return wordImplType;
    }
}