package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.core.amd64.AMD64SuitesCreator;
import graalvm.compiler.debug.Assertions;
import graalvm.compiler.hotspot.lir.HotSpotZapRegistersPhase;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.tiers.CompilerConfiguration;

public class AMD64HotSpotSuitesCreator extends AMD64SuitesCreator
{
    public AMD64HotSpotSuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super(compilerConfiguration, plugins);
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        LIRSuites lirSuites = super.createLIRSuites(options);
        if (Assertions.detailedAssertionsEnabled(options))
        {
            lirSuites.getPostAllocationOptimizationStage().appendPhase(new HotSpotZapRegistersPhase());
        }
        return lirSuites;
    }
}
