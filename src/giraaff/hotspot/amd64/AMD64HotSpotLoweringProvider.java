package giraaff.hotspot.amd64;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.graph.Node;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.meta.DefaultHotSpotLoweringProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.amd64.AMD64ConvertSnippets;

// @class AMD64HotSpotLoweringProvider
public final class AMD64HotSpotLoweringProvider extends DefaultHotSpotLoweringProvider
{
    // @field
    private AMD64ConvertSnippets.Templates ___convertSnippets;

    // @cons
    public AMD64HotSpotLoweringProvider(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, ForeignCallsProvider __foreignCalls, HotSpotRegistersProvider __registers, HotSpotConstantReflectionProvider __constantReflection, TargetDescription __target)
    {
        super(__runtime, __metaAccess, __foreignCalls, __registers, __constantReflection, __target);
    }

    @Override
    public void initialize(HotSpotProviders __providers)
    {
        this.___convertSnippets = new AMD64ConvertSnippets.Templates(__providers, __providers.getSnippetReflection(), __providers.getCodeCache().getTarget());
        super.initialize(__providers);
    }

    @Override
    public void lower(Node __n, LoweringTool __tool)
    {
        if (__n instanceof FloatConvertNode)
        {
            this.___convertSnippets.lower((FloatConvertNode) __n, __tool);
        }
        else
        {
            super.lower(__n, __tool);
        }
    }

    @Override
    public Integer smallestCompareWidth()
    {
        return 8;
    }
}
