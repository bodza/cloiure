package giraaff.replacements.amd64;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.calc.FloatConvertNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;

/**
 * Snippets used for conversion operations on AMD64 where the AMD64 instruction used does not match
 * the semantics of the JVM specification.
 */
// @class AMD64ConvertSnippets
public final class AMD64ConvertSnippets implements Snippets
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
    public static int f2i(float __input, int __result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __result == Integer.MIN_VALUE))
        {
            if (Float.isNaN(__input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (__input > 0.0f)
            {
                // input is > 0 -> return max int
                return Integer.MAX_VALUE;
            }
        }
        return __result;
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
    public static long f2l(float __input, long __result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __result == Long.MIN_VALUE))
        {
            if (Float.isNaN(__input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (__input > 0.0f)
            {
                // input is > 0 -> return max int
                return Long.MAX_VALUE;
            }
        }
        return __result;
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
    public static int d2i(double __input, int __result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __result == Integer.MIN_VALUE))
        {
            if (Double.isNaN(__input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (__input > 0.0d)
            {
                // input is positive -> return maxInt
                return Integer.MAX_VALUE;
            }
        }
        return __result;
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
    public static long d2l(double __input, long __result)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __result == Long.MIN_VALUE))
        {
            if (Double.isNaN(__input))
            {
                // input is NaN -> return 0
                return 0;
            }
            else if (__input > 0.0d)
            {
                // input is positive -> return maxInt
                return Long.MAX_VALUE;
            }
        }
        return __result;
    }

    // @class AMD64ConvertSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo f2i;
        // @field
        private final SnippetInfo f2l;
        // @field
        private final SnippetInfo d2i;
        // @field
        private final SnippetInfo d2l;

        // @cons
        public Templates(Providers __providers, SnippetReflectionProvider __snippetReflection, TargetDescription __target)
        {
            super(__providers, __snippetReflection, __target);

            f2i = snippet(AMD64ConvertSnippets.class, "f2i");
            f2l = snippet(AMD64ConvertSnippets.class, "f2l");
            d2i = snippet(AMD64ConvertSnippets.class, "d2i");
            d2l = snippet(AMD64ConvertSnippets.class, "d2l");
        }

        public void lower(FloatConvertNode __convert, LoweringTool __tool)
        {
            SnippetInfo __key;
            switch (__convert.getFloatConvert())
            {
                case F2I:
                    __key = f2i;
                    break;
                case F2L:
                    __key = f2l;
                    break;
                case D2I:
                    __key = d2i;
                    break;
                case D2L:
                    __key = d2l;
                    break;
                default:
                    return;
            }

            StructuredGraph __graph = __convert.graph();

            Arguments __args = new Arguments(__key, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("input", __convert.getValue());
            __args.add("result", __graph.unique(new AMD64FloatConvertNode(__convert.getFloatConvert(), __convert.getValue())));

            SnippetTemplate __template = template(__convert, __args);
            __template.instantiate(providers.getMetaAccess(), __convert, SnippetTemplate.DEFAULT_REPLACER, __tool, __args);
            __convert.safeDelete();
        }
    }
}
