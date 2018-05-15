package graalvm.compiler.truffle.common;

import jdk.vm.ci.code.InstalledCode;

/**
 * Represents some machine code whose validity depends on an assumption. Valid machine code can
 * still be executed.
 */
public interface OptimizedAssumptionDependency {

    /**
     * Invalidates the machine code referenced by this object.
     */
    void invalidate();

    /**
     * Determines if the machine code referenced by this object is valid.
     */
    boolean isValid();

    /**
     * Gets the Truffle AST whose machine code is represented by this object. May be {@code null}.
     */
    default CompilableTruffleAST getCompilable() {
        return null;
    }

    /**
     * Determines if the reachability of this object corresponds with the validity of the referenced
     * machine code.
     *
     * @return {@code true} if the referenced machine code is guaranteed to be invalid when this
     *         object becomes unreachable, {@code false} if the reachability of this object says
     *         nothing about the validity of the referenced machine code
     */
    default boolean reachabilityDeterminesValidity() {
        return true;
    }

    /**
     * Provides access to a {@link OptimizedAssumptionDependency}.
     *
     * Introduced when {@code OptimizedCallTarget} was changed to no longer extend
     * {@link InstalledCode}. Prior to that change, {@code OptimizedAssumption} dependencies were
     * {@link InstalledCode} objects.
     */
    interface Access {
        OptimizedAssumptionDependency getDependency();
    }
}
