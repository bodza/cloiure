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
import giraaff.word.Word;

///
// Encapsulates information for Java types representing raw words (as opposed to Objects).
///
// @class WordTypes
public class WordTypes
{
    ///
    // Resolved type for {@link WordBase}.
    ///
    // @field
    private final ResolvedJavaType ___wordBaseType;

    ///
    // Resolved type for {@link Word}.
    ///
    // @field
    private final ResolvedJavaType ___wordImplType;

    ///
    // Resolved type for {@link WordFactory}.
    ///
    // @field
    private final ResolvedJavaType ___wordFactoryType;

    ///
    // Resolved type for {@link ObjectAccess}.
    ///
    // @field
    private final ResolvedJavaType ___objectAccessType;

    ///
    // Resolved type for {@link BarrieredAccess}.
    ///
    // @field
    private final ResolvedJavaType ___barrieredAccessType;

    // @field
    private final JavaKind ___wordKind;

    // @cons WordTypes
    public WordTypes(MetaAccessProvider __metaAccess, JavaKind __wordKind)
    {
        super();
        this.___wordKind = __wordKind;
        this.___wordBaseType = __metaAccess.lookupJavaType(WordBase.class);
        this.___wordImplType = __metaAccess.lookupJavaType(Word.class);
        this.___wordFactoryType = __metaAccess.lookupJavaType(WordFactory.class);
        this.___objectAccessType = __metaAccess.lookupJavaType(ObjectAccess.class);
        this.___barrieredAccessType = __metaAccess.lookupJavaType(BarrieredAccess.class);

        Word.ensureInitialized();
        this.___wordImplType.initialize();
    }

    ///
    // Determines if a given method denotes a word operation.
    ///
    public boolean isWordOperation(ResolvedJavaMethod __targetMethod)
    {
        final boolean __isWordFactory = this.___wordFactoryType.equals(__targetMethod.getDeclaringClass());
        if (__isWordFactory)
        {
            return true;
        }
        final boolean __isObjectAccess = this.___objectAccessType.equals(__targetMethod.getDeclaringClass());
        final boolean __isBarrieredAccess = this.___barrieredAccessType.equals(__targetMethod.getDeclaringClass());
        if (__isObjectAccess || __isBarrieredAccess)
        {
            return true;
        }
        return isWord(__targetMethod.getDeclaringClass());
    }

    ///
    // Gets the method annotated with {@link Word.Operation} based on a given method that represents a word operation
    // (but may not necessarily have the annotation).
    //
    // @param callingContextType the {@linkplain ResolvedJavaType type} from which {@code targetMethod} is invoked
    // @return the {@link Word.Operation} method resolved for {@code targetMethod} if any
    ///
    public ResolvedJavaMethod getWordOperation(ResolvedJavaMethod __targetMethod, ResolvedJavaType __callingContextType)
    {
        final boolean __isWordBase = this.___wordBaseType.isAssignableFrom(__targetMethod.getDeclaringClass());
        ResolvedJavaMethod __wordMethod = __targetMethod;
        if (__isWordBase && !__targetMethod.isStatic())
        {
            __wordMethod = this.___wordImplType.resolveConcreteMethod(__targetMethod, __callingContextType);
        }
        return __wordMethod;
    }

    ///
    // Determines if a given node has a word type.
    ///
    public boolean isWord(ValueNode __node)
    {
        return isWord(StampTool.typeOrNull(__node));
    }

    ///
    // Determines if a given type is a word type.
    ///
    public boolean isWord(JavaType __type)
    {
        return __type instanceof ResolvedJavaType && this.___wordBaseType.isAssignableFrom((ResolvedJavaType) __type);
    }

    ///
    // Gets the kind for a given type, returning the {@linkplain #getWordKind() word kind} if
    // {@code type} is a {@linkplain #isWord(JavaType) word type}.
    ///
    public JavaKind asKind(JavaType __type)
    {
        if (isWord(__type))
        {
            return this.___wordKind;
        }
        else
        {
            return __type.getJavaKind();
        }
    }

    public JavaKind getWordKind()
    {
        return this.___wordKind;
    }

    ///
    // Gets the stamp for a given {@linkplain #isWord(JavaType) word type}.
    ///
    public Stamp getWordStamp(ResolvedJavaType __type)
    {
        return StampFactory.forKind(this.___wordKind);
    }

    public ResolvedJavaType getWordImplType()
    {
        return this.___wordImplType;
    }
}
