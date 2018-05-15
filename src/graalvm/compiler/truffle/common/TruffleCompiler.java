package graalvm.compiler.truffle.common;

import graalvm.compiler.core.common.CompilationIdentifier;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.nodes.Cancellable;
import graalvm.compiler.options.OptionValues;

/**
 * A compiler that partially evaluates and compiles a {@link CompilableTruffleAST} to machine code.
 */
public interface TruffleCompiler {

    /**
     * Gets a compilation identifier for a given compilable.
     *
     * @return {@code null} if a {@link CompilationIdentifier} cannot shared across the Truffle
     *         runtime/compiler boundary represented by this object
     */
    CompilationIdentifier getCompilationIdentifier(CompilableTruffleAST compilable);

    /**
     * Opens a debug context for compiling {@code compilable}. The {@link DebugContext#close()}
     * method should be called on the returned object once the compilation is finished.
     *
     * @return {@code null} if a {@link DebugContext} cannot be shared across the Truffle
     *         runtime/compiler boundary represented by this object
     */
    DebugContext openDebugContext(OptionValues options, CompilationIdentifier compilationId, CompilableTruffleAST compilable);

    /**
     * Compiles {@code compilable} to machine code.
     *
     * @param debug a debug context to use or {@code null} if a {@link DebugContext} cannot cross
     *            the Truffle runtime/compiler boundary represented by this object
     * @param compilationId an identifier to be used for the compilation or {@code null} if a
     *            {@link CompilationIdentifier} cannot cross the Truffle runtime/compiler boundary
     *            represented by this object
     * @param options option values relevant to compilation
     * @param compilable the Truffle AST to be compiled
     * @param inlining a guide for Truffle level inlining to be performed during compilation
     * @param task an object that must be periodically queried during compilation to see if the
     *            compilation has been cancelled by the requestor
     */
    void doCompile(DebugContext debug, CompilationIdentifier compilationId, OptionValues options, CompilableTruffleAST compilable, TruffleInliningPlan inlining, Cancellable task,
                    TruffleCompilerListener listener);

    /**
     * Returns a unique name for the configuration in use by this compiler.
     */
    String getCompilerConfigurationName();

    /**
     * Notifies this object that it will no longer being used and should thus perform all relevant
     * finalization tasks.
     */
    void shutdown();
}
