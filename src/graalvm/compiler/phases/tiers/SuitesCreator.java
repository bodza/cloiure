package graalvm.compiler.phases.tiers;

import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.options.OptionValues;

/**
 * Interface used for composing {@link SuitesProvider}s.
 */
public interface SuitesCreator extends SuitesProvider {
    /**
     * Create a new set of phase suites based on {@code options}.
     */
    Suites createSuites(OptionValues options);

    /**
     * Create a new set of low-level phase suites based on {@code options}.
     */
    LIRSuites createLIRSuites(OptionValues options);
}
