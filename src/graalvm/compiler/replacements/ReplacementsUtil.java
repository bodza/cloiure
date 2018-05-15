package graalvm.compiler.replacements;

import graalvm.compiler.debug.Assertions;
import graalvm.compiler.replacements.nodes.AssertionNode;

public final class ReplacementsUtil {
    private ReplacementsUtil() {
        // empty
    }

    public static final boolean REPLACEMENTS_ASSERTIONS_ENABLED = Assertions.assertionsEnabled();

    /**
     * Asserts that condition evaluates to true by the time compilation is finished. This is
     * intended to be used within snippets or stubs, and will lead to a compile error if the
     * assertion fails.
     */
    public static void staticAssert(boolean condition, String message) {
        if (REPLACEMENTS_ASSERTIONS_ENABLED) {
            AssertionNode.assertion(true, condition, message);
        }
    }

    /**
     * Asserts that condition evaluates to true at runtime. This is intended to be used within
     * snippets or stubs, and will lead to a VM error if it fails.
     */
    public static void runtimeAssert(boolean condition, String message) {
        if (REPLACEMENTS_ASSERTIONS_ENABLED) {
            AssertionNode.assertion(false, condition, message);
        }
    }
}
