package graalvm.compiler.hotspot;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.bytecode.BytecodeProvider;
import graalvm.compiler.hotspot.word.HotSpotOperation;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.replacements.ReplacementsImpl;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Filters certain method substitutions based on whether there is underlying hardware support for
 * them.
 */
public class HotSpotReplacementsImpl extends ReplacementsImpl
{
    public HotSpotReplacementsImpl(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, BytecodeProvider bytecodeProvider, TargetDescription target)
    {
        super(options, providers, snippetReflection, bytecodeProvider, target);
    }

    @Override
    protected boolean hasGenericInvocationPluginAnnotation(ResolvedJavaMethod method)
    {
        return method.getAnnotation(HotSpotOperation.class) != null || super.hasGenericInvocationPluginAnnotation(method);
    }
}
