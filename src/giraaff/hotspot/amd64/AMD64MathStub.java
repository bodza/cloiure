package giraaff.hotspot.amd64;

import giraaff.api.replacements.Snippet;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.amd64.AMD64HotSpotForeignCallsProvider;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.stubs.SnippetStub;
import giraaff.options.OptionValues;
import giraaff.replacements.nodes.BinaryMathIntrinsicNode;
import giraaff.replacements.nodes.BinaryMathIntrinsicNode.BinaryOperation;
import giraaff.replacements.nodes.UnaryMathIntrinsicNode;
import giraaff.replacements.nodes.UnaryMathIntrinsicNode.UnaryOperation;

/**
 * Stub called to support {@link Math}.
 */
public class AMD64MathStub extends SnippetStub
{
    public AMD64MathStub(ForeignCallDescriptor descriptor, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super(snippetName(descriptor), options, providers, linkage);
    }

    private static String snippetName(ForeignCallDescriptor descriptor)
    {
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_LOG_STUB)
        {
            return "log";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_LOG10_STUB)
        {
            return "log10";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_SIN_STUB)
        {
            return "sin";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_COS_STUB)
        {
            return "cos";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_TAN_STUB)
        {
            return "tan";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_EXP_STUB)
        {
            return "exp";
        }
        if (descriptor == AMD64HotSpotForeignCallsProvider.ARITHMETIC_POW_STUB)
        {
            return "pow";
        }
        throw new InternalError("Unknown operation " + descriptor);
    }

    @Snippet
    private static double log(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.LOG);
    }

    @Snippet
    private static double log10(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.LOG10);
    }

    @Snippet
    private static double sin(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.SIN);
    }

    @Snippet
    private static double cos(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.COS);
    }

    @Snippet
    private static double tan(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.TAN);
    }

    @Snippet
    private static double exp(double value)
    {
        return UnaryMathIntrinsicNode.compute(value, UnaryOperation.EXP);
    }

    @Snippet
    private static double pow(double value1, double value2)
    {
        return BinaryMathIntrinsicNode.compute(value1, value2, BinaryOperation.POW);
    }
}
