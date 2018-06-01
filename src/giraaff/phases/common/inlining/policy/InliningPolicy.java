package giraaff.phases.common.inlining.policy;

import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.walker.MethodInvocation;

// @iface InliningPolicy
public interface InliningPolicy
{
    // @class InliningPolicy.Decision
    static final class Decision
    {
        public static final Decision YES = new Decision(true, "(unknown reason)");
        public static final Decision NO = new Decision(false, "(unknown reason)");

        public final boolean shouldInline;
        public final String reason;

        // @cons
        private Decision(boolean shouldInline, String reason)
        {
            super();
            this.shouldInline = shouldInline;
            this.reason = reason;
        }
    }

    boolean continueInlining(StructuredGraph graph);

    Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed);
}
