package graalvm.compiler.replacements;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.core.common.spi.ArrayOffsetProvider;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory.InjectionProvider;
import graalvm.compiler.word.WordTypes;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

public class NodeIntrinsificationProvider implements InjectionProvider
{
    private final MetaAccessProvider metaAccess;
    private final SnippetReflectionProvider snippetReflection;
    private final ForeignCallsProvider foreignCalls;
    private final ArrayOffsetProvider arrayOffsetProvider;
    private final WordTypes wordTypes;

    public NodeIntrinsificationProvider(MetaAccessProvider metaAccess, SnippetReflectionProvider snippetReflection, ForeignCallsProvider foreignCalls, ArrayOffsetProvider arrayOffsetProvider, WordTypes wordTypes)
    {
        this.metaAccess = metaAccess;
        this.snippetReflection = snippetReflection;
        this.foreignCalls = foreignCalls;
        this.arrayOffsetProvider = arrayOffsetProvider;
        this.wordTypes = wordTypes;
    }

    @Override
    public Stamp getInjectedStamp(Class<?> type, boolean nonNull)
    {
        JavaKind kind = JavaKind.fromJavaClass(type);
        if (kind == JavaKind.Object)
        {
            ResolvedJavaType returnType = metaAccess.lookupJavaType(type);
            if (wordTypes.isWord(returnType))
            {
                return wordTypes.getWordStamp(returnType);
            }
            else
            {
                return StampFactory.object(TypeReference.createWithoutAssumptions(returnType), nonNull);
            }
        }
        else
        {
            return StampFactory.forKind(kind);
        }
    }

    @Override
    public <T> T getInjectedArgument(Class<T> type)
    {
        T injected = snippetReflection.getInjectedNodeIntrinsicParameter(type);
        if (injected != null)
        {
            return injected;
        }
        else if (type.equals(ForeignCallsProvider.class))
        {
            return type.cast(foreignCalls);
        }
        else if (type.equals(SnippetReflectionProvider.class))
        {
            return type.cast(snippetReflection);
        }
        else if (type.equals(ArrayOffsetProvider.class))
        {
            return type.cast(arrayOffsetProvider);
        }
        else
        {
            throw new GraalError("Cannot handle injected argument of type %s.", type.getName());
        }
    }
}
