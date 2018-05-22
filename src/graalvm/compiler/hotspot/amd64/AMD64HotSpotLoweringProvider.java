package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.spi.ForeignCallsProvider;
import graalvm.compiler.graph.Node;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend.Options;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.hotspot.amd64.AMD64HotSpotForeignCallsProvider;
import graalvm.compiler.hotspot.meta.DefaultHotSpotLoweringProvider;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.hotspot.nodes.profiling.ProfileNode;
import graalvm.compiler.hotspot.replacements.profiling.ProbabilisticProfileSnippets;
import graalvm.compiler.nodes.calc.FloatConvertNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.amd64.AMD64ConvertSnippets;
import graalvm.compiler.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation;
import graalvm.compiler.replacements.nodes.UnaryMathIntrinsicNode.UnaryOperation;

public class AMD64HotSpotLoweringProvider extends DefaultHotSpotLoweringProvider
{
    private AMD64ConvertSnippets.Templates convertSnippets;
    private ProbabilisticProfileSnippets.Templates profileSnippets;

    public AMD64HotSpotLoweringProvider(HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, ForeignCallsProvider foreignCalls, HotSpotRegistersProvider registers, HotSpotConstantReflectionProvider constantReflection, TargetDescription target)
    {
        super(runtime, metaAccess, foreignCalls, registers, constantReflection, target);
    }

    @Override
    public void initialize(OptionValues options, HotSpotProviders providers, GraalHotSpotVMConfig config)
    {
        convertSnippets = new AMD64ConvertSnippets.Templates(options, providers, providers.getSnippetReflection(), providers.getCodeCache().getTarget());
        profileSnippets = ProfileNode.Options.ProbabilisticProfiling.getValue(options) ? new ProbabilisticProfileSnippets.Templates(options, providers, providers.getCodeCache().getTarget()) : null;
        super.initialize(options, providers, config);
    }

    @Override
    public void lower(Node n, LoweringTool tool)
    {
        if (n instanceof FloatConvertNode)
        {
            convertSnippets.lower((FloatConvertNode) n, tool);
        }
        else if (profileSnippets != null && n instanceof ProfileNode)
        {
            profileSnippets.lower((ProfileNode) n, tool);
        }
        else
        {
            super.lower(n, tool);
        }
    }

    @Override
    protected ForeignCallDescriptor toForeignCall(UnaryOperation operation)
    {
        if (Options.GraalArithmeticStubs.getValue(runtime.getOptions()))
        {
            switch (operation)
            {
                case LOG:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_LOG_STUB;
                case LOG10:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_LOG10_STUB;
                case SIN:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_SIN_STUB;
                case COS:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_COS_STUB;
                case TAN:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_TAN_STUB;
                case EXP:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_EXP_STUB;
            }
        }
        else if (operation == UnaryOperation.EXP)
        {
            return operation.foreignCallDescriptor;
        }
        // Lower only using LIRGenerator
        return null;
    }

    @Override
    protected ForeignCallDescriptor toForeignCall(BinaryOperation operation)
    {
        if (Options.GraalArithmeticStubs.getValue(runtime.getOptions()))
        {
            switch (operation)
            {
                case POW:
                    return AMD64HotSpotForeignCallsProvider.ARITHMETIC_POW_STUB;
            }
        }
        else if (operation == BinaryOperation.POW)
        {
            return operation.foreignCallDescriptor;
        }
        // Lower only using LIRGenerator
        return null;
    }

    @Override
    public Integer smallestCompareWidth()
    {
        return 8;
    }
}
