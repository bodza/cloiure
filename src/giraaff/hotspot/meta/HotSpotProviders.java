package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.tiers.SuitesProvider;
import giraaff.phases.util.Providers;

///
// Extends {@link Providers} to include a number of extra capabilities used by the HotSpot parts of
// the compiler.
///
// @class HotSpotProviders
public final class HotSpotProviders extends Providers
{
    // @field
    private final SuitesProvider ___suites;
    // @field
    private final HotSpotRegistersProvider ___registers;
    // @field
    private final SnippetReflectionProvider ___snippetReflection;
    // @field
    private final HotSpotWordTypes ___wordTypes;
    // @field
    private final GraphBuilderConfiguration.Plugins ___graphBuilderPlugins;

    // @cons HotSpotProviders
    public HotSpotProviders(MetaAccessProvider __metaAccess, HotSpotCodeCacheProvider __codeCache, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantField, HotSpotForeignCallsProvider __foreignCalls, LoweringProvider __lowerer, Replacements __replacements, SuitesProvider __suites, HotSpotRegistersProvider __registers, SnippetReflectionProvider __snippetReflection, HotSpotWordTypes __wordTypes, GraphBuilderConfiguration.Plugins __graphBuilderPlugins)
    {
        super(__metaAccess, __codeCache, __constantReflection, __constantField, __foreignCalls, __lowerer, __replacements, new HotSpotStampProvider());
        this.___suites = __suites;
        this.___registers = __registers;
        this.___snippetReflection = __snippetReflection;
        this.___wordTypes = __wordTypes;
        this.___graphBuilderPlugins = __graphBuilderPlugins;
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
        return this.___suites;
    }

    public HotSpotRegistersProvider getRegisters()
    {
        return this.___registers;
    }

    public SnippetReflectionProvider getSnippetReflection()
    {
        return this.___snippetReflection;
    }

    public GraphBuilderConfiguration.Plugins getGraphBuilderPlugins()
    {
        return this.___graphBuilderPlugins;
    }

    public HotSpotWordTypes getWordTypes()
    {
        return this.___wordTypes;
    }
}
