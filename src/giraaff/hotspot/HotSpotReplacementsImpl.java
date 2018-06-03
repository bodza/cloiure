package giraaff.hotspot;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.bytecode.BytecodeProvider;
import giraaff.hotspot.word.HotSpotOperation;
import giraaff.phases.util.Providers;
import giraaff.replacements.ReplacementsImpl;

/**
 * Filters certain method substitutions based on whether there is underlying hardware support for them.
 */
// @class HotSpotReplacementsImpl
public final class HotSpotReplacementsImpl extends ReplacementsImpl
{
    // @cons
    public HotSpotReplacementsImpl(Providers __providers, SnippetReflectionProvider __snippetReflection, BytecodeProvider __bytecodeProvider, TargetDescription __target)
    {
        super(__providers, __snippetReflection, __bytecodeProvider, __target);
    }
}
