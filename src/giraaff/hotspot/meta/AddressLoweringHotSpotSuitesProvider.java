package giraaff.hotspot.meta;

import java.util.ListIterator;

import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.common.ExpandLogicPhase;
import giraaff.phases.common.FixReadsPhase;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

///
// Subclass to factor out management of address lowering.
///
// @class AddressLoweringHotSpotSuitesProvider
public final class AddressLoweringHotSpotSuitesProvider extends HotSpotSuitesProvider
{
    // @field
    private final Phase ___addressLowering;

    // @cons
    public AddressLoweringHotSpotSuitesProvider(SuitesCreator __defaultSuitesCreator, HotSpotGraalRuntime __runtime, Phase __addressLowering)
    {
        super(__defaultSuitesCreator, __runtime);
        this.___addressLowering = __addressLowering;
    }

    @Override
    public Suites createSuites()
    {
        Suites __suites = super.createSuites();

        ListIterator<BasePhase<? super LowTierContext>> __findPhase = __suites.getLowTier().findPhase(FixReadsPhase.class);
        if (__findPhase == null)
        {
            __findPhase = __suites.getLowTier().findPhase(ExpandLogicPhase.class);
        }
        __findPhase.add(this.___addressLowering);

        return __suites;
    }
}
