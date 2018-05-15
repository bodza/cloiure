package graalvm.compiler.phases.tiers;

import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;

/**
 * Main interface providing access to suites used for compilation.
 */

public interface SuitesProvider {

    /**
     * Get the default phase suites of this compiler. This will take {@code options} into account,
     * returning an appropriately constructed suite. The returned suite is immutable by default but
     * {@link Suites#copy} can be used to create a customized version.
     */
    Suites getDefaultSuites(OptionValues values);

    /**
     * Get the default phase suite for creating new graphs.
     */
    PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite();

    /**
     * Get the default LIR phase suites of this compiler. This will take in account any options
     * enabled at the time of call, returning an appropriately constructed suite. The returned suite
     * is immutable by default but {@link LIRSuites#copy} can be used to create a customized
     * version.
     */
    LIRSuites getDefaultLIRSuites(OptionValues options);
}
