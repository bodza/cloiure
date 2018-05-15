package graalvm.compiler.hotspot.meta;

import java.util.ListIterator;

import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.common.ExpandLogicPhase;
import graalvm.compiler.phases.common.FixReadsPhase;
import graalvm.compiler.phases.tiers.LowTierContext;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.phases.tiers.SuitesCreator;

/**
 * Subclass to factor out management of address lowering.
 */
public class AddressLoweringHotSpotSuitesProvider extends HotSpotSuitesProvider {

    private final Phase addressLowering;

    public AddressLoweringHotSpotSuitesProvider(SuitesCreator defaultSuitesCreator, GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime,
                    Phase addressLowering) {
        super(defaultSuitesCreator, config, runtime);
        this.addressLowering = addressLowering;
    }

    @Override
    public Suites createSuites(OptionValues options) {
        Suites suites = super.createSuites(options);

        ListIterator<BasePhase<? super LowTierContext>> findPhase = suites.getLowTier().findPhase(FixReadsPhase.class);
        if (findPhase == null) {
            findPhase = suites.getLowTier().findPhase(ExpandLogicPhase.class);
        }
        findPhase.add(addressLowering);

        return suites;
    }
}
