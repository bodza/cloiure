package giraaff.phases.common.inlining.policy;

import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.walker.MethodInvocation;

public interface InliningPolicy
{
    class Decision
    {
        public static final Decision YES = new Decision(true, "(unknown reason)");
        public static final Decision NO = new Decision(false, "(unknown reason)");

        private final boolean shouldInline;
        private final String reason;

        private Decision(boolean shouldInline, String reason)
        {
            this.shouldInline = shouldInline;
            this.reason = reason;
        }

        public boolean shouldInline()
        {
            return shouldInline;
        }

        public String getReason()
        {
            return reason;
        }
    }

    boolean continueInlining(StructuredGraph graph);

    Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed);
}
