package giraaff.phases.tiers;

import giraaff.lir.phases.LIRSuites;
import giraaff.phases.PhaseSuite;

/**
 * Main interface providing access to suites used for compilation.
 */

// @iface SuitesProvider
public interface SuitesProvider
{
    /**
     * Get the default phase suites of this compiler. The returned suite is immutable by default, but
     * {@link Suites#copy} can be used to create a customized version.
     */
    Suites getDefaultSuites();

    /**
     * Get the default phase suite for creating new graphs.
     */
    PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite();

    /**
     * Get the default LIR phase suites of this compiler. The returned suite is immutable by default,
     * but {@link LIRSuites#copy} can be used to create a customized version.
     */
    LIRSuites getDefaultLIRSuites();
}
