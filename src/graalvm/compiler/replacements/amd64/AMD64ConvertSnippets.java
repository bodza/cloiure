package graalvm.compiler.replacements.amd64;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.calc.FloatConvertNode;
import graalvm.compiler.nodes.extended.BranchProbabilityNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;

/**
 * Snippets used for conversion operations on AMD64 where the AMD64 instruction used does not match
 * the semantics of the JVM specification.
 */
public class AMD64ConvertSnippets implements Snippets
{
    /**
     * Converts a float to an int.
     *
     * This snippet accounts for the semantics of the x64 CVTTSS2SI instruction used to do the
     * conversion. If the float value is a NaN, infinity or if the result of the conversion is
     * larger than {@link Integer#MAX_VALUE} then CVTTSS2SI returns {@link Integer#MIN_VALUE} and
     * extra tests are required on the float value to return the correct int value.
     *
     * @param input the float being converted
     * @param result the result produced by the CVTTSS2SI instruction
     */
    @Snippet
    public static int f2i(float input, int result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, result == Integer.MIN_VALUE))
        {
            if (Float.isNaN(input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (input > 0.0f)
            {
                // input is > 0 -> return max int
                return Integer.MAX_VALUE;
            }
        }
        return result;
    }

    /**
     * Converts a float to a long.
     *
     * This snippet accounts for the semantics of the x64 CVTTSS2SI instruction used to do the
     * conversion. If the float value is a NaN or infinity then CVTTSS2SI returns
     * {@link Long#MIN_VALUE} and extra tests are required on the float value to return the correct
     * long value.
     *
     * @param input the float being converted
     * @param result the result produced by the CVTTSS2SI instruction
     */
    @Snippet
    public static long f2l(float input, long result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, result == Long.MIN_VALUE))
        {
            if (Float.isNaN(input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (input > 0.0f)
            {
                // input is > 0 -> return max int
                return Long.MAX_VALUE;
            }
        }
        return result;
    }

    /**
     * Converts a double to an int.
     *
     * This snippet accounts for the semantics of the x64 CVTTSD2SI instruction used to do the
     * conversion. If the double value is a NaN, infinity or if the result of the conversion is
     * larger than {@link Integer#MAX_VALUE} then CVTTSD2SI returns {@link Integer#MIN_VALUE} and
     * extra tests are required on the double value to return the correct int value.
     *
     * @param input the double being converted
     * @param result the result produced by the CVTTSS2SI instruction
     */
    @Snippet
    public static int d2i(double input, int result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, result == Integer.MIN_VALUE))
        {
            if (Double.isNaN(input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (input > 0.0d)
            {
                // input is positive -> return maxInt
                return Integer.MAX_VALUE;
            }
        }
        return result;
    }

    /**
     * Converts a double to a long.
     *
     * This snippet accounts for the semantics of the x64 CVTTSD2SI instruction used to do the
     * conversion. If the double value is a NaN, infinity or if the result of the conversion is
     * larger than {@link Long#MAX_VALUE} then CVTTSD2SI returns {@link Long#MIN_VALUE} and extra
     * tests are required on the double value to return the correct long value.
     *
     * @param input the double being converted
     * @param result the result produced by the CVTTSS2SI instruction
     */
    @Snippet
    public static long d2l(double input, long result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, result == Long.MIN_VALUE))
        {
            if (Double.isNaN(input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (input > 0.0d)
            {
                // input is positive -> return maxInt
                return Long.MAX_VALUE;
            }
        }
        return result;
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo f2i;
        private final SnippetInfo f2l;
        private final SnippetInfo d2i;
        private final SnippetInfo d2l;

        public Templates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
        {
            super(options, providers, snippetReflection, target);

            f2i = snippet(AMD64ConvertSnippets.class, "f2i");
            f2l = snippet(AMD64ConvertSnippets.class, "f2l");
            d2i = snippet(AMD64ConvertSnippets.class, "d2i");
            d2l = snippet(AMD64ConvertSnippets.class, "d2l");
        }

        public void lower(FloatConvertNode convert, LoweringTool tool)
        {
            SnippetInfo key;
            switch (convert.getFloatConvert())
            {
                case F2I:
                    key = f2i;
                    break;
                case F2L:
                    key = f2l;
                    break;
                case D2I:
                    key = d2i;
                    break;
                case D2L:
                    key = d2l;
                    break;
                default:
                    return;
            }

            StructuredGraph graph = convert.graph();

            Arguments args = new Arguments(key, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("input", convert.getValue());
            args.add("result", graph.unique(new AMD64FloatConvertNode(convert.getFloatConvert(), convert.getValue())));

            SnippetTemplate template = template(convert, args);
            template.instantiate(providers.getMetaAccess(), convert, SnippetTemplate.DEFAULT_REPLACER, tool, args);
            convert.safeDelete();
        }
    }
}
