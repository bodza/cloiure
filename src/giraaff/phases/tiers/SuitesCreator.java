package giraaff.phases.tiers;

import giraaff.lir.phases.LIRSuites;
import giraaff.options.OptionValues;

/**
 * Interface used for composing {@link SuitesProvider}s.
 */
public interface SuitesCreator extends SuitesProvider
{
    /**
     * Create a new set of phase suites based on {@code options}.
     */
    Suites createSuites(OptionValues options);

    /**
     * Create a new set of low-level phase suites based on {@code options}.
     */
    LIRSuites createLIRSuites(OptionValues options);
}
