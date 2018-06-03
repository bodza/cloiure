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
    // @field
    private final SuitesProvider suites;
    // @field
    private final HotSpotRegistersProvider registers;
    // @field
    private final SnippetReflectionProvider snippetReflection;
    // @field
    private final HotSpotWordTypes wordTypes;
    // @field
    private final Plugins graphBuilderPlugins;

    // @cons
    public HotSpotProviders(MetaAccessProvider __metaAccess, HotSpotCodeCacheProvider __codeCache, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantField, HotSpotForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, Replacements __replacements, SuitesProvider __suites, HotSpotRegistersProvider __registers, SnippetReflectionProvider __snippetReflection, HotSpotWordTypes __wordTypes, Plugins __graphBuilderPlugins)
    {
        super(__metaAccess, __codeCache, __constantReflection, __constantField, __foreignCalls, __lowerer, __replacements, new HotSpotStampProvider());
        this.suites = __suites;
        this.registers = __registers;
        this.snippetReflection = __snippetReflection;
        this.wordTypes = __wordTypes;
        this.graphBuilderPlugins = __graphBuilderPlugins;
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
