package graalvm.compiler.truffle.common;

import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.ExceptionHandler;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Mark;

/**
 * A listener for events related to the compilation of a {@link CompilableTruffleAST}. The events
 * are described only in terms of types that can be easily serialized or proxied across a heap
 * boundary.
 */
public interface TruffleCompilerListener {

    /**
     * Summary information for a Graal compiler graph.
     */
    interface GraphInfo {
        /**
         * Gets the number of nodes in the graph.
         */
        int getNodeCount();

        /**
         * Gets the set of nodes in the graph.
         *
         * @param simpleNames whether to return {@linkplain Class#getSimpleName() simple} names
         * @return list of type names for all the nodes in the graph
         */
        String[] getNodeTypes(boolean simpleNames);
    }

    /**
     * Summary information for the result of a compilation.
     */
    interface CompilationResultInfo {
        /**
         * Gets the size of the machine code generated.
         */
        int getTargetCodeSize();

        /**
         * Gets the total frame size of compiled code in bytes. This includes the return address
         * pushed onto the stack, if any.
         */
        int getTotalFrameSize();

        /**
         * Gets the number of {@link ExceptionHandler}s in the compiled code.
         */
        int getExceptionHandlersCount();

        /**
         * Gets the number of {@link Infopoint}s in the compiled code.
         */
        int getInfopointsCount();

        /**
         * Gets the infopoint {@linkplain InfopointReason reasons} in the compiled code.
         */
        String[] getInfopoints();

        /**
         * Gets the number of {@link Mark}s in the compiled code.
         */
        int getMarksCount();

        /**
         * Gets the number of {@link DataPatch}es in the compiled code.
         */
        int getDataPatchesCount();
    }

    /**
     * Notifies this object when Graal IR compilation {@code compilable} completes. Graal
     * compilation occurs between {@link #onTruffleTierFinished} and code installation.
     *
     * @param compilable the call target that was compiled
     * @param graph the graph representing {@code compilable}
     */
    void onGraalTierFinished(CompilableTruffleAST compilable, GraphInfo graph);

    /**
     * Notifies this object when compilation of {@code compilable} has completed partial evaluation
     * and is about to perform compilation of the graph produced by partial evaluation.
     *
     * @param compilable the call target being compiled
     * @param inliningPlan the inlining plan used during partial evaluation
     * @param graph the graph representing {@code compilable}
     */
    void onTruffleTierFinished(CompilableTruffleAST compilable, TruffleInliningPlan inliningPlan, GraphInfo graph);

    /**
     * Notifies this object when compilation of {@code compilable} succeeds.
     *
     * @param compilable the Truffle AST whose compilation succeeded
     */
    void onSuccess(CompilableTruffleAST compilable, TruffleInliningPlan inliningPlan, GraphInfo graphInfo, CompilationResultInfo compilationResultInfo);

    /**
     * Notifies this object when compilation of {@code compilable} fails.
     *
     * @param compilable the Truffle AST whose compilation failed
     * @param reason the reason compilation failed
     * @param bailout specifies whether the failure was a bailout or an error in the compiler. A
     *            bailout means the compiler aborted the compilation based on some of property of
     *            {@code target} (e.g., too big). A non-bailout means an unexpected error in the
     *            compiler itself.
     * @param permanentBailout specifies if a bailout is due to a condition that probably won't
     *            change if the {@code target} is compiled again. This value is meaningless if
     *            {@code bailout == false}.
     */
    void onFailure(CompilableTruffleAST compilable, String reason, boolean bailout, boolean permanentBailout);
}
