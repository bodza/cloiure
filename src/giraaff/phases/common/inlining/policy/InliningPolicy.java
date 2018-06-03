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
        // @def
        public static final Decision YES = new Decision(true, "(unknown reason)");
        // @def
        public static final Decision NO = new Decision(false, "(unknown reason)");

        // @field
        public final boolean ___shouldInline;
        // @field
        public final String ___reason;

        // @cons
        private Decision(boolean __shouldInline, String __reason)
        {
            super();
            this.___shouldInline = __shouldInline;
            this.___reason = __reason;
        }
    }

    boolean continueInlining(StructuredGraph __graph);

    Decision isWorthInlining(Replacements __replacements, MethodInvocation __invocation, int __inliningDepth, boolean __fullyProcessed);
}
