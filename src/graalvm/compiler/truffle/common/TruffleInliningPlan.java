package graalvm.compiler.truffle.common;

import graalvm.compiler.graph.SourceLanguagePosition;

import jdk.vm.ci.meta.JavaConstant;

/**
 * A plan to be consulted when partial evaluating or compiling a Truffle AST as to whether a given
 * call should be inlined.
 */
public interface TruffleInliningPlan {

    /**
     * Gets the decision of whether or not to inline the Truffle AST called by {@code callNode}.
     *
     * @param callNode a call in the AST represented by this object
     * @return the decision for {@code callNode} or {@code null} when this object contains no
     *         decision for {@code callNode}
     */
    Decision findDecision(JavaConstant callNode);

    /**
     * If {@code node} represents an AST Node then return the nearest source information for it.
     * Otherwise simply return null.
     */
    SourceLanguagePosition getPosition(JavaConstant node);

    /**
     * Decision of whether a called Truffle AST should be inlined. If {@link #shouldInline()}
     * returns {@code true}, this object is also an inlining plan for the calls in the to-be-inlined
     * AST.
     */
    interface Decision extends TruffleInliningPlan {

        /**
         * Returns whether the Truffle AST to which this decision pertains should be inlined.
         */
        boolean shouldInline();

        /**
         * Determines if the Truffle AST to which this decision pertains did not change between AST
         * execution and computation of the inlining decision tree.
         */
        boolean isTargetStable();

        /**
         * Gets a name for the Truffle AST to which this decision pertains.
         */
        String getTargetName();

        /**
         * Gets the assumption that will be invalidated when a node is rewritten in the Truffle AST
         * to which this decision pertains.
         */
        JavaConstant getNodeRewritingAssumption();
    }
}
