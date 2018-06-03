package giraaff.phases.tiers;

import giraaff.lir.phases.LIRSuites;

///
// Interface used for composing {@link SuitesProvider}s.
///
// @iface SuitesCreator
public interface SuitesCreator extends SuitesProvider
{
    ///
    // Create a new set of phase suites.
    ///
    Suites createSuites();

    ///
    // Create a new set of low-level phase suites.
    ///
    LIRSuites createLIRSuites();
}
