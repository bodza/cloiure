package graalvm.compiler.core.match;

import graalvm.compiler.core.gen.NodeMatchRules;

/**
 * Code generator for complex match patterns.
 */
public interface MatchGenerator {
    /**
     * @returns null if the match can't be generated or a {@link ComplexMatchResult} that can be
     *          evaluated during LIR generation to produce the final LIR value.
     */
    ComplexMatchResult match(NodeMatchRules matchRules, Object... args);

    /**
     * @return a descriptive name meaningful to the user.
     */
    String getName();
}
