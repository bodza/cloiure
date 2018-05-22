package graalvm.compiler.replacements;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.core.common.spi.ArrayOffsetProvider;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.nodes.ExplodeLoopNode;
import graalvm.util.UnsafeAccess;

public class ConstantStringIndexOfSnippets implements Snippets
{
    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo indexOfConstant = snippet(ConstantStringIndexOfSnippets.class, "indexOfConstant");

        public Templates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
        {
            super(options, providers, snippetReflection, target);
        }

        public void lower(SnippetLowerableMemoryNode stringIndexOf, LoweringTool tool)
        {
            StructuredGraph graph = stringIndexOf.graph();
            Arguments args = new Arguments(indexOfConstant, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("source", stringIndexOf.getArgument(0));
            args.add("sourceOffset", stringIndexOf.getArgument(1));
            args.add("sourceCount", stringIndexOf.getArgument(2));
            args.addConst("target", stringIndexOf.getArgument(3));
            args.add("targetOffset", stringIndexOf.getArgument(4));
            args.add("targetCount", stringIndexOf.getArgument(5));
            args.add("origFromIndex", stringIndexOf.getArgument(6));
            char[] targetCharArray = snippetReflection.asObject(char[].class, stringIndexOf.getArgument(3).asJavaConstant());
            args.addConst("md2", md2(targetCharArray));
            args.addConst("cache", computeCache(targetCharArray));
            template(stringIndexOf, args).instantiate(providers.getMetaAccess(), stringIndexOf, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }

    static int md2(char[] target)
    {
        int c = target.length;
        if (c == 0)
        {
            return 0;
        }
        char lastChar = target[c - 1];
        int md2 = c;
        for (int i = 0; i < c - 1; i++)
        {
            if (target[i] == lastChar)
            {
                md2 = (c - 1) - i;
            }
        }
        return md2;
    }

    static long computeCache(char[] s)
    {
        int c = s.length;
        int cache = 0;
        int i;
        for (i = 0; i < c - 1; i++)
        {
            cache |= (1 << (s[i] & 63));
        }
        return cache;
    }

    @Fold
    static int charArrayBaseOffset(@InjectedParameter ArrayOffsetProvider arrayOffsetProvider)
    {
        return arrayOffsetProvider.arrayBaseOffset(JavaKind.Char);
    }

    /** Marker value for the {@link InjectedParameter} injected parameter. */
    static final ArrayOffsetProvider INJECTED = null;

    @Snippet
    public static int indexOfConstant(char[] source, int sourceOffset, int sourceCount, @ConstantParameter char[] target, int targetOffset, int targetCount, int origFromIndex, @ConstantParameter int md2, @ConstantParameter long cache)
    {
        int fromIndex = origFromIndex;
        if (fromIndex >= sourceCount)
        {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0)
        {
            fromIndex = 0;
        }
        if (targetCount == 0)
        {
            return fromIndex;
        }

        int targetCountLess1 = targetCount - 1;
        int sourceEnd = sourceCount - targetCountLess1;

        long base = charArrayBaseOffset(INJECTED);
        int lastChar = UnsafeAccess.UNSAFE.getChar(target, base + targetCountLess1 * 2);

        outer_loop: for (long i = sourceOffset + fromIndex; i < sourceEnd;)
        {
            int src = UnsafeAccess.UNSAFE.getChar(source, base + (i + targetCountLess1) * 2);
            if (src == lastChar)
            {
                // With random strings and a 4-character alphabet,
                // reverse matching at this point sets up 0.8% fewer
                // frames, but (paradoxically) makes 0.3% more probes.
                // Since those probes are nearer the lastChar probe,
                // there is may be a net D$ win with reverse matching.
                // But, reversing loop inhibits unroll of inner loop
                // for unknown reason. So, does running outer loop from
                // (sourceOffset - targetCountLess1) to (sourceOffset + sourceCount)
                if (targetCount <= 8)
                {
                    ExplodeLoopNode.explodeLoop();
                }
                for (long j = 0; j < targetCountLess1; j++)
                {
                    char sourceChar = UnsafeAccess.UNSAFE.getChar(source, base + (i + j) * 2);
                    if (UnsafeAccess.UNSAFE.getChar(target, base + (targetOffset + j) * 2) != sourceChar)
                    {
                        if ((cache & (1 << sourceChar)) == 0)
                        {
                            if (md2 < j + 1)
                            {
                                i += j + 1;
                                continue outer_loop;
                            }
                        }
                        i += md2;
                        continue outer_loop;
                    }
                }
                return (int) (i - sourceOffset);
            }
            if ((cache & (1 << src)) == 0)
            {
                i += targetCountLess1;
            }
            i++;
        }
        return -1;
    }
}
