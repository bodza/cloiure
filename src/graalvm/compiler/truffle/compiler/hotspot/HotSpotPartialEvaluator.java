package graalvm.compiler.truffle.compiler.hotspot;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.truffle.compiler.PartialEvaluator;
import graalvm.compiler.truffle.compiler.phases.InstrumentPhase.Instrumentation;

import jdk.vm.ci.code.Architecture;

public final class HotSpotPartialEvaluator extends PartialEvaluator {
    public HotSpotPartialEvaluator(Providers providers, GraphBuilderConfiguration configForRoot, SnippetReflectionProvider snippetReflection, Architecture architecture,
                    Instrumentation instrumentation) {
        super(providers, configForRoot, snippetReflection, architecture, instrumentation, new HotSpotKnownTruffleTypes(providers.getMetaAccess()));
    }

    @Override
    protected void registerTruffleInvocationPlugins(InvocationPlugins invocationPlugins, boolean canDelayIntrinsification) {
        super.registerTruffleInvocationPlugins(invocationPlugins, canDelayIntrinsification);
        HotSpotTruffleGraphBuilderPlugins.registerCompilationFinalReferencePlugins(invocationPlugins, canDelayIntrinsification, (HotSpotKnownTruffleTypes) getKnownTruffleTypes());
    }
}
