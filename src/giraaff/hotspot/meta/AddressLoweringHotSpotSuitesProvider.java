package giraaff.hotspot.meta;

import java.util.ListIterator;

import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.options.OptionValues;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.common.ExpandLogicPhase;
import giraaff.phases.common.FixReadsPhase;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

/**
 * Subclass to factor out management of address lowering.
 */
public class AddressLoweringHotSpotSuitesProvider extends HotSpotSuitesProvider
{
    private final Phase addressLowering;

    public AddressLoweringHotSpotSuitesProvider(SuitesCreator defaultSuitesCreator, HotSpotGraalRuntimeProvider runtime, Phase addressLowering)
    {
        super(defaultSuitesCreator, runtime);
        this.addressLowering = addressLowering;
    }

    @Override
    public Suites createSuites(OptionValues options)
    {
        Suites suites = super.createSuites(options);

        ListIterator<BasePhase<? super LowTierContext>> findPhase = suites.getLowTier().findPhase(FixReadsPhase.class);
        if (findPhase == null)
        {
            findPhase = suites.getLowTier().findPhase(ExpandLogicPhase.class);
        }
        findPhase.add(addressLowering);

        return suites;
    }
}
