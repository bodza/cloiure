package graalvm.compiler.truffle.common;

import java.util.function.Supplier;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SpeculationLog;

/**
 * A Truffle AST that can be compiled by a {@link TruffleCompiler}.
 */
public interface CompilableTruffleAST {
    /**
     * Gets this AST as a compiler constant.
     */
    JavaConstant asJavaConstant();

    SpeculationLog getSpeculationLog();

    /**
     * Notifies this object that a compilation of the AST it represents failed.
     *
     * @param reasonAndStackTrace the output of {@link Throwable#printStackTrace()} for the
     *            exception representing the reason for compilation failure
     * @param bailout specifies whether the failure was a bailout or an error in the compiler. A
     *            bailout means the compiler aborted the compilation based on some of property of
     *            the AST (e.g., too big). A non-bailout means an unexpected error in the compiler
     *            itself.
     * @param permanentBailout specifies if a bailout is due to a condition that probably won't
     *            change if this AST is compiled again. This value is meaningless if
     *            {@code bailout == false}.
     */
    void onCompilationFailed(Supplier<String> reasonAndStackTrace, boolean bailout, boolean permanentBailout);

    /**
     * Gets a descriptive name for this call target.
     */
    String getName();
}
