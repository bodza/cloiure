package giraaff.hotspot.amd64;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.graph.Node;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.amd64.AMD64HotSpotForeignCallsProvider;
import giraaff.hotspot.meta.DefaultHotSpotLoweringProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.amd64.AMD64ConvertSnippets;

public class AMD64HotSpotLoweringProvider extends DefaultHotSpotLoweringProvider
{
    private AMD64ConvertSnippets.Templates convertSnippets;

    public AMD64HotSpotLoweringProvider(HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, ForeignCallsProvider foreignCalls, HotSpotRegistersProvider registers, HotSpotConstantReflectionProvider constantReflection, TargetDescription target)
    {
        super(runtime, metaAccess, foreignCalls, registers, constantReflection, target);
    }

    @Override
    public void initialize(OptionValues options, HotSpotProviders providers, GraalHotSpotVMConfig config)
    {
        convertSnippets = new AMD64ConvertSnippets.Templates(options, providers, providers.getSnippetReflection(), providers.getCodeCache().getTarget());
        super.initialize(options, providers, config);
    }

    @Override
    public void lower(Node n, LoweringTool tool)
    {
        if (n instanceof FloatConvertNode)
        {
            convertSnippets.lower((FloatConvertNode) n, tool);
        }
        else
        {
            super.lower(n, tool);
        }
    }

    @Override
    public Integer smallestCompareWidth()
    {
        return 8;
    }
}
