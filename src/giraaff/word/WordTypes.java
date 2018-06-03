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
// @class WordTypes
public class WordTypes
{
    /**
     * Resolved type for {@link WordBase}.
     */
    // @field
    private final ResolvedJavaType wordBaseType;

    /**
     * Resolved type for {@link Word}.
     */
    // @field
    private final ResolvedJavaType wordImplType;

    /**
     * Resolved type for {@link WordFactory}.
     */
    // @field
    private final ResolvedJavaType wordFactoryType;

    /**
     * Resolved type for {@link ObjectAccess}.
     */
    // @field
    private final ResolvedJavaType objectAccessType;

    /**
     * Resolved type for {@link BarrieredAccess}.
     */
    // @field
    private final ResolvedJavaType barrieredAccessType;

    // @field
    private final JavaKind wordKind;

    // @cons
    public WordTypes(MetaAccessProvider __metaAccess, JavaKind __wordKind)
    {
        super();
        this.wordKind = __wordKind;
        this.wordBaseType = __metaAccess.lookupJavaType(WordBase.class);
        this.wordImplType = __metaAccess.lookupJavaType(Word.class);
        this.wordFactoryType = __metaAccess.lookupJavaType(WordFactory.class);
        this.objectAccessType = __metaAccess.lookupJavaType(ObjectAccess.class);
        this.barrieredAccessType = __metaAccess.lookupJavaType(BarrieredAccess.class);

        Word.ensureInitialized();
        this.wordImplType.initialize();
    }

    /**
     * Determines if a given method denotes a word operation.
     */
    public boolean isWordOperation(ResolvedJavaMethod __targetMethod)
    {
        final boolean __isWordFactory = wordFactoryType.equals(__targetMethod.getDeclaringClass());
        if (__isWordFactory)
        {
            return true;
        }
        final boolean __isObjectAccess = objectAccessType.equals(__targetMethod.getDeclaringClass());
        final boolean __isBarrieredAccess = barrieredAccessType.equals(__targetMethod.getDeclaringClass());
        if (__isObjectAccess || __isBarrieredAccess)
        {
            return true;
        }
        return isWord(__targetMethod.getDeclaringClass());
    }

    /**
     * Gets the method annotated with {@link Operation} based on a given method that represents a
     * word operation (but may not necessarily have the annotation).
     *
     * @param callingContextType the {@linkplain ResolvedJavaType type} from which
     *            {@code targetMethod} is invoked
     * @return the {@link Operation} method resolved for {@code targetMethod} if any
     */
    public ResolvedJavaMethod getWordOperation(ResolvedJavaMethod __targetMethod, ResolvedJavaType __callingContextType)
    {
        final boolean __isWordBase = wordBaseType.isAssignableFrom(__targetMethod.getDeclaringClass());
        ResolvedJavaMethod __wordMethod = __targetMethod;
        if (__isWordBase && !__targetMethod.isStatic())
        {
            __wordMethod = wordImplType.resolveConcreteMethod(__targetMethod, __callingContextType);
        }
        return __wordMethod;
    }

    /**
     * Determines if a given node has a word type.
     */
    public boolean isWord(ValueNode __node)
    {
        return isWord(StampTool.typeOrNull(__node));
    }

    /**
     * Determines if a given type is a word type.
     */
    public boolean isWord(JavaType __type)
    {
        return __type instanceof ResolvedJavaType && wordBaseType.isAssignableFrom((ResolvedJavaType) __type);
    }

    /**
     * Gets the kind for a given type, returning the {@linkplain #getWordKind() word kind} if
     * {@code type} is a {@linkplain #isWord(JavaType) word type}.
     */
    public JavaKind asKind(JavaType __type)
    {
        if (isWord(__type))
        {
            return wordKind;
        }
        else
        {
            return __type.getJavaKind();
        }
    }

    public JavaKind getWordKind()
    {
        return wordKind;
    }

    /**
     * Gets the stamp for a given {@linkplain #isWord(JavaType) word type}.
     */
    public Stamp getWordStamp(ResolvedJavaType __type)
    {
        return StampFactory.forKind(wordKind);
    }

    public ResolvedJavaType getWordImplType()
    {
        return wordImplType;
    }
}
