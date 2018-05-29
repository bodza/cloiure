package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.tiers.SuitesProvider;
import giraaff.phases.util.Providers;

/**
 * Extends {@link Providers} to include a number of extra capabilities used by the HotSpot parts of
 * the compiler.
 */
// @class HotSpotProviders
public final class HotSpotProviders extends Providers
{
    private final SuitesProvider suites;
    private final HotSpotRegistersProvider registers;
    private final SnippetReflectionProvider snippetReflection;
    private final HotSpotWordTypes wordTypes;
    private final Plugins graphBuilderPlugins;

    // @cons
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
