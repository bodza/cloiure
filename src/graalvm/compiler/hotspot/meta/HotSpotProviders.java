package graalvm.compiler.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.hotspot.word.HotSpotWordTypes;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.nodes.spi.LoweringProvider;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.phases.tiers.SuitesProvider;
import graalvm.compiler.phases.util.Providers;

/**
 * Extends {@link Providers} to include a number of extra capabilities used by the HotSpot parts of
 * the compiler.
 */
public class HotSpotProviders extends Providers
{
    private final SuitesProvider suites;
    private final HotSpotRegistersProvider registers;
    private final SnippetReflectionProvider snippetReflection;
    private final HotSpotWordTypes wordTypes;
    private final Plugins graphBuilderPlugins;

    public HotSpotProviders(MetaAccessProvider metaAccess, HotSpotCodeCacheProvider codeCache, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantField, HotSpotForeignCallsProvider foreignCalls, LoweringProvider lowerer, Replacements replacements, SuitesProvider suites, HotSpotRegistersProvider registers, SnippetReflectionProvider snippetReflection, HotSpotWordTypes wordTypes, Plugins graphBuilderPlugins)
    {
        super(metaAccess, codeCache, constantReflection, constantField, foreignCalls, lowerer, replacements, new HotSpotStampProvider());
        this.suites = suites;
        this.registers = registers;
        this.snippetReflection = snippetReflection;
        this.wordTypes = wordTypes;
        this.graphBuilderPlugins = graphBuilderPlugins;
    }

    @Override
    public HotSpotCodeCacheProvider getCodeCache()
    {
        return (HotSpotCodeCacheProvider) super.getCodeCache();
    }

    @Override
    public HotSpotForeignCallsProvider getForeignCalls()
    {
        return (HotSpotForeignCallsProvider) super.getForeignCalls();
    }

    public SuitesProvider getSuites()
    {
        return suites;
    }

    public HotSpotRegistersProvider getRegisters()
    {
        return registers;
    }

    public SnippetReflectionProvider getSnippetReflection()
    {
        return snippetReflection;
    }

    public Plugins getGraphBuilderPlugins()
    {
        return graphBuilderPlugins;
    }

    public HotSpotWordTypes getWordTypes()
    {
        return wordTypes;
    }
}
